# Usage

1. **Configure the Sun Position driver** – Open the lone sun device that the app created and choose whether `autoUpdate` should stay on. Pick an interval (1 minute through 3 hours) from the dropdown; this schedule now lives entirely on the driver.
2. **Create regions** – Use the *Create New Region* flow and, after the device is created, fine-tune the geometry inputs directly on that region’s device page.
3. **Monitor attributes** – Region devices expose a single useful attribute: `motion`. It stays blank until the next sun reading, then reports `active` when the sun is inside the geometry and `inactive` when it is outside—nothing else is published to keep the surface minimal.
4. **Automate** – In Rule Machine or Simple Automation Rules, treat the region device exactly like a motion sensor to trigger lighting, shading, or notifications when the sun enters/leaves a window.
5. **Manual recalculation and stats** – Use the sun driver’s `Refresh` or `Update Position` command to recompute immediately; results propagate straight to every child region. When you need the azimuths for the next sunrise/sunset (e.g., to fine tune a window region), tap `Calculate Solar Stats`. The driver logs the stored angles and exposes them as state variables on the device details page.
