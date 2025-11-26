# Installation Guide

## Prerequisites

- Hubitat Elevation hub (firmware 2.2.0 or later)
- Access to your hub's web interface

## Installation Methods

### Method 1: Hubitat Package Manager (Recommended)

1. Open Hubitat Package Manager on your hub
2. Select "Install" from the menu
3. Search for "Advanced Heliotrope"
4. Follow the prompts to complete installation

### Method 2: Manual Installation

#### Install the App

1. Navigate to Apps Code in your Hubitat hub
2. Click "New App"
3. Copy the contents of `apps/advanced-heliotrope-app.groovy`
4. Click "Save"

#### Install the Drivers

1. Navigate to Drivers Code in your Hubitat hub
2. For each driver file in the `drivers/` folder:
   - Click "New Driver"
   - Copy the contents of the driver file
   - Click "Save"

## Post-Installation Setup

After installation, see the [Usage Guide](USAGE.md) for configuration instructions.
