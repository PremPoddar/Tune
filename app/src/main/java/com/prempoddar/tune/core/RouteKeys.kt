package com.prempoddar.tune.core

/** An output device as Android reports it. [type] is an `AudioDeviceInfo.TYPE_*` value. */
data class OutputDevice(val type: Int, val address: String, val name: String)

/** The output Tune keeps levels for: a stable key plus a name to show. */
data class RouteInfo(val key: String, val name: String)

/** Picks the output audio is actually going to and gives it a stable key. */
object RouteKeys {
    // AudioDeviceInfo constants, repeated here so this file stays free of Android classes.
    private const val TYPE_WIRED_HEADSET = 3
    private const val TYPE_WIRED_HEADPHONES = 4
    private const val TYPE_BLUETOOTH_A2DP = 8
    private const val TYPE_USB_DEVICE = 11
    private const val TYPE_USB_HEADSET = 22
    private const val TYPE_HEARING_AID = 23
    private const val TYPE_BLE_HEADSET = 26
    private const val TYPE_BLE_SPEAKER = 27

    private val bluetooth = setOf(TYPE_BLUETOOTH_A2DP, TYPE_HEARING_AID, TYPE_BLE_HEADSET, TYPE_BLE_SPEAKER)
    private val usb = setOf(TYPE_USB_HEADSET, TYPE_USB_DEVICE)
    private val wired = setOf(TYPE_WIRED_HEADSET, TYPE_WIRED_HEADPHONES)

    val SPEAKER = RouteInfo(SPEAKER_ROUTE, "Phone speaker")

    fun pick(devices: List<OutputDevice>): RouteInfo {
        devices.firstOrNull { it.type in bluetooth }?.let {
            return RouteInfo("bt:${it.address}", it.name.ifBlank { "Bluetooth" })
        }
        devices.firstOrNull { it.type in usb }?.let {
            return RouteInfo("usb", it.name.ifBlank { "USB audio" })
        }
        if (devices.any { it.type in wired }) return RouteInfo("wired", "Wired headphones")
        return SPEAKER
    }
}
