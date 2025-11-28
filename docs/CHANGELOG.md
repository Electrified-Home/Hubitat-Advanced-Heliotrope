# Changelog

## Unreleased

- _No changes yet._

## 1.1.0 - 2025-11-28

- Added a `Calculate Solar Stats` driver command that records the next sunrise/sunset azimuths using hub data (with fallback to same-day events when needed) so users can calibrate region geometry.
- Documentation now matches the simplified runtime model where the Sky Regions app only installs/configures devices.
- Removed references to the deprecated planning helper and nonexistent `lastUpdate` attribute from README/usage docs.
- Sun Position driver description updated to cover `autoUpdate`, interval dropdown, and direct child notifications.
- Region documentation now highlights the motion-only capability and lack of auxiliary timestamps.
- Installation guide reminds users to review Sun Position preferences after running the wizard.
- Circular and rectangular region drivers now behave exactly like motion sensors—no extra attributes such as `inRegion`, `enteredRegion`, or geometry summaries remain.
- Region drivers now initialize the `motion` attribute to a blank value and rely on Hubitat’s duplicate suppression, and the README/docs were refreshed to spell that out.

## 1.0.0 - 2025-11-26

- Initial public release of Advanced Heliotrope for Hubitat
- Added parent app for configuration, scheduling, and child management
- Added Sun Position driver that computes azimuth/altitude and feeds region devices
- Added circular and rectangular region drivers with geometry controls and events
