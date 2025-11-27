import java.math.RoundingMode

/**
 * Advanced Heliotrope - Circular Sky Region Driver
 *
 * Describes a circular cone in the sky defined by a center azimuth/altitude and
 * a radius in degrees. Emits events whenever the sun enters or exits the region.
 */
@Field static final String DEGREE_SYMBOL = '°'
@Field static final String STATUS_INSIDE = 'inside'
@Field static final String STATUS_OUTSIDE = 'outside'
@Field static final String FLAG_TRUE = 'true'
@Field static final String FLAG_FALSE = 'false'
@Field static final String ATTR_IN_REGION = 'inRegion'
@Field static final String ATTR_REGION_STATUS = 'regionStatus'
@Field static final String ATTR_LAST_AZIMUTH = 'lastAzimuth'
@Field static final String ATTR_LAST_ALTITUDE = 'lastAltitude'
@Field static final String ATTR_DISTANCE = 'distanceFromCenter'
@Field static final String ATTR_ENTERED = 'enteredRegion'
@Field static final String ATTR_EXITED = 'exitedRegion'
@Field static final String ATTR_SUMMARY = 'regionSummary'
@Field static final String TYPE_NUMBER = 'NUMBER'
@Field static final String TYPE_STRING = 'STRING'
@Field static final String INPUT_DECIMAL = 'decimal'
@Field static final BigDecimal FULL_CIRCLE_DEGREES = 360G
@Field static final BigDecimal MIN_ALTITUDE_DEGREES = -90G
@Field static final BigDecimal MAX_ALTITUDE_DEGREES = 90G
@Field static final BigDecimal MIN_RADIUS_DEGREES = 0.1G
@Field static final BigDecimal STRAIGHT_ANGLE_DEGREES = 180G
@Field static final BigDecimal MAX_RADIUS_DEGREES = STRAIGHT_ANGLE_DEGREES
@Field static final BigDecimal DEFAULT_RADIUS_DEGREES = 10G
@Field static final BigDecimal DEFAULT_CENTER_AZIMUTH = STRAIGHT_ANGLE_DEGREES
@Field static final BigDecimal DEFAULT_CENTER_ALTITUDE = 45G
@Field static final int ANGLE_SCALE = 3
metadata {
    definition(name: 'Advanced Heliotrope Circular Region', namespace: 'electrified-home', author: 'Electrified Home') {
        capability 'Sensor'

        attribute ATTR_IN_REGION, 'ENUM'
        attribute ATTR_REGION_STATUS, TYPE_STRING
        attribute ATTR_LAST_AZIMUTH, TYPE_NUMBER
        attribute ATTR_LAST_ALTITUDE, TYPE_NUMBER
        attribute ATTR_DISTANCE, TYPE_NUMBER
        attribute ATTR_ENTERED, TYPE_STRING
        attribute ATTR_EXITED, TYPE_STRING
        attribute ATTR_SUMMARY, TYPE_STRING

        command 'updateSunPosition', [[name: 'Azimuth', type: TYPE_NUMBER], [name: 'Altitude', type: TYPE_NUMBER]]
        command 'clearHistory'
    }

    preferences {
        input 'centerAzimuth', INPUT_DECIMAL, title: 'Center azimuth (degrees)', range: '0..360',
            defaultValue: DEFAULT_CENTER_AZIMUTH.intValue()
        input 'centerAltitude', INPUT_DECIMAL, title: 'Center altitude (degrees)', range: '-90..90',
            defaultValue: DEFAULT_CENTER_ALTITUDE.intValue()
        input 'radiusDegrees', INPUT_DECIMAL, title: 'Radius (degrees)', range: '1..180',
            defaultValue: DEFAULT_RADIUS_DEGREES.intValue()
        input 'logDebug', 'bool', title: 'Enable debug logging', defaultValue: false
    }
}

void installed() {
    log.info 'Circular region driver installed'
    initialize()
}

void updated() {
    log.info 'Circular region driver updated'
    initialize()
}

void clearHistory() {
    state.inRegion = null
    sendEvent(name: ATTR_ENTERED, value: '')
    sendEvent(name: ATTR_EXITED, value: '')
    sendEvent(name: ATTR_IN_REGION, value: FLAG_FALSE)
    sendEvent(name: ATTR_REGION_STATUS, value: STATUS_OUTSIDE)
}

void updateSunPosition(Number azimuth, Number altitude) {
    if (azimuth == null || altitude == null) {
        return
    }

    BigDecimal normalizedAz = normalizeAzimuth(azimuth)
    BigDecimal sanitizedAlt = sanitizeAltitude(altitude)

    sendEvent(name: ATTR_LAST_AZIMUTH, value: normalizedAz, unit: DEGREE_SYMBOL)
    sendEvent(name: ATTR_LAST_ALTITUDE, value: sanitizedAlt, unit: DEGREE_SYMBOL)

    BigDecimal distance = angularDistance(normalizedAz, sanitizedAlt, getCenterAzimuth(), getCenterAltitude())
    sendEvent(name: ATTR_DISTANCE, value: round(distance), unit: DEGREE_SYMBOL)

    boolean inside = distance <= getRadius()
    applyRegionState(inside)
    debugLog "Sun update az=${normalizedAz} alt=${sanitizedAlt} distance=${round(distance)} inside=${inside}"
}

private void initialize() {
    updateSummary()
    if (state.inRegion == null) {
        state.inRegion = false
    }
}

private void updateSummary() {
    String summary = "Center ${getCenterAzimuth()}${DEGREE_SYMBOL}/${getCenterAltitude()}${DEGREE_SYMBOL}"
    summary += " radius ${getRadius()}${DEGREE_SYMBOL}"
    sendEvent(name: ATTR_SUMMARY, value: summary)
}

private void applyRegionState(boolean inside) {
    String status = inside ? STATUS_INSIDE : STATUS_OUTSIDE
    sendEvent(name: ATTR_IN_REGION, value: inside ? FLAG_TRUE : FLAG_FALSE)
    sendEvent(name: ATTR_REGION_STATUS, value: status)

    if (state.inRegion == null || state.inRegion != inside) {
        state.inRegion = inside
        if (inside) {
            sendEvent(name: ATTR_ENTERED, value: timestamp())
        } else {
            sendEvent(name: ATTR_EXITED, value: timestamp())
        }
    }
}

private BigDecimal normalizeAzimuth(Number value) {
    BigDecimal degrees = toBigDecimal(value, BigDecimal.ZERO)
    BigDecimal normalized = degrees.remainder(FULL_CIRCLE_DEGREES)
    if (normalized.signum() < 0) {
        normalized = normalized.add(FULL_CIRCLE_DEGREES)
    }
    return normalized.setScale(ANGLE_SCALE, RoundingMode.HALF_UP)
}

private BigDecimal sanitizeAltitude(Number value) {
    BigDecimal alt = toBigDecimal(value, BigDecimal.ZERO)
    BigDecimal clamped = clamp(alt, MIN_ALTITUDE_DEGREES, MAX_ALTITUDE_DEGREES)
    return clamped.setScale(ANGLE_SCALE, RoundingMode.HALF_UP)
}

private BigDecimal getCenterAzimuth() {
    return normalizeAzimuth(settings.centerAzimuth ?: DEFAULT_CENTER_AZIMUTH)
}

private BigDecimal getCenterAltitude() {
    return sanitizeAltitude(settings.centerAltitude ?: DEFAULT_CENTER_ALTITUDE)
}

private BigDecimal getRadius() {
    BigDecimal raw = toBigDecimal(settings.radiusDegrees, DEFAULT_RADIUS_DEGREES)
    BigDecimal clamped = clamp(raw, MIN_RADIUS_DEGREES, MAX_RADIUS_DEGREES)
    return clamped.setScale(ANGLE_SCALE, RoundingMode.HALF_UP)
}

private BigDecimal round(BigDecimal value) {
    return value?.setScale(ANGLE_SCALE, RoundingMode.HALF_UP)
}

private BigDecimal angularDistance(BigDecimal az1, BigDecimal alt1, BigDecimal az2, BigDecimal alt2) {
    BigDecimal az1Rad = toRadians(az1)
    BigDecimal alt1Rad = toRadians(alt1)
    BigDecimal az2Rad = toRadians(az2)
    BigDecimal alt2Rad = toRadians(alt2)

    BigDecimal x1 = cos(alt1Rad) * sin(az1Rad)
    BigDecimal y1 = cos(alt1Rad) * cos(az1Rad)
    BigDecimal z1 = sin(alt1Rad)

    BigDecimal x2 = cos(alt2Rad) * sin(az2Rad)
    BigDecimal y2 = cos(alt2Rad) * cos(az2Rad)
    BigDecimal z2 = sin(alt2Rad)

    BigDecimal dot = (x1 * x2) + (y1 * y2) + (z1 * z2)
    BigDecimal limited = clamp(dot, BigDecimal.valueOf(-1d), BigDecimal.ONE)
    BigDecimal radians = BigDecimal.valueOf(Math.acos(limited.doubleValue()))
    BigDecimal degrees = BigDecimal.valueOf(Math.toDegrees(radians.doubleValue()))
    return degrees.setScale(ANGLE_SCALE, RoundingMode.HALF_UP)
}

private BigDecimal toRadians(BigDecimal degrees) {
    return BigDecimal.valueOf(Math.toRadians(degrees.doubleValue()))
}

private BigDecimal sin(BigDecimal radians) {
    return BigDecimal.valueOf(Math.sin(radians.doubleValue()))
}

private BigDecimal cos(BigDecimal radians) {
    return BigDecimal.valueOf(Math.cos(radians.doubleValue()))
}

private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
    BigDecimal lower = value.max(min)
    return lower.min(max)
}

private BigDecimal toBigDecimal(Number value, BigDecimal fallback) {
    if (value == null) {
        return fallback
    }
    return BigDecimal.valueOf(value.doubleValue())
}

private String timestamp() {
    TimeZone tz = location?.timeZone ?: TimeZone.getDefault()
    return new Date().format('yyyy-MM-dd HH:mm:ss z', tz)
}

private void debugLog(String message) {
    if (settings.logDebug) {
        log.debug "[Circular Region] ${message}"
    }
}
