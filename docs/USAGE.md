# Usage

1. **Set the update cadence** – On the main app page choose how frequently (in minutes) the sun position should refresh.
2. **Create regions** – Use the *Create New Region* flow to name the region and pick a geometry type. Fine-tune geometry from the device detail page.
3. **Monitor attributes** – Every region device exposes:
   - `inRegion` (string `'true'`/`'false'`)
   - `regionStatus`
   - `enteredRegion` and `exitedRegion` timestamps
   - `lastAzimuth`, `lastAltitude`, and driver-specific diagnostics
4. **Automate** – In Rule Machine, select the region device and use the attributes above to trigger lighting, shading, or notification routines when the sun enters/leaves a window.
5. **Manual recalculation** – From the app’s region detail page you can force a recalculation using the most recent sun reading to test geometry changes instantly.
