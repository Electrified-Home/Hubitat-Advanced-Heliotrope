import groovy.transform.Field

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
@Field static final String ATTR_MOTION = 'motion'
@Field static final String MOTION_ACTIVE = 'active'
@Field static final String MOTION_INACTIVE = 'inactive'
@Field static final String TYPE_NUMBER = 'NUMBER'
@Field static final String TYPE_STRING = 'STRING'
@Field static final String INPUT_DECIMAL = 'decimal'
@Field static final String INPUT_BOOL = 'bool'
@Field static final String RANGE_AZIMUTH = '0..360'
@Field static final String RANGE_ALTITUDE = '-90..90'
@Field static final String RANGE_RADIUS = '1..180'
@Field static final double FULL_CIRCLE_DEGREES = 360d
@Field static final double MIN_ALTITUDE_DEGREES = -90d
@Field static final double MAX_ALTITUDE_DEGREES = 90d
@Field static final double MIN_RADIUS_DEGREES = 1d
@Field static final double MAX_RADIUS_DEGREES = 180d
@Field static final double DEFAULT_RADIUS_DEGREES = 10d
@Field static final double DEFAULT_CENTER_AZIMUTH = 180d
@Field static final double DEFAULT_CENTER_ALTITUDE = 45d
@Field static final double ROUND_SCALE = 1000d
metadata {
    definition(name: 'Advanced Heliotrope Circular Region', namespace: 'electrified-home', author: 'Electrified Home') {
        capability 'Sensor'
        capability 'MotionSensor'

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
        input 'centerAzimuth', INPUT_DECIMAL, title: 'Center azimuth (degrees)', range: RANGE_AZIMUTH,
            defaultValue: (int) DEFAULT_CENTER_AZIMUTH
        input 'centerAltitude', INPUT_DECIMAL, title: 'Center altitude (degrees)', range: RANGE_ALTITUDE,
            defaultValue: (int) DEFAULT_CENTER_ALTITUDE
        input 'radiusDegrees', INPUT_DECIMAL, title: 'Radius (degrees)', range: RANGE_RADIUS,
            defaultValue: (int) DEFAULT_RADIUS_DEGREES
        input 'logDebug', INPUT_BOOL, title: 'Enable debug logging', defaultValue: false
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

    double distance = angularDistance(normalizedAz, sanitizedAlt, getCenterAzimuth(), getCenterAltitude())
    sendEvent(name: ATTR_DISTANCE, value: distance, unit: DEGREE_SYMBOL)

    boolean inside = distance <= getRadius()
    applyRegionState(inside)
    debugLog "Sun update az=${normalizedAz} alt=${sanitizedAlt} distance=${distance} inside=${inside}"
}

private void initialize() {
    updateSummary()
    if (state.inRegion == null) {
        state.inRegion = false
        applyRegionState(false)
    }
}

private void updateSummary() {
    String summary = "Center ${formatDegrees(getCenterAzimuth())}${DEGREE_SYMBOL}/" +
        "${formatDegrees(getCenterAltitude())}${DEGREE_SYMBOL}"
    summary += " radius ${formatDegrees(getRadius())}${DEGREE_SYMBOL}"
    sendEvent(name: ATTR_SUMMARY, value: summary)
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

private double normalizeAzimuth(Number value) {
    double degrees = toDouble(value, 0d)
    double normalized = degrees % FULL_CIRCLE_DEGREES
    if (normalized < 0d) {
        normalized += FULL_CIRCLE_DEGREES
    }
    return roundDegrees(normalized)
}

private double sanitizeAltitude(Number value) {
    double alt = toDouble(value, 0d)
    double clamped = clamp(alt, MIN_ALTITUDE_DEGREES, MAX_ALTITUDE_DEGREES)
    return roundDegrees(clamped)
}

private double getCenterAzimuth() {
    return normalizeAzimuth(settings.centerAzimuth ?: DEFAULT_CENTER_AZIMUTH)
}

private double getCenterAltitude() {
    return sanitizeAltitude(settings.centerAltitude ?: DEFAULT_CENTER_ALTITUDE)
}

private double getRadius() {
    double raw = toDouble(settings.radiusDegrees, DEFAULT_RADIUS_DEGREES)
    double clamped = clamp(raw, MIN_RADIUS_DEGREES, MAX_RADIUS_DEGREES)
    return roundDegrees(clamped)
}

private double angularDistance(double az1, double alt1, double az2, double alt2) {
    double az1Rad = Math.toRadians(az1)
    double alt1Rad = Math.toRadians(alt1)
    double az2Rad = Math.toRadians(az2)
    double alt2Rad = Math.toRadians(alt2)

    double x1 = Math.cos(alt1Rad) * Math.sin(az1Rad)
    double y1 = Math.cos(alt1Rad) * Math.cos(az1Rad)
    double z1 = Math.sin(alt1Rad)

    double x2 = Math.cos(alt2Rad) * Math.sin(az2Rad)
    double y2 = Math.cos(alt2Rad) * Math.cos(az2Rad)
    double z2 = Math.sin(alt2Rad)

    double dot = clamp((x1 * x2) + (y1 * y2) + (z1 * z2), -1d, 1d)
    double radians = Math.acos(dot)
    return roundDegrees(Math.toDegrees(radians))
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
    return Math.round(value * ROUND_SCALE) / ROUND_SCALE
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
        log.debug "[Circular Region] ${message}"
    }
}
