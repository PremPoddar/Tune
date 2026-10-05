package com.prempoddar.tune.service

import android.accessibilityservice.AccessibilityService
import android.media.AudioManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.ui.panel.PanelWindow

/**
 * Keeps Tune running: tells the engine which app is on screen and turns the volume keys into
 * per-app changes. Tune reads no screen content; only window changes and volume keys.
 */
class TuneAccessibilityService : AccessibilityService() {

    private val engine get() = AppGraph.engine
    private var panel: PanelWindow? = null

    override fun onServiceConnected() {
        engine.start()
        panel = PanelWindow(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            engine.onWindowChanged(event.packageName?.toString())
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val up = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> true
            KeyEvent.KEYCODE_VOLUME_DOWN -> false
            else -> return false
        }
        val settings = engine.settings
        if (!settings.enabled || !settings.panelEnabled || inCallOrRinging()) return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            engine.step(up)
            panel?.show()
        }
        return true
    }

    private fun inCallOrRinging(): Boolean {
        val mode = getSystemService(AudioManager::class.java).mode
        return mode == AudioManager.MODE_IN_CALL ||
            mode == AudioManager.MODE_IN_COMMUNICATION ||
            mode == AudioManager.MODE_RINGTONE
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        panel?.destroy()
        panel = null
        engine.stop()
        super.onDestroy()
    }
}
