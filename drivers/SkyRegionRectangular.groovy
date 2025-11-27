import groovy.transform.Field

/**
 * Advanced Heliotrope - Rectangular Sky Region Driver
 *
 * Defines a rectangular window in azimuth/altitude space and emits `motion`
 * events that behave like a basic sensor.
 */
@Field static final String ATTR_MOTION = 'motion'
@Field static final String MOTION_ACTIVE = 'active'
@Field static final String MOTION_INACTIVE = 'inactive'
@Field static final String TYPE_NUMBER = 'NUMBER'
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

        command 'updateSunPosition', [[name: 'Azimuth', type: TYPE_NUMBER], [name: 'Altitude', type: TYPE_NUMBER]]
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
    }
}

void installed() {
    initialize()
}

void updated() {
    initialize()
}

void updateSunPosition(Number azimuth, Number altitude) {
    if (azimuth == null || altitude == null) {
        return
    }

    double normalizedAz = normalizeAzimuth(azimuth)
    double sanitizedAlt = sanitizeAltitude(altitude)

    boolean inside = isAzimuthInside(normalizedAz) && isAltitudeInside(sanitizedAlt)
    applyRegionState(inside)
}

private void initialize() {
    sendEvent(name: ATTR_MOTION, value: '')
}

private void applyRegionState(boolean inside) {
    sendEvent(name: ATTR_MOTION, value: inside ? MOTION_ACTIVE : MOTION_INACTIVE)
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

