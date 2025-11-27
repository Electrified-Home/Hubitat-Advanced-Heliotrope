# Advanced Heliotrope Documentation

Advanced Heliotrope pairs a Hubitat parent app with dedicated drivers that keep track of the sun and determine when it enters user-defined sky regions.

## Components

- **Sky Regions App** (`apps/SkyRegionsApp.groovy`)
	- Provides the UI wizard for creating regions
	- Manages the lone Sun Position device and all region child devices
	- Schedules periodic updates and propagates the calculated sun data
- **Sun Position Driver** (`drivers/SunPositionDriver.groovy`)
	- Computes azimuth/altitude from the hub’s location
	- Reports sunrise/sunset metadata and measurement runtime
- **Region Drivers** (`drivers/SkyRegion*.groovy`)
	- Circular: center azimuth/altitude with radius
	- Rectangular: min/max azimuth and altitude window that can wrap midnight
	- Emit `inRegion`, `enteredRegion`, and `exitedRegion` attributes for Rule Machine

Refer to `docs/INSTALLATION.md` for setup instructions and `docs/USAGE.md` for automation ideas.
