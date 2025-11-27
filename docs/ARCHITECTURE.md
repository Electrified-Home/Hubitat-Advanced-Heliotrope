# Architecture

```
SkyRegionsApp.groovy (parent app)
│
├─ SunPositionDriver.groovy (child device)
│   └─ Schedules or receives updatePosition() calls
│   └─ Emits azimuth/altitude events and notifies parent via sunPositionUpdated()
│
└─ SkyRegion*.groovy (child region devices)
	└─ Receive updateSunPosition() calls from the app
	└─ Evaluate geometry and emit entered/exited/inRegion events
```

Key responsibilities:

- **Parent app** – Owns the device graph, exposes the configuration UI, schedules periodic updates, and fans sun data out to every region child.
- **Sun driver** – Performs the astronomical calculations using the hub’s latitude/longitude and time zone.
- **Region drivers** – Contain isolated geometry logic so adding new region types only requires a new driver file.

Communication flow:

1. App schedules `refreshSunPosition()` according to the configured interval.
2. Sun driver calculates azimuth/altitude and calls `parent.sunPositionUpdated(positionMap)`.
3. App caches the reading and invokes `updateSunPosition(az, alt)` on every region child.
4. Region devices update attributes/events that Rule Machine and dashboards can subscribe to.
