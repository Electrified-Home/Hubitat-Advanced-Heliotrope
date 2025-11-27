# Installation

## Hubitat Package Manager (Recommended)

1. Open Hubitat Package Manager on your hub.
2. Choose **Install** → **From a URL** and paste the manifest link:
	`https://raw.githubusercontent.com/Electrified-Home/Hubitat-Advanced-Heliotrope/main/packageManifest.json`
3. Select **Advanced Heliotrope** and follow the prompts. HPM will install the parent app plus all required drivers.
4. After installation, open the Apps list on your hub and launch **Advanced Heliotrope - Sky Regions** to begin configuration.

## Manual Installation

1. In the Hubitat UI, go to **Drivers Code** → **New Driver**, paste each driver file from the `drivers/` folder, and click **Save**.
2. Go to **Apps Code** → **New App**, paste `apps/SkyRegionsApp.groovy`, and click **Save**.
3. From **Apps**, click **Add User App** and select **Advanced Heliotrope - Sky Regions**.
4. Complete the setup wizard to create the Sun Position device and your first regions.
