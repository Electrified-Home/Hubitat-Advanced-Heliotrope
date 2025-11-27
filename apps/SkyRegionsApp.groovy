/**
 * Advanced Heliotrope - Sky Regions Parent App
 */

@Field static final Map REGION_TYPES = [
    circular   : [driver: 'SkyRegionCircular', description: 'Circular (center + radius)'],
    rectangular: [driver: 'SkyRegionRectangular', description: 'Rectangular (min/max azimuth & altitude)']
]
@Field static final String HUB_NAMESPACE = 'electrified-home'
@Field static final String PAGE_MAIN = 'mainPage'
@Field static final String PAGE_REGION_CREATE = 'regionCreatePage'
@Field static final String PAGE_REGION_DETAIL = 'regionDetailPage'
@Field static final String BUTTON_CREATE_REGION = 'Create Region'
@Field static final String SECTION_ACTIONS = 'Actions'
@Field static final String UNKNOWN_VALUE = 'Unknown'
@Field static final String INPUT_TYPE_TEXT = 'text'
@Field static final String INPUT_TYPE_ENUM = 'enum'
@Field static final String MESSAGE_UNABLE_ADD_REGION = 'Unable to add region device'
@Field static final String MESSAGE_REGION_REMOVAL_FAILED = 'Region removal failed'
@Field static final String SUN_DNI_PREFIX = 'AH-Sun-'
@Field static final String REGION_DNI_PREFIX = 'AH-REGION'
@Field static final String ATTR_MOTION = 'motion'
@Field static final String DATA_REGION_TYPE = 'regionType'
@Field static final String SETTING_PENDING_LABEL = 'pendingRegionLabel'
@Field static final String SETTING_PENDING_TYPE = 'pendingRegionType'
@Field static final Map ORIENTATION_SUGGESTIONS = [
    north     : [label: 'North (0°)', azimuth: 0d],
    northeast : [label: 'Northeast (45°)', azimuth: 45d],
    east      : [label: 'East (90°)', azimuth: 90d],
    southeast : [label: 'Southeast (135°)', azimuth: 135d],
    south     : [label: 'South (180°)', azimuth: 180d],
    southwest : [label: 'Southwest (225°)', azimuth: 225d],
    west      : [label: 'West (270°)', azimuth: 270d],
    northwest : [label: 'Northwest (315°)', azimuth: 315d]
]
@Field static final Map ALTITUDE_SUGGESTIONS = [
    low : [label: 'Low horizon (15°)', altitude: 15d],
    mid : [label: 'Mid sky (35°)', altitude: 35d],
    high: [label: 'High sky (55°)', altitude: 55d]
]
@Field static final Map WIDTH_SUGGESTIONS = [
    narrow: [label: 'Tight focus (radius 8°)', radius: 8d],
    medium: [label: 'Standard (radius 15°)', radius: 15d],
    wide  : [label: 'Broad window (radius 25°)', radius: 25d]
]

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
        section('Sun Device') {
            def sun = getSunDevice()
            if (sun) {
                paragraph "Sun Position device installed as '${sun.displayName}'. " +
                    'Configure update cadence directly on that device.'
            } else {
                paragraph 'Sun Position device not found.'
                appButton 'repairSunDevice', title: 'Recreate Sun Device'
            }
        }

        section('Regions') {
            def regions = regionDevices()
            if (regions) {
                regions.sort { region -> region.displayName?.toLowerCase() }.each { child ->
                    def typeKey = child.getDataValue(DATA_REGION_TYPE)
                    def motionState = child.currentValue(ATTR_MOTION) ?: UNKNOWN_VALUE
                    String typeSummary = REGION_TYPES[typeKey]?.description ?: 'Tap to view'
                    String detail = "Motion: ${motionState} • ${typeSummary}"
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

        section('Region Planning Helper') {
            input 'planningOrientation', INPUT_TYPE_ENUM,
                title: 'Window faces', options: orientationOptions(), submitOnChange: true,
                required: false
            input 'planningAltitude', INPUT_TYPE_ENUM,
                title: 'Sun height of interest', options: altitudeOptions(), submitOnChange: true,
                required: false
            input 'planningWidth', INPUT_TYPE_ENUM,
                title: 'How wide should the window be?', options: widthOptions(), submitOnChange: true,
                required: false
            paragraph planningSummary()
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
            input SETTING_PENDING_TYPE, INPUT_TYPE_ENUM, title: 'Region type', required: true,
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
            paragraph "Motion: ${child.currentValue(ATTR_MOTION) ?: UNKNOWN_VALUE}"
            paragraph "Device Id: ${child.id}"
        }

        section(SECTION_ACTIONS) {
            paragraph 'Use the region device detail page to adjust geometry inputs.'
        }

        section('Remove') {
            paragraph 'Deleting removes the region device and any automations attached to it.'
            appButton 'deleteRegionDevice', title: 'Delete Region'
        }
    }
}

def appButtonHandler(String buttonName) {
    switch (buttonName) {
        case 'createRegion':
            registerRegionDevice()
            break
        case 'deleteRegionDevice':
            deleteActiveRegion()
            break
        case 'repairSunDevice':
            repairSunDevice()
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

private void initialize() {
    state.uiMessage = state.uiMessage ?: ''
    ensureSunDevice()
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

private void repairSunDevice() {
    def sun = getSunDevice()
    if (sun) {
        state.uiMessage = 'Sun Position device already installed.'
        return
    }
    ensureSunDevice()
    state.uiMessage = 'Sun Position device recreated.'
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

private Map orientationOptions() {
    return ORIENTATION_SUGGESTIONS.collectEntries { key, meta -> [(key): meta.label] }
}

private Map altitudeOptions() {
    return ALTITUDE_SUGGESTIONS.collectEntries { key, meta -> [(key): meta.label] }
}

private Map widthOptions() {
    return WIDTH_SUGGESTIONS.collectEntries { key, meta -> [(key): meta.label] }
}

private String planningSummary() {
    def orientation = settings.planningOrientation ? ORIENTATION_SUGGESTIONS[settings.planningOrientation] : null
    def altitude = settings.planningAltitude ? ALTITUDE_SUGGESTIONS[settings.planningAltitude] : null
    def width = settings.planningWidth ? WIDTH_SUGGESTIONS[settings.planningWidth] : null

    if (!orientation && !altitude && !width) {
        return 'Choose an orientation, altitude, and width to see suggested geometry for both region types.'
    }

    double centerAz = orientation?.azimuth ?: 180d
    double centerAlt = altitude?.altitude ?: 35d
    double radius = width?.radius ?: 15d

    double minAz = normalizeAzimuthValue(centerAz - radius)
    double maxAz = normalizeAzimuthValue(centerAz + radius)
    double minAlt = Math.max(centerAlt - radius, -90d)
    double maxAlt = Math.min(centerAlt + radius, 90d)

    String circular = "Circular region → center ${formatRange(centerAz)}°/${formatRange(centerAlt)}° " +
        "with radius ${formatRange(radius)}°."
    String rectangular = "Rectangular region → azimuth ${formatRange(minAz)}° to ${formatRange(maxAz)}°, " +
        "altitude ${formatRange(minAlt)}° to ${formatRange(maxAlt)}°."
    return "${circular}\n${rectangular}"
}

private double normalizeAzimuthValue(double value) {
    double normalized = value % 360d
    return normalized < 0d ? normalized + 360d : normalized
}

private int formatRange(double value) {
    return Math.round(value as float)
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
