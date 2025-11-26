# Advanced Heliotrope

A Hubitat app and driver suite that provides advanced sun-position tracking with configurable sky regions, enabling precise automations based on solar geometry.

## Overview

Advanced Heliotrope allows you to:

- Track the sun's position (azimuth and elevation) throughout the day
- Define custom sky regions for automations
- Trigger actions based on when the sun enters or exits specific sky regions

## Project Structure

```
Hubitat-Advanced-Heliotrope/
├── apps/
│   └── advanced-heliotrope-app.groovy    # Parent app
├── drivers/
│   ├── advanced-heliotrope-driver.groovy      # Parent driver
│   ├── advanced-heliotrope-sky-region.groovy  # Child driver for sky regions
│   └── advanced-heliotrope-sun-position.groovy # Child driver for sun position
├── docs/
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

See the [Installation Guide](docs/INSTALLATION.md) for detailed installation instructions.

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
