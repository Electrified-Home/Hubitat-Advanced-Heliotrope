# Advanced Heliotrope Documentation

Advanced Heliotrope pairs a Hubitat parent app with dedicated drivers that keep track of the sun and determine when it enters user-defined sky regions.

## Components

- **Sky Regions App** (`apps/SkyRegionsApp.groovy`)
	- Provides the UI wizard for creating regions plus an optional planning helper for sun/window math
	- Ensures exactly one Sun Position driver exists and links each region to it
	- Stays passive at runtime—there is no scheduling or event forwarding in the app itself
- **Sun Position Driver** (`drivers/SunPositionDriver.groovy`)
	- Computes azimuth/altitude from the hub’s location on a timer managed by `autoUpdate` + interval preferences
	- Publishes `lastCalculated` to indicate when math finished and notifies every child region directly
- **Region Drivers** (`drivers/SkyRegion*.groovy`)
	- Circular: center azimuth/altitude with radius
	- Rectangular: min/max azimuth and altitude window that can wrap midnight
	- Expose only the `MotionSensor` capability, starting blank until the next sun reading then reporting `active` (inside) or `inactive` (outside)

Refer to `docs/INSTALLATION.md` for setup instructions and `docs/USAGE.md` for automation ideas.
