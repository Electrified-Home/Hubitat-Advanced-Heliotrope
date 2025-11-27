/*
 * Advanced Heliotrope - Circular Sky Region Driver
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
 * Advanced Heliotrope - Circular Sky Region Driver
 *
 * Describes a circular cone in the sky defined by a center azimuth/altitude and
 * a radius in degrees. Emits `motion` events that mimic a simple sensor.
 */
@Field static final String ATTR_MOTION = 'motion'
@Field static final String MOTION_ACTIVE = 'active'
@Field static final String MOTION_INACTIVE = 'inactive'
@Field static final String TYPE_NUMBER = 'NUMBER'
@Field static final String INPUT_DECIMAL = 'decimal'
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
@Field static final String NEVER_TIME_VALUE = 'Never'
@Field static final String SEARCH_STATE_SKIP_TO_EXIT = 'SKIP_TO_EXIT'
@Field static final String SEARCH_STATE_FIND_ENTRY = 'FIND_ENTRY'
@Field static final String SEARCH_STATE_FIND_EXIT = 'FIND_EXIT'
@Field static final long MILLIS_PER_DAY = 86_400_000L
@Field static final long WINDOW_STEP_MINUTES = 5L
@Field static final long WINDOW_STEP_SECONDS = 300L
@Field static final int WINDOW_REFINE_ITERATIONS = 8
@Field static final long WINDOW_SEARCH_DAYS = 2L
@Field static final DateTimeFormatter WINDOW_TIME_FORMATTER = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm')
@Field static final String COMMAND_CALCULATE_WINDOW = 'calculateNextWindow'
metadata {
    definition(
        name: 'Advanced Heliotrope Region, Circular',
        namespace: 'electrified-home',
        author: 'Electrified Home'
    ) {
        capability 'Sensor'
        capability 'MotionSensor'
        command COMMAND_CALCULATE_WINDOW
    }

    preferences {
        input 'centerAzimuth', INPUT_DECIMAL, title: 'Center azimuth (degrees)', range: RANGE_AZIMUTH,
            defaultValue: (int) DEFAULT_CENTER_AZIMUTH
        input 'centerAltitude', INPUT_DECIMAL, title: 'Center altitude (degrees)', range: RANGE_ALTITUDE,
            defaultValue: (int) DEFAULT_CENTER_ALTITUDE
        input 'radiusDegrees', INPUT_DECIMAL, title: 'Radius (degrees)', range: RANGE_RADIUS,
            defaultValue: (int) DEFAULT_RADIUS_DEGREES
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
    double distance = angularDistance(normalizedAz, sanitizedAlt, getCenterAzimuth(), getCenterAltitude())

    boolean inside = distance <= getRadius()
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
    double distance = angularDistance(normalizedAz, sanitizedAlt, getCenterAzimuth(), getCenterAltitude())
    return distance <= getRadius()
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
    Instant entryInstant = null
    Instant exitInstant = null
    Instant pendingEntry = null
    String searchState = cursorInside ? SEARCH_STATE_SKIP_TO_EXIT : SEARCH_STATE_FIND_ENTRY
    long stepSeconds = WINDOW_STEP_SECONDS

    while (cursor.isBefore(endInstant)) {
        Instant nextInstant = cursor.plusSeconds(stepSeconds)
        if (nextInstant.isAfter(endInstant)) {
            nextInstant = endInstant
        }

        boolean nextInside = regionContainsAt(nextInstant, cache)
        if (searchState == SEARCH_STATE_SKIP_TO_EXIT && cursorInside && !nextInside) {
            searchState = SEARCH_STATE_FIND_ENTRY
        } else if (searchState == SEARCH_STATE_FIND_ENTRY && !cursorInside && nextInside) {
            pendingEntry = refineBoundary(cursor, nextInstant, true, cache)
            searchState = SEARCH_STATE_FIND_EXIT
        } else if (searchState == SEARCH_STATE_FIND_EXIT && cursorInside && !nextInside) {
            exitInstant = refineBoundary(cursor, nextInstant, false, cache)
            entryInstant = pendingEntry
            break
        }

        cursorInside = nextInside
        cursor = nextInstant
    }

    if (!entryInstant && pendingEntry) {
        entryInstant = pendingEntry
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

