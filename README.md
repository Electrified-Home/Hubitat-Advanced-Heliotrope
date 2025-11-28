# Hubitat-Advanced-Heliotrope

A Hubitat app and driver suite that provides advanced sun-position tracking with configurable sky regions, enabling precise automations based on solar geometry.

## Features

- Track the sun's position (azimuth and elevation) throughout the day
- Sun Position driver manages its own cadence via an `autoUpdate` toggle and dropdown covering 1 minute through 3 hours, publishing a `lastCalculated` timestamp with every reading
- Manual `Calculate Solar Stats` command captures the next sunrise/sunset azimuths (state variables) using Hubitat's sun data plus fallback logic, making it easy to calibrate window regions
- Define custom sky regions for automations
- Motion-capable region devices (`SkyRegionCircular`, `SkyRegionRectangular`) behave exactly like a motion sensor—state stays blank until the next sun reading, then flips to `active` (inside) or `inactive` (outside)
- Region creation wizard keeps configuration lightweight—set the label/type in the app, then fine tune geometry directly on each region device page
- Trigger actions based on when the sun enters or exits specific sky regions

## Project Structure

```
Hubitat-Advanced-Heliotrope/
├── apps/
│   └── SkyRegionsApp.groovy
├── drivers/
│   ├── SunPositionDriver.groovy
│   ├── SkyRegionCircular.groovy
│   └── SkyRegionRectangular.groovy
├── docs/
│   ├── README.md
│   ├── INSTALLATION.md
│   ├── USAGE.md
│   ├── ARCHITECTURE.md
│   └── CHANGELOG.md
├── .github/
│   └── workflows/
├── packageManifest.json
├── LICENSE
└── README.md
```

## Documentation

- [Installation Guide](docs/INSTALLATION.md)
- [Usage Guide](docs/USAGE.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Changelog](docs/CHANGELOG.md)
- [Documentation Overview](docs/README.md)

## Installation

### Hubitat Package Manager (Recommended)

1. Open Hubitat Package Manager on your hub
2. Select "Install" from the menu
3. Search for "Advanced Heliotrope"
4. Follow the prompts to complete installation

### Manual Installation

See the [Installation Guide](docs/INSTALLATION.md) for manual installation instructions.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
