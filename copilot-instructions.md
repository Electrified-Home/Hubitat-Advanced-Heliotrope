# Copilot Instructions

## Overview

This repository contains **Advanced Heliotrope**, a Hubitat integration for tracking the sun’s position and determining whether it enters user-defined sky regions.

The system consists of:

* A **host/parent app** that manages configuration and creates region devices
* A **sun position driver** that calculates azimuth and altitude
* One or more **sky region drivers**, each representing a different region type (circular, rectangular, etc.)

## Architecture

Use this structure:

* Parent App: `SkyRegionsApp.groovy`
* Sun Position Driver: `SunPositionDriver.groovy`
* Region Drivers:

  * `SkyRegionCircular.groovy`
  * `SkyRegionRectangular.groovy`
  * Future types use `SkyRegion<Type>.groovy`

The host app creates region devices and updates them when sun position changes.
The sun position driver computes azimuth/altitude.
Region drivers determine whether the sun is inside their defined region and update state accordingly.

## Development Guidance

* Follow standard Hubitat app/driver patterns.
* Keep naming simple and descriptive.
* Keep each region type in its own driver.
* Keep the design modular so new region types can be added later.
* Implement logic cleanly without over-optimizing or over-complicating.

## Flexibility

You may implement logic, UI pages, scheduling, and event handling as needed.
Do not restructure the project layout unless necessary.

## Goal

Produce a clear, maintainable, fully functional Hubitat integration using the structure above.
