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
metadata {
    definition(name: 'Advanced Heliotrope Circular Region', namespace: 'electrified-home', author: 'Electrified Home') {
        capability 'Sensor'
        capability 'MotionSensor'

        command 'updateSunPosition', [[name: 'Azimuth', type: TYPE_NUMBER], [name: 'Altitude', type: TYPE_NUMBER]]
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

private void initialize() {
    sendEvent(name: ATTR_MOTION, value: '')
}

private void applyRegionState(boolean inside) {
    sendEvent(name: ATTR_MOTION, value: inside ? MOTION_ACTIVE : MOTION_INACTIVE)
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

