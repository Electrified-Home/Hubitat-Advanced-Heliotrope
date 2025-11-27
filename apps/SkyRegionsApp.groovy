/*
 * Advanced Heliotrope Application
 * Copyright (c) 2024-2025 Electrified Home
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */


import groovy.transform.Field

/* groovylint-disable MethodCount */

@Field static final String APP_NAME = 'Advanced Heliotrope'
@Field static final Map REGION_TYPES = [
    circular: [
        driver      : 'Advanced Heliotrope Region, Circular',
        description : 'Circular (center + radius)'
    ],
    rectangular: [
        driver      : 'Advanced Heliotrope Region, Rectangular',
        description : 'Rectangular (min/max azimuth & altitude)'
    ]
]
@Field static final String HUB_NAMESPACE = 'electrified-home'
@Field static final String PAGE_MAIN = 'mainPage'
@Field static final String PAGE_SUN_DEVICE = 'sunDevicePage'
@Field static final String PAGE_REGION_CREATE = 'regionCreatePage'
@Field static final String PAGE_REGION_CREATE_CONFIRM = 'regionCreateConfirmPage'
@Field static final String PAGE_REGION_DETAIL = 'regionDetailPage'
@Field static final String PAGE_REGION_DELETE = 'regionDeletePage'
@Field static final String SECTION_STATUS = 'Status'
@Field static final String SECTION_RESULT = 'Result'
@Field static final String SECTION_NEXT_STEPS = 'Next Steps'
@Field static final String BUTTON_DELETE_REGION = 'Delete Region'
@Field static final String LINK_BACK_TO_REGIONS = 'Back to Regions'
@Field static final String DESCRIPTION_RETURN_MAIN = 'Return to the main overview'
@Field static final String ACTION_REPAIR = 'repair'
@Field static final String BUTTON_CREATE_REGION = 'Create Region'
@Field static final String SECTION_ACTIONS = 'Actions'
@Field static final String UNKNOWN_VALUE = 'Unknown'
@Field static final String INPUT_TYPE_TEXT = 'text'
@Field static final String INPUT_TYPE_ENUM = 'enum'
@Field static final String MESSAGE_UNABLE_ADD_REGION = 'Unable to add region device'
@Field static final String MESSAGE_REGION_REMOVAL_FAILED = 'Region removal failed'
@Field static final String MESSAGE_SUN_NOT_FOUND = 'Sun Position device not found.'
@Field static final String MESSAGE_UNKNOWN_ERROR = 'Unknown error'
@Field static final String SUN_DNI_PREFIX = 'AH-Sun-'
@Field static final String REGION_DNI_PREFIX = 'AH-REGION'
@Field static final String DRIVER_SUN_POSITION = 'Advanced Heliotrope Driver'
@Field static final String ATTR_MOTION = 'motion'
@Field static final String ATTR_SUN_AZIMUTH = 'azimuth'
@Field static final String ATTR_SUN_ALTITUDE = 'altitude'
@Field static final String DATA_REGION_TYPE = 'regionType'
@Field static final String SETTING_PENDING_LABEL = 'pendingRegionLabel'
@Field static final String SETTING_PENDING_TYPE = 'pendingRegionType'
@Field static final Map DEFAULT_CIRCULAR = [azimuth: 180d, altitude: 35d, radius: 15d]
@Field static final Map DEFAULT_RECTANGULAR = [minAz: 90d, maxAz: 270d, minAlt: 0d, maxAlt: 70d]
@Field static final String DEVICE_EDIT_PATH = '/device/edit/'
@Field static final String STYLE_EXTERNAL = 'external'
@Field static final String TITLE_CREATE_SUN_DEVICE = 'Create Sun Device'

definition(
    name: APP_NAME,
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
    page(name: PAGE_SUN_DEVICE)
    page(name: PAGE_REGION_CREATE)
    page(name: PAGE_REGION_CREATE_CONFIRM)
    page(name: PAGE_REGION_DETAIL)
    page(name: PAGE_REGION_DELETE)
}

def mainPage() {
    dynamicPage(name: PAGE_MAIN, title: APP_NAME, uninstall: true, install: true) {
        section('Sun Device') {
            def sun = getSunDevice()
            if (sun) {
                paragraph "Sun Position device installed as '${sun.displayName}'. " +
                    'Configure update cadence directly on that device.'
                href name: 'sunDeviceLink', title: 'Open Sun Device Page',
                    description: 'Opens the Hubitat device detail view in a new tab',
                    style: STYLE_EXTERNAL, url: deviceEditUrl(sun.id)
            } else {
                paragraph MESSAGE_SUN_NOT_FOUND
                href name: 'sunDeviceCreateAction', title: TITLE_CREATE_SUN_DEVICE,
                    description: 'Create the sun device now',
                    page: PAGE_SUN_DEVICE
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
                    href name: "regionDeviceLink${child.id}", title: child.displayName,
                        description: detail,
                        style: STYLE_EXTERNAL, url: deviceEditUrl(child.id)
                }
            } else {
                paragraph 'No regions configured yet.'
            }
        }

        section('Add Region') {
            href PAGE_REGION_CREATE, title: 'Create New Region', description: 'Guided creation wizard'
        }

        section(SECTION_STATUS) {
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
            paragraph 'Tap Create once the label and type are chosen. Adjust geometry later on the device page.'
            href name: 'createRegionConfirm', title: BUTTON_CREATE_REGION,
                description: 'Use the values above to create a region device',
                page: PAGE_REGION_CREATE_CONFIRM, params: [token: UUID.randomUUID().toString()]
        }

        section('After Creation') {
            paragraph 'Open the region device in Hubitat to fine tune center azimuth, altitude, and width.'
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
            href name: 'openRegionDevice', title: 'Open Region Device Page',
                description: 'Jump to the region device detail view in Hubitat',
                style: STYLE_EXTERNAL, url: deviceEditUrl(child.id)
        }

        section(SECTION_ACTIONS) {
            paragraph 'Use the region device detail page to adjust geometry inputs.'
        }

        section('Remove') {
            paragraph 'Deleting removes the region device and any automations attached to it.'
            href name: 'deleteRegionConfirm', title: BUTTON_DELETE_REGION,
                description: 'Permanently remove this region device',
                page: PAGE_REGION_DELETE, params: [deviceId: child.id, token: UUID.randomUUID().toString()]
        }
    }
}

def sunDevicePage(Map params) {
    String token = params?.token
    boolean wantsRepair = params?.action == ACTION_REPAIR
    if (wantsRepair && token && state.lastSunRepairToken != token) {
        state.lastSunRepairToken = token
        repairSunDevice()
    }

    def sun = getSunDevice()
    dynamicPage(name: PAGE_SUN_DEVICE, title: 'Sun Device Maintenance') {
        section('Current Device') {
            if (sun) {
                paragraph "Device: ${sun.displayName} (${sun.deviceNetworkId})"
                paragraph 'Use the device detail page to adjust logging and scheduling.'
            } else {
                paragraph 'No Sun Position device is currently installed.'
            }
        }

        section(SECTION_ACTIONS) {
            if (sun) {
                href name: 'returnFromSunMaintenance', title: LINK_BACK_TO_REGIONS,
                    description: DESCRIPTION_RETURN_MAIN, page: PAGE_MAIN
            } else {
                href name: 'sunDeviceRepairAction', title: TITLE_CREATE_SUN_DEVICE,
                    description: 'Create the sun device if it is missing or unresponsive',
                    page: PAGE_SUN_DEVICE, params: [action: ACTION_REPAIR, token: UUID.randomUUID().toString()]
            }
        }

        section(SECTION_STATUS) {
            paragraph state.uiMessage ?: 'No recent sun device actions.'
        }
    }
}

def regionCreateConfirmPage(Map params) {
    String token = params?.token
    if (token && state.lastRegionCreateToken != token) {
        state.lastRegionCreateToken = token
        registerRegionDevice()
    }

    String message = state.uiMessage ?: 'Provide both a label and region type before creating a device.'
    dynamicPage(name: PAGE_REGION_CREATE_CONFIRM, title: 'Region Creation Result') {
        section(SECTION_RESULT) {
            paragraph message
        }

        section(SECTION_NEXT_STEPS) {
            href name: 'createAnotherRegion', title: 'Create Another Region',
                description: 'Return to the creation wizard', page: PAGE_REGION_CREATE
            href name: 'returnToMainFromCreate', title: LINK_BACK_TO_REGIONS,
                description: DESCRIPTION_RETURN_MAIN, page: PAGE_MAIN
        }
    }
}

def regionDeletePage(Map params) {
    def deviceId = params?.deviceId ?: state.activeRegionDeviceId
    if (params?.deviceId) {
        state.activeRegionDeviceId = params.deviceId
    }
    String token = params?.token
    if (deviceId && token && state.lastRegionDeleteToken != token) {
        state.lastRegionDeleteToken = token
        deleteRegionDeviceById(deviceId)
    }

    String message = state.uiMessage ?: 'Select a region before attempting deletion.'
    dynamicPage(name: PAGE_REGION_DELETE, title: BUTTON_DELETE_REGION) {
        section(SECTION_RESULT) {
            paragraph message
        }

        section(SECTION_NEXT_STEPS) {
            href name: 'returnToRegionDetail', title: 'Back to Region',
                description: 'Return to the previous region detail view',
                page: PAGE_REGION_DETAIL, params: [deviceId: state.activeRegionDeviceId]
            href name: 'returnToMainFromDelete', title: LINK_BACK_TO_REGIONS,
                description: DESCRIPTION_RETURN_MAIN, page: PAGE_MAIN
        }
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
    unsubscribe()
    ensureSunDevice()
}

private void ensureSunDevice() {
    String dni = ensureSunDeviceDni()
    if (!childDevices?.find { device -> device.deviceNetworkId == dni }) {
        try {
            def sunDevice = addChildDevice(
                HUB_NAMESPACE,
                DRIVER_SUN_POSITION,
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
    state.uiMessage = 'Sun Position device created.'
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

    def sun = getSunDevice()
    if (!sun) {
        state.uiMessage = MESSAGE_SUN_NOT_FOUND
        return
    }

    Map geometry = pendingGeometryValues(typeKey)

    def result = sun.registerRegionDevice(label, typeKey, geometry)
    if (result?.success) {
        state.uiMessage = "Region ${label} created. Configure geometry on the device page."
        clearRegionBuilderInputs()
    } else {
        String reason = result?.message ?: MESSAGE_UNKNOWN_ERROR
        state.uiMessage = "Failed to create region: ${reason}"
        log.error MESSAGE_UNABLE_ADD_REGION
    }
}

private void deleteRegionDeviceById(Object deviceId = null) {
    def child = deviceId ? findRegionById(deviceId) : getActiveRegionDevice()
    if (!child) {
        state.uiMessage = 'No region selected for removal.'
        return
    }
    def sun = getSunDevice()
    if (!sun) {
        state.uiMessage = MESSAGE_SUN_NOT_FOUND
        return
    }
    def result = sun.removeRegionDeviceById(child.id)
    if (result?.success) {
        state.uiMessage = "Region ${child.displayName} deleted."
    } else {
        String reason = result?.message ?: MESSAGE_UNKNOWN_ERROR
        state.uiMessage = "Unable to delete region: ${reason}"
        log.warn MESSAGE_REGION_REMOVAL_FAILED
    }
}

private Map pendingGeometryValues(String typeKey) {
    switch (typeKey) {
        case 'circular':
            return [
                centerAzimuth : DEFAULT_CIRCULAR.azimuth,
                centerAltitude: DEFAULT_CIRCULAR.altitude,
                radiusDegrees : DEFAULT_CIRCULAR.radius
            ]
        case 'rectangular':
            return [
                minAzimuth : DEFAULT_RECTANGULAR.minAz,
                maxAzimuth : DEFAULT_RECTANGULAR.maxAz,
                minAltitude: DEFAULT_RECTANGULAR.minAlt,
                maxAltitude: DEFAULT_RECTANGULAR.maxAlt
            ]
        default:
            return [:]
    }
}

private void clearRegionBuilderInputs() {
    app.updateSetting(SETTING_PENDING_LABEL, [value: '', type: INPUT_TYPE_TEXT])
    app.updateSetting(SETTING_PENDING_TYPE, '')
}

private Object getSunDevice() {
    String dni = state.sunDeviceDni ?: ensureSunDeviceDni()
    return childDevices?.find { device -> device.deviceNetworkId == dni }
}

private List regionDevices() {
    def sun = getSunDevice()
    return sun?.getChildDevices()?.findAll { device -> device.deviceNetworkId?.startsWith(REGION_DNI_PREFIX) } ?: []
}

private Map regionTypeOptions() {
    return REGION_TYPES.collectEntries { key, meta -> [(key): meta.description] }
}

private String deviceEditUrl(Object deviceId) {
    return "${DEVICE_EDIT_PATH}${deviceId}"
}

private Object getActiveRegionDevice() {
    return findRegionById(state.activeRegionDeviceId)
}

private String ensureSunDeviceDni() {
    state.sunDeviceDni = state.sunDeviceDni ?: "${SUN_DNI_PREFIX}${app.id}"
    return state.sunDeviceDni
}

private Object findRegionById(Object deviceId) {
    if (!deviceId) {
        return null
    }
    return regionDevices().find { device -> "${device.id}" == "${deviceId}" }
}
