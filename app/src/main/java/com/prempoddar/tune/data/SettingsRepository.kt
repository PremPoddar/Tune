package com.prempoddar.tune.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.prempoddar.tune.ui.design.Appearance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TuneSettings(
    val appearance: Appearance = Appearance(),
    /** False pauses Tune: nothing is applied and every effect is released. */
    val enabled: Boolean = true,
    val panelEnabled: Boolean = true,
    val stepPct: Int = 5,
    val onboarded: Boolean = false,
    /** Use Shizuku for exact per-app volume whenever it's running and allowed. */
    val useShizuku: Boolean = true,
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val dynamic = booleanPreferencesKey("dynamic")
        val accent = intPreferencesKey("accent")
        val black = booleanPreferencesKey("pure_black")
        val look = stringPreferencesKey("look")
        val enabled = booleanPreferencesKey("enabled")
        val panel = booleanPreferencesKey("panel")
        val step = intPreferencesKey("step")
        val onboarded = booleanPreferencesKey("onboarded")
        val shizuku = booleanPreferencesKey("shizuku")
    }

    val settings: Flow<TuneSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (TuneSettings) -> TuneSettings) {
        context.dataStore.edit { p ->
            val next = transform(p.toSettings())
            p[Keys.theme] = next.appearance.mode.name
            p[Keys.dynamic] = next.appearance.dynamic
            p[Keys.black] = next.appearance.pureBlack
            p[Keys.look] = next.appearance.extras()
            next.appearance.accent?.let { p[Keys.accent] = it } ?: p.remove(Keys.accent)
            p[Keys.enabled] = next.enabled
            p[Keys.panel] = next.panelEnabled
            p[Keys.step] = next.stepPct
            p[Keys.onboarded] = next.onboarded
            p[Keys.shizuku] = next.useShizuku
        }
    }

    private fun Preferences.toSettings(): TuneSettings {
        val d = TuneSettings()
        return TuneSettings(
            appearance = Appearance(
                // Earlier versions stored SYSTEM, LIGHT or DARK under the same key.
                mode = this[Keys.theme]?.let { runCatching { Appearance.Mode.valueOf(it) }.getOrNull() } ?: Appearance.Mode.SYSTEM,
                dynamic = this[Keys.dynamic] ?: false,
                accent = this[Keys.accent],
                pureBlack = this[Keys.black] ?: false,
            ).withExtras(this[Keys.look]),
            enabled = this[Keys.enabled] ?: d.enabled,
            panelEnabled = this[Keys.panel] ?: d.panelEnabled,
            stepPct = this[Keys.step] ?: d.stepPct,
            onboarded = this[Keys.onboarded] ?: d.onboarded,
            useShizuku = this[Keys.shizuku] ?: d.useShizuku,
        )
    }
}
