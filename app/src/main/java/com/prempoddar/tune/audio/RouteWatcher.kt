package com.prempoddar.tune.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.prempoddar.tune.core.OutputDevice
import com.prempoddar.tune.core.RouteInfo
import com.prempoddar.tune.core.RouteKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Follows which output media is currently playing through. */
class RouteWatcher(context: Context) {

    private val am = context.getSystemService(AudioManager::class.java)
    private val _route = MutableStateFlow(compute())
    val route: StateFlow<RouteInfo> = _route.asStateFlow()

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) = refresh()
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) = refresh()
    }

    fun start() {
        am.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun stop() = am.unregisterAudioDeviceCallback(callback)

    fun refresh() {
        _route.value = compute()
    }

    private fun compute(): RouteInfo {
        val devices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The device media would actually play through right now.
            val media = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
            am.getAudioDevicesForAttributes(media).ifEmpty { am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() }
        } else {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }
        return RouteKeys.pick(
            devices.map { d ->
                val name = d.productName?.toString().orEmpty()
                OutputDevice(d.type, d.address.orEmpty().ifBlank { name }, name)
            },
        )
    }
}
