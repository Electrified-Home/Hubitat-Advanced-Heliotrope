import groovy.transform.Field
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Advanced Heliotrope - Sun Position Driver
 *
 * Calculates the current sun azimuth/altitude for the hub location and shares
 * the results with the parent app so region drivers can react.
 */
@Field static final String UNIT_DEGREES = '°'
@Field static final String UNIT_SECONDS = 's'
@Field static final String UNKNOWN_VALUE = 'Unknown'
@Field static final String INPUT_TYPE_BOOL = 'bool'
@Field static final String SCHEDULE_HANDLER = 'scheduledUpdate'
@Field static final String ATTR_AZIMUTH = 'azimuth'
@Field static final String ATTR_ALTITUDE = 'altitude'
@Field static final String ATTR_LAST_UPDATE = 'lastUpdate'
@Field static final String ATTR_LAST_CALCULATED = 'lastCalculated'
@Field static final String ATTR_RUNTIME = 'runtime'
@Field static final float FULL_CIRCLE_DEGREES = 360f
@Field static final float HALF_CIRCLE_DEGREES = 180f
@Field static final float RIGHT_ANGLE_DEGREES = 90f
@Field static final float MINUTES_PER_DAY = 1440f
@Field static final float MINUTES_PER_HOUR = 60f
/* groovylint-disable-next-line DuplicateNumberLiteral */
@Field static final float SECONDS_PER_MINUTE = 60f
@Field static final float SECONDS_PER_HOUR = 3600f
@Field static final int SECONDS_PER_MINUTE_INT = 60
@Field static final long MILLIS_PER_DAY = 86_400_000L
@Field static final double JULIAN_DATE_OFFSET = 2440587.5d
@Field static final double J2000_REFERENCE = 2451545.0d
@Field static final double DAYS_PER_CENTURY = 36525d
@Field static final float TWO = 2f
@Field static final float THREE = 3f
@Field static final double ROUNDING_SCALE = 1000d
@Field static final DateTimeFormatter TIMESTAMP_FORMATTER =
    DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss z')
@Field static final String DEFAULT_INTERVAL_LABEL = '2 Minutes'
@Field static final Map<String, Integer> UPDATE_INTERVAL_OPTIONS = [
    '1 Minute': 1,
    '2 Minutes': 2,
    '5 Minutes': 5,
    '10 Minutes': 10,
    '15 Minutes': 15,
    '30 Minutes': 30,
    '1 Hour': 60,
    '3 Hours': 180
]

@Field static final String NUMBER = 'NUMBER'
@Field static final String STRING = 'STRING'

metadata {
    definition(name: 'Advanced Heliotrope Sun Position', namespace: 'electrified-home', author: 'Electrified Home') {
        capability 'Sensor'
        capability 'Refresh'

        attribute ATTR_AZIMUTH, NUMBER
        attribute ATTR_ALTITUDE, NUMBER
        attribute ATTR_LAST_UPDATE, STRING
        attribute ATTR_LAST_CALCULATED, STRING
        attribute ATTR_RUNTIME, NUMBER

        command 'updatePosition'
        command 'setUpdateInterval', [
            [name: 'Minutes', type: NUMBER, description: 'Override interval from parent/app']
        ]
    }

    preferences {
        input 'autoUpdate', INPUT_TYPE_BOOL, title: 'Automatically update position', defaultValue: true
        input 'updateInterval', 'enum', title: 'Update interval', options: UPDATE_INTERVAL_OPTIONS.keySet() as List,
            defaultValue: DEFAULT_INTERVAL_LABEL, required: true
        input 'logDebug', INPUT_TYPE_BOOL, title: 'Enable debug logging', defaultValue: false
    }
}

void installed() {
    log.info 'Sun position driver installed'
    initialize()
}

void updated() {
    log.info 'Sun position driver updated'
    initialize()
}

void refresh() {
    updatePosition()
}

void updatePosition() {
    if (!isLocationConfigured()) {
        log.warn 'Cannot calculate sun position because hub location is not fully configured'
        return
    }

    Instant startTime = Instant.now()
    def position = calculateSunPosition(startTime)
    sendEvent(name: ATTR_AZIMUTH, value: position.azimuth, unit: UNIT_DEGREES)
    sendEvent(name: ATTR_ALTITUDE, value: position.altitude, unit: UNIT_DEGREES)
    sendEvent(name: ATTR_LAST_UPDATE, value: position.timestamp)
    sendEvent(name: ATTR_LAST_CALCULATED, value: position.timestamp)
    Duration runtime = Duration.between(startTime, Instant.now())
    double runtimeSeconds = runtime.toNanos() / 1_000_000_000d
    float seconds = roundNumber(runtimeSeconds)
    sendEvent(name: ATTR_RUNTIME, value: seconds, unit: UNIT_SECONDS)

    debugLog "Sun update azimuth=${position.azimuth} altitude=${position.altitude}"
    parent?.sunPositionUpdated(position)
}

void setUpdateInterval(Number minutes) {
    int overrideMinutes = minutes ? minutes.toInteger() : 0
    state.parentInterval = overrideMinutes
    debugLog "Parent interval override set to ${overrideMinutes} minutes"
    initialize()
}

void scheduledUpdate() {
    updatePosition()
    scheduleUpdateJob(getEffectiveIntervalMinutes(), false)
}

private void initialize() {
    unschedule()
    scheduleUpdateJob(getEffectiveIntervalMinutes(), true)
}

private int getEffectiveIntervalMinutes() {
    int parentInterval = (state.parentInterval ?: 0) as int
    if (parentInterval > 0) {
        return parentInterval
    }
    boolean autoEnabled = (settings.autoUpdate == null) ? true : settings.autoUpdate
    if (!autoEnabled) {
        return 0
    }
    return getSelectedIntervalMinutes()
}

private int getSelectedIntervalMinutes() {
    String selected = settings.updateInterval ?: DEFAULT_INTERVAL_LABEL
    int fallback = UPDATE_INTERVAL_OPTIONS[DEFAULT_INTERVAL_LABEL] ?: 2
    return UPDATE_INTERVAL_OPTIONS[selected] ?: fallback
}

private Map calculateSunPosition(Instant moment) {
    ZoneId zoneId = hubZoneId()
    ZonedDateTime zonedMoment = moment.atZone(zoneId)

    double latitude = (location.latitude ?: 0d) as double
    double longitude = (location.longitude ?: 0d) as double

    double julianDate = (moment.toEpochMilli() / (double) MILLIS_PER_DAY) + JULIAN_DATE_OFFSET
    double julianCentury = (julianDate - J2000_REFERENCE) / DAYS_PER_CENTURY

    double geomMeanLongSun = normalizeDegrees(
        280.46646d + julianCentury * (36000.76983d + julianCentury * 0.0003032d)
    )
    double geomMeanAnomSun = 357.52911d +
        julianCentury * (35999.05029d - 0.0001537d * julianCentury)
    double eccentEarthOrbit = 0.016708634d -
        julianCentury * (0.000042037d + 0.0000001267d * julianCentury)

    double sunEqOfCenter =
        Math.sin(Math.toRadians(geomMeanAnomSun)) *
            (1.914602d - julianCentury * (0.004817d + 0.000014d * julianCentury)) +
        Math.sin(Math.toRadians(TWO * geomMeanAnomSun)) *
            (0.019993d - 0.000101d * julianCentury) +
        Math.sin(Math.toRadians(THREE * geomMeanAnomSun)) * 0.000289d

    double sunTrueLong = geomMeanLongSun + sunEqOfCenter
    double omega = 125.04d - 1934.136d * julianCentury
    double sunAppLong = sunTrueLong - 0.00569d -
        0.00478d * Math.sin(Math.toRadians(omega))

    double meanObliqEcliptic = 23.0d +
        (26.0d + ((21.448d - julianCentury * (46.815d + julianCentury * (0.00059d - julianCentury * 0.001813d))) /
        MINUTES_PER_HOUR)) / MINUTES_PER_HOUR

    double obliqCorr = meanObliqEcliptic + 0.00256d * Math.cos(Math.toRadians(omega))
    double sunDeclination = Math.toDegrees(
        Math.asin(
            Math.sin(Math.toRadians(obliqCorr)) *
                Math.sin(Math.toRadians(sunAppLong))
        )
    )

    double varY = Math.tan(Math.toRadians(obliqCorr / 2d))
    varY *= varY

    double eqOfTime = 4d * Math.toDegrees(
        varY * Math.sin(2d * Math.toRadians(geomMeanLongSun)) -
        2d * eccentEarthOrbit * Math.sin(Math.toRadians(geomMeanAnomSun)) +
        4d * eccentEarthOrbit * varY *
            Math.sin(Math.toRadians(geomMeanAnomSun)) *
            Math.cos(2d * Math.toRadians(geomMeanLongSun)) -
        0.5d * varY * varY *
            Math.sin(4d * Math.toRadians(geomMeanLongSun)) -
        1.25d * eccentEarthOrbit * eccentEarthOrbit *
            Math.sin(2d * Math.toRadians(geomMeanAnomSun))
    )

    double minutesFromMidnight = (zonedMoment.getHour() * (double) MINUTES_PER_HOUR) +
        zonedMoment.getMinute() +
        (zonedMoment.getSecond() / (double) SECONDS_PER_MINUTE)
    double timezoneHours = zonedMoment.getOffset().getTotalSeconds() / (double) SECONDS_PER_HOUR
    double trueSolarTime = minutesFromMidnight + eqOfTime +
        4d * longitude - MINUTES_PER_HOUR * timezoneHours
    trueSolarTime = normalizeMinutes(trueSolarTime)

    double hourAngle = trueSolarTime / 4d - HALF_CIRCLE_DEGREES
    if (hourAngle < -HALF_CIRCLE_DEGREES) {
        hourAngle += FULL_CIRCLE_DEGREES
    }

    double sinLatitude = Math.sin(Math.toRadians(latitude))
    double cosLatitude = Math.cos(Math.toRadians(latitude))
    double sinDecl = Math.sin(Math.toRadians(sunDeclination))
    double cosDecl = Math.cos(Math.toRadians(sunDeclination))
    double sinHourAngle = Math.sin(Math.toRadians(hourAngle))
    double cosHourAngle = Math.cos(Math.toRadians(hourAngle))

    double cosZenith = sinLatitude * sinDecl + cosLatitude * cosDecl * cosHourAngle
    double clampedCos = clamp(cosZenith, -1d, 1d)
    double zenith = Math.toDegrees(Math.acos(clampedCos))
    double altitude = RIGHT_ANGLE_DEGREES - zenith

    double azimuth = Math.toDegrees(
        Math.atan2(
            sinHourAngle,
            cosHourAngle * sinLatitude - Math.tan(Math.toRadians(sunDeclination)) * cosLatitude
        )
    ) + HALF_CIRCLE_DEGREES
    azimuth = normalizeDegrees(azimuth)

    return [
        azimuth    : roundNumber(azimuth),
        altitude   : roundNumber(altitude),
        timestamp  : formatDate(moment),
        epochMillis: moment.toEpochMilli(),
        deviceId   : device?.id
    ]
}

private void scheduleUpdateJob(int minutes, boolean logMessage) {
    if (minutes > 0) {
        if (logMessage) {
            debugLog "Scheduling automatic sun updates every ${minutes} minute(s)"
        }
        runIn(minutes * SECONDS_PER_MINUTE_INT, SCHEDULE_HANDLER)
    } else if (logMessage) {
        debugLog 'Automatic scheduling disabled; awaiting parent or manual refresh'
    }
}

private String formatDate(Instant instant) {
    if (!instant) {
        return UNKNOWN_VALUE
    }
    return TIMESTAMP_FORMATTER.withZone(hubZoneId()).format(instant)
}

private ZoneId hubZoneId() {
    def tz = location?.timeZone
    return tz ? tz.toZoneId() : ZoneId.systemDefault()
}

private float roundNumber(double value) {
    double scaled = Math.round(value * ROUNDING_SCALE) / ROUNDING_SCALE
    return (float) scaled
}

private double normalizeDegrees(double degrees) {
    double normalized = degrees % FULL_CIRCLE_DEGREES
    return normalized < 0d ? normalized + FULL_CIRCLE_DEGREES : normalized
}

private double normalizeMinutes(double minutes) {
    double normalized = minutes % MINUTES_PER_DAY
    return normalized < 0d ? normalized + MINUTES_PER_DAY : normalized
}

private double clamp(double value, double min, double max) {
    return Math.min(Math.max(value, min), max)
}

private boolean isLocationConfigured() {
    return location?.latitude != null && location?.longitude != null
}

private void debugLog(String message) {
    if (settings.logDebug) {
        log.debug message
    }
}
