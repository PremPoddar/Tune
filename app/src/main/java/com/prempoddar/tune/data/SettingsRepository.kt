package com.prempoddar.tune.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class TuneSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** False pauses Tune: nothing is applied and every effect is released. */
    val enabled: Boolean = true,
    val panelEnabled: Boolean = true,
    val stepPct: Int = 5,
    val onboarded: Boolean = false,
)

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val enabled = booleanPreferencesKey("enabled")
        val panel = booleanPreferencesKey("panel")
        val step = intPreferencesKey("step")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<TuneSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (TuneSettings) -> TuneSettings) {
        context.dataStore.edit { p ->
            val next = transform(p.toSettings())
            p[Keys.theme] = next.theme.name
            p[Keys.enabled] = next.enabled
            p[Keys.panel] = next.panelEnabled
            p[Keys.step] = next.stepPct
            p[Keys.onboarded] = next.onboarded
        }
    }

    private fun Preferences.toSettings(): TuneSettings {
        val d = TuneSettings()
        return TuneSettings(
            theme = this[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.theme,
            enabled = this[Keys.enabled] ?: d.enabled,
            panelEnabled = this[Keys.panel] ?: d.panelEnabled,
            stepPct = this[Keys.step] ?: d.stepPct,
            onboarded = this[Keys.onboarded] ?: d.onboarded,
        )
    }
}
