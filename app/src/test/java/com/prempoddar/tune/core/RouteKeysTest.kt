package com.prempoddar.tune.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RouteKeysTest {
    private val speaker = OutputDevice(2, "", "Pixel")
    private val wired = OutputDevice(4, "", "Headphones")
    private val usb = OutputDevice(22, "card=1", "USB-C dongle")
    private val bt = OutputDevice(8, "AA:BB:CC:00:11:22", "Pixel Buds")

    @Test fun speakerWhenNothingElse() {
        assertThat(RouteKeys.pick(listOf(speaker))).isEqualTo(RouteInfo("speaker", "Phone speaker"))
        assertThat(RouteKeys.pick(emptyList()).key).isEqualTo("speaker")
    }

    @Test fun bluetoothBeatsEverything() {
        assertThat(RouteKeys.pick(listOf(speaker, wired, usb, bt)))
            .isEqualTo(RouteInfo("bt:AA:BB:CC:00:11:22", "Pixel Buds"))
    }

    @Test fun usbBeatsWired() {
        assertThat(RouteKeys.pick(listOf(speaker, wired, usb)).key).isEqualTo("usb")
    }

    @Test fun wiredHeadsetAndHeadphonesShareAKey() {
        assertThat(RouteKeys.pick(listOf(speaker, OutputDevice(3, "", "Headset"))).key).isEqualTo("wired")
        assertThat(RouteKeys.pick(listOf(speaker, wired)).name).isEqualTo("Wired headphones")
    }

    @Test fun bleHeadsetIsBluetooth() {
        assertThat(RouteKeys.pick(listOf(OutputDevice(26, "11:22", "Buds"))).key).isEqualTo("bt:11:22")
    }
}
