# Architecture

## Overview

Advanced Heliotrope uses a parent-child architecture to manage sun position tracking and sky region monitoring.

## Components

### Parent App

`apps/advanced-heliotrope-app.groovy`

The parent app is responsible for:
- Managing user configuration
- Creating and managing child devices
- Coordinating updates across components

### Parent Driver

`drivers/advanced-heliotrope-driver.groovy`

The parent driver provides:
- Central coordination for child drivers
- Shared data and calculations

### Child Drivers

#### Sky Region Driver

`drivers/advanced-heliotrope-sky-region.groovy`

Each sky region child device:
- Represents a defined area of the sky
- Tracks whether the sun is currently in the region
- Provides attributes for automation triggers

#### Sun Position Driver

`drivers/advanced-heliotrope-sun-position.groovy`

The sun position child device:
- Calculates and reports current sun azimuth
- Calculates and reports current sun elevation
- Updates at configurable intervals

## Data Flow

1. Parent app manages configuration and creates child devices
2. Sun position driver calculates current sun position
3. Sky region drivers evaluate if sun is within their boundaries
4. Attribute changes trigger automations
