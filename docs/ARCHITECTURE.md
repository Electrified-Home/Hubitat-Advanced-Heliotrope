# Architecture

```
SkyRegionsApp.groovy (parent app)
│
├─ SunPositionDriver.groovy (child device)
│   └─ Owns its own schedule via autoUpdate + interval preferences
│   └─ Emits azimuth/altitude events and notifies children directly
│
└─ SkyRegion*.groovy (child region devices)
	└─ Receive updateSunPosition() calls from the sun driver
	└─ Evaluate geometry and emit simple motion events
```

Key responsibilities:

- **Parent app** – Owns the device graph, exposes the configuration UI for creating regions, and ensures every region links to the shared sun driver. It does not schedule anything at runtime.
- **Sun driver** – Performs the astronomical calculations using the hub’s latitude/longitude and time zone, publishing to all children.
- **Region drivers** – Contain isolated geometry logic so adding new region types only requires a new driver file.

Communication flow:

1. Sun driver schedules `scheduledUpdate()` (or awaits manual `Refresh`) based on its preferences.
2. Driver calculates azimuth/altitude, updates its attributes (`lastCalculated`, `runtime`, etc.), and calls `updateSunPosition(az, alt)` on every region child.
3. Region devices start with a blank `motion` value and, after each sun reading, emit `active`/`inactive` updates so Rule Machine or dashboards can react exactly as they would to a basic motion sensor.
