/**
 * Advanced Heliotrope - Sky Regions Parent App
 */

@Field static final Map REGION_TYPES = [
    circular   : [driver: 'SkyRegionCircular', description: 'Circular (center + radius)'],
    rectangular: [driver: 'SkyRegionRectangular', description: 'Rectangular (min/max azimuth & altitude)']
]
@Field static final Integer DEFAULT_UPDATE_INTERVAL = 1
@Field static final int SECONDS_PER_MINUTE = 60
@Field static final int MILLIS_PER_SECOND = 1000
@Field static final String HUB_NAMESPACE = 'electrified-home'
@Field static final String PAGE_MAIN = 'mainPage'
@Field static final String PAGE_REGION_CREATE = 'regionCreatePage'
@Field static final String PAGE_REGION_DETAIL = 'regionDetailPage'
@Field static final String BUTTON_CREATE_REGION = 'Create Region'
@Field static final String SECTION_ACTIONS = 'Actions'
@Field static final String UNKNOWN_VALUE = 'Unknown'
@Field static final String VALUE_NOT_AVAILABLE = 'n/a'
@Field static final String INPUT_TYPE_TEXT = 'text'
@Field static final String MESSAGE_UNABLE_ADD_REGION = 'Unable to add region device'
@Field static final String MESSAGE_REGION_REMOVAL_FAILED = 'Region removal failed'
@Field static final String HANDLER_SCHEDULED_SUN = 'scheduledSunUpdate'
@Field static final String SUN_DNI_PREFIX = 'AH-Sun-'
@Field static final String REGION_DNI_PREFIX = 'AH-REGION'
@Field static final String ATTR_REGION_STATUS = 'regionStatus'
@Field static final String ATTR_REGION_SUMMARY = 'regionSummary'
@Field static final String ATTR_LAST_AZIMUTH = 'lastAzimuth'
@Field static final String ATTR_LAST_ALTITUDE = 'lastAltitude'
@Field static final String ATTR_IN_REGION = 'inRegion'
@Field static final String DATA_REGION_TYPE = 'regionType'
@Field static final String SETTING_PENDING_LABEL = 'pendingRegionLabel'
@Field static final String SETTING_PENDING_TYPE = 'pendingRegionType'

definition(
    name: 'Advanced Heliotrope - Sky Regions',
    namespace: HUB_NAMESPACE,
    author: 'Electrified Home',
    description: 'Parent app that manages sun tracking and sky regions',
    category: 'Convenience',
    iconUrl: '',
    iconX2Url: '',
    importUrl: ''
)

preferences {
    page(name: PAGE_MAIN)
    page(name: PAGE_REGION_CREATE)
    page(name: PAGE_REGION_DETAIL)
}

def mainPage() {
    dynamicPage(name: PAGE_MAIN, title: 'Advanced Heliotrope', uninstall: true, install: true) {
        section('Sun Tracking') {
            input 'updateIntervalMinutes', 'number', title: 'Sun update interval (minutes)', required: true,
                range: '1..60', defaultValue: (settings.updateIntervalMinutes ?: 1)
            if (state.lastSunPosition) {
                def reading = state.lastSunPosition
                String summary = "Last update: ${reading.timestamp}\n" +
                    "Azimuth ${reading.azimuth}°, Altitude ${reading.altitude}°"
                paragraph summary
            } else {
                paragraph 'Sun position has not been calculated yet.'
            }
            appButton 'refreshSunPositionNow', title: 'Refresh Now'
        }

        section('Regions') {
            def regions = regionDevices()
            if (regions) {
                regions.sort { region -> region.displayName?.toLowerCase() }.each { child ->
                    def status = child.currentValue(ATTR_REGION_STATUS) ?: UNKNOWN_VALUE
                    def typeKey = child.getDataValue(DATA_REGION_TYPE)
                    def regionSummary = child.currentValue(ATTR_REGION_SUMMARY) ?: REGION_TYPES[typeKey]?.description
                    String detail = "${status} • ${regionSummary ?: 'Tap to view'}"
                    href PAGE_REGION_DETAIL, title: child.displayName,
                        description: detail, params: [deviceId: child.id]
                }
            } else {
                paragraph 'No regions configured yet.'
            }
        }

        section('Add Region') {
            href PAGE_REGION_CREATE, title: 'Create New Region', description: 'Guided creation wizard'
        }

        section('Status') {
            String message = state.uiMessage ?: 'All systems ready.'
            paragraph message
        }
    }
}

def regionCreatePage() {
    dynamicPage(name: PAGE_REGION_CREATE, title: BUTTON_CREATE_REGION) {
        section('Region Basics') {
            input SETTING_PENDING_LABEL, INPUT_TYPE_TEXT, title: 'Region label', required: true,
                submitOnChange: true, defaultValue: settings[SETTING_PENDING_LABEL]
            input SETTING_PENDING_TYPE, 'enum', title: 'Region type', required: true,
                options: regionTypeOptions(), submitOnChange: true, defaultValue: settings[SETTING_PENDING_TYPE]
        }

        section(SECTION_ACTIONS) {
            paragraph 'Tap Create once all fields are complete. Geometry is configured on the device after creation.'
            appButton 'createRegion', title: BUTTON_CREATE_REGION
        }

        section('Notes') {
            paragraph 'Region drivers expose geometry inputs such as radius and azimuth on their device detail pages.'
        }
    }
}

def regionDetailPage(Map params) {
    def deviceId = params?.deviceId ?: state.activeRegionDeviceId
    if (params?.deviceId) {
        state.activeRegionDeviceId = params.deviceId
    }

    def child = findRegionById(deviceId)
    dynamicPage(name: PAGE_REGION_DETAIL, title: child?.displayName ?: 'Region') {
        if (!child) {
            section { paragraph 'Region was not found.' }
            return
        }

        def typeKey = child.getDataValue(DATA_REGION_TYPE)
        def typeMeta = REGION_TYPES[typeKey]
        section('Overview') {
            paragraph "Type: ${typeMeta?.description ?: child.typeName}"
            paragraph "Status: ${child.currentValue(ATTR_REGION_STATUS) ?: UNKNOWN_VALUE}"
            paragraph "Device Id: ${child.id}"
        }

        section('Recent Readings') {
            paragraph "Last azimuth: ${child.currentValue(ATTR_LAST_AZIMUTH) ?: VALUE_NOT_AVAILABLE}°"
            paragraph "Last altitude: ${child.currentValue(ATTR_LAST_ALTITUDE) ?: VALUE_NOT_AVAILABLE}°"
            paragraph "In region: ${child.currentValue(ATTR_IN_REGION) ?: 'false'}"
        }

        section(SECTION_ACTIONS) {
            paragraph 'Use the region device detail page to adjust geometry inputs.'
            appButton 'recalculateRegion', title: 'Recalculate Now'
        }

        section('Remove') {
            paragraph 'Deleting removes the region device and any automations attached to it.'
            appButton 'deleteRegionDevice', title: 'Delete Region'
        }
    }
}

def appButtonHandler(String buttonName) {
    switch (buttonName) {
        case 'refreshSunPositionNow':
            refreshSunPosition()
            state.uiMessage = 'Manual sun refresh requested.'
            break
        case 'createRegion':
            registerRegionDevice()
            break
        case 'recalculateRegion':
            forceRegionRecalculation()
            break
        case 'deleteRegionDevice':
            deleteActiveRegion()
            break
    }
}

def installed() {
    log.info 'Advanced Heliotrope app installed'
    initialize()
}

def updated() {
    log.info 'Advanced Heliotrope app updated'
    initialize()
}

def uninstalled() {
    try {
        childDevices?.each { device -> deleteChildDevice(device.deviceNetworkId) }
    } catch (IllegalArgumentException | IllegalStateException ex) {
        log.warn "Unable to delete child device: ${ex.message}"
    }
}

def scheduledSunUpdate() {
    refreshSunPosition()
    scheduleSunUpdates()
}

def refreshSunPosition() {
    def sun = getSunDevice()
    if (!sun) {
        log.warn 'Sun device missing; recreating'
        ensureSunDevice()
        return
    }
    sun.updatePosition()
}

def sunPositionUpdated(Map position) {
    if (!position) {
        log.warn 'Sun position payload missing'
        return
    }
    state.lastSunPosition = position
    propagateSunPosition(position)
}

private void initialize() {
    unschedule()
    state.uiMessage = state.uiMessage ?: ''
    ensureSunDevice()
    scheduleSunUpdates()
    runIn(5, 'refreshSunPosition')
}

private void ensureSunDevice() {
    String dni = ensureSunDeviceDni()
    if (!childDevices?.find { device -> device.deviceNetworkId == dni }) {
        try {
            def sunDevice = addChildDevice(
                HUB_NAMESPACE,
                'SunPositionDriver',
                dni,
                [label: 'Sun Position', isComponent: false]
            )
            log.info "Created sun position child device ${sunDevice.displayName}"
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log.error "Unable to create sun device: ${ex.message}"
        }
    }
}

private void scheduleSunUpdates() {
    int minutes = getUpdateInterval()
    state.updateIntervalMinutes = minutes
    state.nextSunSchedule = now() + (minutes * SECONDS_PER_MINUTE * MILLIS_PER_SECOND)
    unschedule(HANDLER_SCHEDULED_SUN)
    runIn(minutes * SECONDS_PER_MINUTE, HANDLER_SCHEDULED_SUN)
}

private void propagateSunPosition(Map position) {
    regionDevices().each { child ->
        try {
            BigDecimal azimuth = position.azimuth as BigDecimal
            BigDecimal altitude = position.altitude as BigDecimal
            child.updateSunPosition(azimuth, altitude)
        } catch (MissingMethodException ignored) {
            log.warn "${child.displayName} does not implement updateSunPosition"
        } catch (NumberFormatException | ClassCastException ex) {
            log.warn "Region ${child.displayName} rejected sun update: ${ex.message}", ex
        }
    }
}

private void registerRegionDevice() {
    def label = settings[SETTING_PENDING_LABEL]?.trim()
    def typeKey = settings[SETTING_PENDING_TYPE]
    if (!label || !typeKey) {
        state.uiMessage = 'Provide both a label and region type.'
        return
    }

    def driverName = REGION_TYPES[typeKey]?.driver
    if (!driverName) {
        state.uiMessage = "Unsupported region type ${typeKey}."
        return
    }

    def dni = generateRegionDni(typeKey)
    try {
        def device = addChildDevice(HUB_NAMESPACE, driverName, dni, [label: label, isComponent: false])
        device.updateDataValue(DATA_REGION_TYPE, typeKey)
        state.uiMessage = "Region ${label} created. Configure geometry on the device page."
        app.updateSetting(SETTING_PENDING_LABEL, [value: '', type: INPUT_TYPE_TEXT])
        app.updateSetting(SETTING_PENDING_TYPE, '')
    } catch (IllegalArgumentException | IllegalStateException ex) {
        state.uiMessage = "Failed to create region: ${ex.message}"
        log.error MESSAGE_UNABLE_ADD_REGION, ex
    }
}

private void forceRegionRecalculation() {
    def child = getActiveRegionDevice()
    if (!child) {
        state.uiMessage = 'Select a region first.'
        return
    }
    if (!state.lastSunPosition) {
        state.uiMessage = 'Sun position unavailable.'
        return
    }
    try {
        BigDecimal azimuth = state.lastSunPosition.azimuth as BigDecimal
        BigDecimal altitude = state.lastSunPosition.altitude as BigDecimal
        child.updateSunPosition(azimuth, altitude)
        state.uiMessage = "Region ${child.displayName} recalculated."
    } catch (NumberFormatException | ClassCastException ex) {
        state.uiMessage = "Failed to recalc region: ${ex.message}"
    }
}

private void deleteActiveRegion() {
    def child = getActiveRegionDevice()
    if (!child) {
        state.uiMessage = 'No region selected for removal.'
        return
    }
    try {
        deleteChildDevice(child.deviceNetworkId)
        state.uiMessage = "Region ${child.displayName} deleted."
    } catch (IllegalArgumentException | IllegalStateException ex) {
        state.uiMessage = "Unable to delete region: ${ex.message}"
        log.warn MESSAGE_REGION_REMOVAL_FAILED, ex
    }
}

private int getUpdateInterval() {
    int minutes = (settings.updateIntervalMinutes ?: DEFAULT_UPDATE_INTERVAL) as int
    return minutes < 1 ? DEFAULT_UPDATE_INTERVAL : minutes
}

private Object getSunDevice() {
    String dni = state.sunDeviceDni ?: ensureSunDeviceDni()
    return childDevices?.find { device -> device.deviceNetworkId == dni }
}

private List regionDevices() {
    return childDevices?.findAll { device -> device.deviceNetworkId?.startsWith(REGION_DNI_PREFIX) } ?: []
}

private Map regionTypeOptions() {
    return REGION_TYPES.collectEntries { key, meta -> [(key): meta.description] }
}

private Object getActiveRegionDevice() {
    return findRegionById(state.activeRegionDeviceId)
}

private String ensureSunDeviceDni() {
    state.sunDeviceDni = state.sunDeviceDni ?: "${SUN_DNI_PREFIX}${app.id}"
    return state.sunDeviceDni
}

private String generateRegionDni(String typeKey) {
    String normalized = typeKey?.toUpperCase() ?: 'UNKNOWN'
    return "${REGION_DNI_PREFIX}-${normalized}-${UUID.randomUUID()}"
}

private Object findRegionById(Object deviceId) {
    if (!deviceId) {
        return null
    }
    return regionDevices().find { device -> "${device.id}" == "${deviceId}" }
}
