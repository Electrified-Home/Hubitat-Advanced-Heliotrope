import groovy.transform.Field
import java.math.RoundingMode

/**
 * Advanced Heliotrope - Rectangular Sky Region Driver
 *
 * Defines a rectangular window in azimuth/altitude space. Emits state changes
 * when the sun enters or exits that window.
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
@Field static final String ATTR_SUMMARY = 'regionSummary'
@Field static final String ATTR_ENTERED = 'enteredRegion'
@Field static final String ATTR_EXITED = 'exitedRegion'
@Field static final String TYPE_NUMBER = 'NUMBER'
@Field static final String TYPE_STRING = 'STRING'
@Field static final String INPUT_DECIMAL = 'decimal'
@Field static final String RANGE_AZIMUTH = '0..360'
@Field static final String RANGE_ALTITUDE = '-90..90'
@Field final BigDecimal ZERO_DEGREES = 0G
@Field final BigDecimal FULL_CIRCLE_DEGREES = 360G
@Field final BigDecimal MIN_ALTITUDE_DEGREES = -90G
@Field final BigDecimal RIGHT_ANGLE_DEGREES = 90G
@Field final BigDecimal MAX_ALTITUDE_DEGREES = RIGHT_ANGLE_DEGREES
@Field final BigDecimal DEFAULT_MIN_AZIMUTH = RIGHT_ANGLE_DEGREES
@Field final BigDecimal DEFAULT_MAX_AZIMUTH = 270G
@Field final BigDecimal DEFAULT_MIN_ALTITUDE = ZERO_DEGREES
@Field final BigDecimal DEFAULT_MAX_ALTITUDE = 70G
@Field final BigDecimal FALLBACK_MIN_ALTITUDE = -30G
@Field final BigDecimal FALLBACK_MAX_ALTITUDE = 60G
@Field final int ANGLE_SCALE = 3
@Field final int SIGNUM_NON_NEGATIVE = 0
metadata {
    definition(
        name: 'Advanced Heliotrope Rectangular Region',
        namespace: 'electrified-home',
        author: 'Electrified Home'
    ) {
        capability 'Sensor'

        attribute ATTR_IN_REGION, 'ENUM'
        attribute ATTR_REGION_STATUS, TYPE_STRING
        attribute ATTR_LAST_AZIMUTH, TYPE_NUMBER
        attribute ATTR_LAST_ALTITUDE, TYPE_NUMBER
        attribute ATTR_SUMMARY, TYPE_STRING
        attribute ATTR_ENTERED, TYPE_STRING
        attribute ATTR_EXITED, TYPE_STRING

        command 'updateSunPosition', [[name: 'Azimuth', type: TYPE_NUMBER], [name: 'Altitude', type: TYPE_NUMBER]]
        command 'clearHistory'
    }

    preferences {
        input 'minAzimuth', INPUT_DECIMAL, title: 'Minimum azimuth (degrees)', range: RANGE_AZIMUTH,
            defaultValue: DEFAULT_MIN_AZIMUTH.intValue()
        input 'maxAzimuth', INPUT_DECIMAL, title: 'Maximum azimuth (degrees)', range: RANGE_AZIMUTH,
            defaultValue: DEFAULT_MAX_AZIMUTH.intValue()
        input 'minAltitude', INPUT_DECIMAL, title: 'Minimum altitude (degrees)', range: RANGE_ALTITUDE,
            defaultValue: DEFAULT_MIN_ALTITUDE.intValue()
        input 'maxAltitude', INPUT_DECIMAL, title: 'Maximum altitude (degrees)', range: RANGE_ALTITUDE,
            defaultValue: DEFAULT_MAX_ALTITUDE.intValue()
        input 'logDebug', 'bool', title: 'Enable debug logging', defaultValue: false
    }
}

void installed() {
    log.info 'Rectangular region driver installed'
    initialize()
}

void updated() {
    log.info 'Rectangular region driver updated'
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

    boolean inside = isAzimuthInside(normalizedAz) && isAltitudeInside(sanitizedAlt)
    applyRegionState(inside)
    debugLog "Sun update az=${normalizedAz} alt=${sanitizedAlt} inside=${inside}"
}

private void initialize() {
    sendEvent(name: ATTR_SUMMARY, value: summaryText())
    if (state.inRegion == null) {
        state.inRegion = false
    }
}

private String summaryText() {
    String azRange = "Az ${getMinAzimuth()}${DEGREE_SYMBOL}-${getMaxAzimuth()}${DEGREE_SYMBOL}"
    String altRange = "Alt ${getMinAltitude()}${DEGREE_SYMBOL}-${getMaxAltitude()}${DEGREE_SYMBOL}"
    return "${azRange} ${altRange}"
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

private boolean isAzimuthInside(BigDecimal value) {
    BigDecimal min = getMinAzimuth()
    BigDecimal max = getMaxAzimuth()

    if (min <= max) {
        return value >= min && value <= max
    }
    // Wrapped window, e.g., 300°..60°
    return value >= min || value <= max
}

private boolean isAltitudeInside(BigDecimal value) {
    return value >= getMinAltitude() && value <= getMaxAltitude()
}

private BigDecimal getMinAzimuth() {
    return normalizeAzimuth(settings.minAzimuth ?: ZERO_DEGREES)
}

private BigDecimal getMaxAzimuth() {
    return normalizeAzimuth(settings.maxAzimuth ?: FULL_CIRCLE_DEGREES)
}

private Map altitudeRange() {
    BigDecimal low = sanitizeAltitude(settings.minAltitude ?: FALLBACK_MIN_ALTITUDE)
    BigDecimal high = sanitizeAltitude(settings.maxAltitude ?: FALLBACK_MAX_ALTITUDE)
    if (low > high) {
        BigDecimal swap = low
        low = high
        high = swap
    }
    return [min: low, max: high]
}

private BigDecimal getMinAltitude() {
    return altitudeRange().min
}

private BigDecimal getMaxAltitude() {
    return altitudeRange().max
}

private BigDecimal normalizeAzimuth(Number value) {
    BigDecimal degrees = toBigDecimal(value, BigDecimal.ZERO)
    BigDecimal normalized = degrees.remainder(FULL_CIRCLE_DEGREES)
    if (normalized.signum() < SIGNUM_NON_NEGATIVE) {
        normalized = normalized.add(FULL_CIRCLE_DEGREES)
    }
    return normalized.setScale(ANGLE_SCALE, RoundingMode.HALF_UP)
}

private BigDecimal sanitizeAltitude(Number value) {
    BigDecimal alt = toBigDecimal(value, BigDecimal.ZERO)
    BigDecimal clamped = clamp(alt, MIN_ALTITUDE_DEGREES, MAX_ALTITUDE_DEGREES)
    return clamped.setScale(ANGLE_SCALE, RoundingMode.HALF_UP)
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
        log.debug "[Rectangular Region] ${message}"
    }
}
