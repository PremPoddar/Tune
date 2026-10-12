package com.prempoddar.tune.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.audio.ShizukuBackend
import com.prempoddar.tune.core.ControlMode
import com.prempoddar.tune.ui.components.MixerCard
import com.prempoddar.tune.ui.components.PhoneVolumes
import com.prempoddar.tune.ui.components.ShizukuCard
import com.prempoddar.tune.ui.design.Appearance
import com.prempoddar.tune.ui.home.HomeScreen
import com.prempoddar.tune.ui.settings.SettingsScreen
import com.prempoddar.tune.ui.theme.TuneTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Draws real screens to build/screens to check light, dark and landscape without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() = AppGraph.init(ApplicationProvider.getApplicationContext())

    private fun shoot(name: String, appearance: Appearance, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { TuneTheme(appearance) { content() } }
        repeat(8) { Thread.sleep(100); rule.mainClock.advanceTimeBy(250) }
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(android.graphics.Canvas(bitmap)) }
        File("build/screens").apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val light = Appearance(mode = Appearance.Mode.LIGHT)
    private val dark = Appearance(mode = Appearance.Mode.DARK)

    @Composable
    private fun Mixer() {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MixerCard("com.spotify.music", 62, "Playing · volume buttons", true, ControlMode.MIX, false, {}, {})
            MixerCard("com.google.android.youtube", 85, "Playing", true, ControlMode.SWITCH, true, {}, {})
            MixerCard("au.com.shiftyjelly.pocketcasts", 40, "Saved for Speaker", false, null, false, {}, {})
            PhoneVolumes()
            ShizukuCard(ShizukuBackend.Status.NEEDS_PERMISSION)
        }
    }

    @Test fun mixerLight() = shoot("mixer-light", light) { Mixer() }
    @Test fun mixerDark() = shoot("mixer-dark", dark) { Mixer() }
    @Test fun homeDark() = shoot("home-dark", dark) { HomeScreen({}, {}, {}) }
    @Test fun settingsDark() = shoot("settings-dark", dark) { SettingsScreen({}, {}, {}) }
    @Test fun settingsLight() = shoot("settings-light", light) { SettingsScreen({}, {}, {}) }

    @Test fun aboutDark() = shoot("about-dark", dark) {
        com.prempoddar.tune.ui.design.AboutScreen(
            "Tune", "A volume for every app.",
            icon = { com.prempoddar.tune.ui.design.LayeredIcon(com.prempoddar.tune.R.drawable.ic_launcher_background, com.prempoddar.tune.R.drawable.ic_launcher_foreground) },
            credits = listOf(com.prempoddar.tune.ui.design.Credit("Jetpack Compose", "Apache 2.0")), onBack = {},
            notes = listOf("Stays on your phone" to "Tune never sends anything anywhere."),
        )
    }

    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    @Test fun homeLandscape() = shoot("home-land", light) { HomeScreen({}, {}, {}) }
}
