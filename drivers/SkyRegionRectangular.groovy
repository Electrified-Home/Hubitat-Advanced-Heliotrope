import groovy.transform.Field

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
@Field static final String ATTR_MOTION = 'motion'
@Field static final String MOTION_ACTIVE = 'active'
@Field static final String MOTION_INACTIVE = 'inactive'
@Field static final String TYPE_NUMBER = 'NUMBER'
@Field static final String TYPE_STRING = 'STRING'
@Field static final String INPUT_DECIMAL = 'decimal'
@Field static final String RANGE_AZIMUTH = '0..360'
@Field static final String RANGE_ALTITUDE = '-90..90'
@Field static final double ZERO_DEGREES = 0d
@Field static final double FULL_CIRCLE_DEGREES = 360d
@Field static final double MIN_ALTITUDE_DEGREES = -90d
@Field static final double MAX_ALTITUDE_DEGREES = 90d
@Field static final double DEFAULT_MIN_AZIMUTH = 90d
@Field static final double DEFAULT_MAX_AZIMUTH = 270d
@Field static final double DEFAULT_MIN_ALTITUDE = 0d
@Field static final double DEFAULT_MAX_ALTITUDE = 70d
@Field static final double FALLBACK_MIN_ALTITUDE = -30d
@Field static final double FALLBACK_MAX_ALTITUDE = 60d
@Field static final double DEGREE_ROUND_SCALE = 1000d
metadata {
    definition(
        name: 'Advanced Heliotrope Rectangular Region',
        namespace: 'electrified-home',
        author: 'Electrified Home'
    ) {
        capability 'Sensor'
        capability 'MotionSensor'

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
            defaultValue: (int) DEFAULT_MIN_AZIMUTH
        input 'maxAzimuth', INPUT_DECIMAL, title: 'Maximum azimuth (degrees)', range: RANGE_AZIMUTH,
            defaultValue: (int) DEFAULT_MAX_AZIMUTH
        input 'minAltitude', INPUT_DECIMAL, title: 'Minimum altitude (degrees)', range: RANGE_ALTITUDE,
            defaultValue: (int) DEFAULT_MIN_ALTITUDE
        input 'maxAltitude', INPUT_DECIMAL, title: 'Maximum altitude (degrees)', range: RANGE_ALTITUDE,
            defaultValue: (int) DEFAULT_MAX_ALTITUDE
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
    sendEvent(name: ATTR_MOTION, value: MOTION_INACTIVE)
}

void updateSunPosition(Number azimuth, Number altitude) {
    if (azimuth == null || altitude == null) {
        return
    }

    double normalizedAz = normalizeAzimuth(azimuth)
    double sanitizedAlt = sanitizeAltitude(altitude)

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
        applyRegionState(false)
    }
}

private String summaryText() {
    String azRange = "Az ${formatDegrees(getMinAzimuth())}${DEGREE_SYMBOL}-" +
        "${formatDegrees(getMaxAzimuth())}${DEGREE_SYMBOL}"
    String altRange = "Alt ${formatDegrees(getMinAltitude())}${DEGREE_SYMBOL}-" +
        "${formatDegrees(getMaxAltitude())}${DEGREE_SYMBOL}"
    return "${azRange} ${altRange}"
}

private void applyRegionState(boolean inside) {
    String status = inside ? STATUS_INSIDE : STATUS_OUTSIDE
    sendEvent(name: ATTR_IN_REGION, value: inside ? FLAG_TRUE : FLAG_FALSE)
    sendEvent(name: ATTR_REGION_STATUS, value: status)
    sendEvent(name: ATTR_MOTION, value: inside ? MOTION_ACTIVE : MOTION_INACTIVE)

    Boolean previousState = (state.inRegion as Boolean)
    state.inRegion = inside
    if (previousState == null) {
        if (inside) {
            sendEvent(name: ATTR_ENTERED, value: timestamp())
        }
        return
    }
    if (previousState != inside) {
        if (inside) {
            sendEvent(name: ATTR_ENTERED, value: timestamp())
        } else {
            sendEvent(name: ATTR_EXITED, value: timestamp())
        }
    }
}

private boolean isAzimuthInside(double value) {
    double min = getMinAzimuth()
    double max = getMaxAzimuth()

    if (min <= max) {
        return value >= min && value <= max
    }
    // Wrapped window, e.g., 300°..60°
    return value >= min || value <= max
}

private boolean isAltitudeInside(double value) {
    return value >= getMinAltitude() && value <= getMaxAltitude()
}

private double getMinAzimuth() {
    return normalizeAzimuth(settings.minAzimuth ?: ZERO_DEGREES)
}

private double getMaxAzimuth() {
    return normalizeAzimuth(settings.maxAzimuth ?: FULL_CIRCLE_DEGREES)
}

private Map altitudeRange() {
    double low = sanitizeAltitude(settings.minAltitude ?: FALLBACK_MIN_ALTITUDE)
    double high = sanitizeAltitude(settings.maxAltitude ?: FALLBACK_MAX_ALTITUDE)
    if (low > high) {
        double swap = low
        low = high
        high = swap
    }
    return [min: low, max: high]
}

private double getMinAltitude() {
    return (double) altitudeRange().min
}

private double getMaxAltitude() {
    return (double) altitudeRange().max
}

private double normalizeAzimuth(Number value) {
    double degrees = toDouble(value, ZERO_DEGREES)
    double normalized = degrees % FULL_CIRCLE_DEGREES
    if (normalized < 0d) {
        normalized += FULL_CIRCLE_DEGREES
    }
    return roundDegrees(normalized)
}

private double sanitizeAltitude(Number value) {
    double alt = toDouble(value, ZERO_DEGREES)
    double clamped = clamp(alt, MIN_ALTITUDE_DEGREES, MAX_ALTITUDE_DEGREES)
    return roundDegrees(clamped)
}

private double clamp(double value, double min, double max) {
    return Math.min(Math.max(value, min), max)
}

private double toDouble(Object value, double fallback) {
    if (value == null) {
        return fallback
    }
    if (value in Number) {
        return ((Number) value).doubleValue()
    }
    try {
        return Double.parseDouble(value.toString())
    } catch (NumberFormatException ignored) {
        return fallback
    }
}

private double roundDegrees(double value) {
    return Math.round(value * DEGREE_ROUND_SCALE) / DEGREE_ROUND_SCALE
}

private String formatDegrees(double value) {
    double rounded = roundDegrees(value)
    boolean isWhole = Math.abs(rounded - Math.rint(rounded)) < 0.0000001d
    return isWhole ? rounded.toInteger().toString() : rounded.toString()
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
