# Hubitat-Advanced-Heliotrope

A Hubitat app and driver suite that provides advanced sun-position tracking with configurable sky regions, enabling precise automations based on solar geometry.

## Features

- Track the sun's position (azimuth and elevation) throughout the day
- Define custom sky regions for automations
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
