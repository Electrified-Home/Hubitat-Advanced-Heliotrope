/*
 * Advanced Heliotrope - Rectangular Sky Region Driver
 * Copyright (c) 2024-2025 Electrified Home
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */

import groovy.transform.Field
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
@Field static final String NEVER_TIME_VALUE = 'Never'
@Field static final long MILLIS_PER_DAY = 86_400_000L
@Field static final long WINDOW_STEP_MINUTES = 5L
@Field static final long WINDOW_STEP_SECONDS = 300L
@Field static final int WINDOW_REFINE_ITERATIONS = 8
@Field static final long WINDOW_SEARCH_DAYS = 2L
@Field static final DateTimeFormatter WINDOW_TIME_FORMATTER = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm')
@Field static final String COMMAND_CALCULATE_WINDOW = 'calculateNextWindow'
metadata {
    definition(
        name: 'Advanced Heliotrope Region, Rectangular',
        namespace: 'electrified-home',
        author: 'Electrified Home'
    ) {
        capability 'Sensor'
        capability 'MotionSensor'
        command COMMAND_CALCULATE_WINDOW
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

void calculateNextWindow() {
    performWindowPrediction()
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

void updateNextWindow(String entryTime, String exitTime) {
    state.nextEntry = entryTime
    state.nextExit = exitTime
}

boolean regionContains(Number azimuth, Number altitude) {
    if (azimuth == null || altitude == null) {
        return false
    }
    double normalizedAz = normalizeAzimuth(azimuth)
    double sanitizedAlt = sanitizeAltitude(altitude)
    return isAzimuthInside(normalizedAz) && isAltitudeInside(sanitizedAlt)
}

private void initialize() {
    Map position = fetchSunPosition(Instant.now())
    Number azimuth = position.azimuth as Number
    Number altitude = position.altitude as Number
    if (azimuth != null && altitude != null) {
        updateSunPosition(azimuth, altitude)
    } else {
        sendEvent(name: ATTR_MOTION, value: '')
    }
    calculateNextWindow()
}

private void applyRegionState(boolean inside) {
    sendEvent(name: ATTR_MOTION, value: inside ? MOTION_ACTIVE : MOTION_INACTIVE)
}

private void performWindowPrediction() {
    if (!isLocationConfigured()) {
        updateNextWindow(NEVER_TIME_VALUE, NEVER_TIME_VALUE)
        return
    }
    Instant startInstant = Instant.now()
    Instant endInstant = startInstant.plusMillis(WINDOW_SEARCH_DAYS * MILLIS_PER_DAY)
    Map<Long, Map> cache = [:]
    Map result = evaluateRegionWindow(startInstant, endInstant, cache)
    updateNextWindow(formatWindowValue((Instant) result.entry), formatWindowValue((Instant) result.exit))
}

private Map evaluateRegionWindow(Instant startInstant, Instant endInstant, Map<Long, Map> cache) {
    Instant cursor = startInstant
    boolean cursorInside = regionContainsAt(cursor, cache)
    Instant entryInstant = cursorInside ? cursor : null
    Instant exitInstant = null
    long stepSeconds = WINDOW_STEP_SECONDS

    while (cursor.isBefore(endInstant)) {
        Instant nextInstant = cursor.plusSeconds(stepSeconds)
        if (nextInstant.isAfter(endInstant)) {
            nextInstant = endInstant
        }

        boolean nextInside = regionContainsAt(nextInstant, cache)
        if (!cursorInside && nextInside && entryInstant == null) {
            entryInstant = refineBoundary(cursor, nextInstant, true, cache)
        }
        if (cursorInside && !nextInside) {
            exitInstant = refineBoundary(cursor, nextInstant, false, cache)
            if (entryInstant == null) {
                entryInstant = cursor
            }
            break
        }

        cursorInside = nextInside
        cursor = nextInstant
    }

    return [entry: entryInstant, exit: exitInstant]
}

private Instant refineBoundary(Instant lowerInstant, Instant upperInstant, boolean targetInside,
        Map<Long, Map> cache) {
    Instant lower = lowerInstant
    Instant upper = upperInstant

    for (int idx = 0; idx < WINDOW_REFINE_ITERATIONS; idx++) {
        long windowMillis = upper.toEpochMilli() - lower.toEpochMilli()
        if (windowMillis <= 1L) {
            break
        }
        long midpointMillis = lower.toEpochMilli() + (windowMillis / 2L)
        Instant midpoint = Instant.ofEpochMilli(midpointMillis)
        boolean midpointInside = regionContainsAt(midpoint, cache)
        if (midpointInside == targetInside) {
            upper = midpoint
        } else {
            lower = midpoint
        }
    }

    return upper
}

private boolean regionContainsAt(Instant instant, Map<Long, Map> cache) {
    Map position = getCachedPosition(instant, cache)
    if (!position) {
        return false
    }
    Number azimuth = position.azimuth as Number
    Number altitude = position.altitude as Number
    if (azimuth == null || altitude == null) {
        return false
    }
    return regionContains(azimuth, altitude)
}

private Map getCachedPosition(Instant instant, Map<Long, Map> cache) {
    if (!instant) {
        return [:]
    }
    Long key = instant.toEpochMilli()
    Map cached = cache[key]
    if (cached) {
        return cached
    }
    Map position = fetchSunPosition(instant)
    cache[key] = position
    return position
}

private Map fetchSunPosition(Instant instant) {
    if (!instant) {
        return [:]
    }
    if (!parent) {
        log.warn 'Parent sun driver is not available for sun position queries'
        return [:]
    }
    try {
        return parent.getSunPositionAtMillis(instant.toEpochMilli()) ?: [:]
    } catch (MissingMethodException ignored) {
        log.warn 'Parent sun driver does not expose getSunPositionAtMillis(Long)'
        return [:]
    }
}

private String formatWindowValue(Instant instant) {
    if (!instant) {
        return NEVER_TIME_VALUE
    }
    return WINDOW_TIME_FORMATTER.withZone(hubZoneId()).format(instant)
}

private ZoneId hubZoneId() {
    def tz = location?.timeZone
    return tz ? tz.toZoneId() : ZoneId.systemDefault()
}

private boolean isLocationConfigured() {
    return location?.latitude != null && location?.longitude != null
}

private boolean isAzimuthInside(double value) {
    double minSetting = settings.minAzimuth != null ? settings.minAzimuth : ZERO_DEGREES
    double maxSetting = settings.maxAzimuth != null ? settings.maxAzimuth : FULL_CIRCLE_DEGREES
    double min = normalizeAzimuth(minSetting)
    double max = normalizeAzimuth(maxSetting)

    if (min <= max) {
        return value >= min && value <= max
    }
    // Wrapped window, e.g., 300°..60°
    return value >= min || value <= max
}

private boolean isAltitudeInside(double value) {
    double minSetting = settings.minAltitude != null ? settings.minAltitude : FALLBACK_MIN_ALTITUDE
    double maxSetting = settings.maxAltitude != null ? settings.maxAltitude : FALLBACK_MAX_ALTITUDE
    double low = sanitizeAltitude(minSetting)
    double high = sanitizeAltitude(maxSetting)
    if (low > high) {
        double swap = low
        low = high
        high = swap
    }
    return value >= low && value <= high
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

