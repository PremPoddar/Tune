package com.prempoddar.tune.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
open class ScreensTest {
    /** Where the pictures go; the tablet runs below draw the same screens into their own folders. */
    protected open val out = "build/screens"

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        // A few made-up apps to mix, each with a name and a plain coloured icon.
        val pm = org.robolectric.Shadows.shadowOf(app.packageManager)
        for ((pkg, label, colour) in APPS) {
            pm.installPackage(android.content.pm.PackageInfo().apply {
                packageName = pkg
                applicationInfo = android.content.pm.ApplicationInfo().apply { packageName = pkg; nonLocalizedLabel = label }
            })
            pm.setApplicationIcon(pkg, appIcon(app, colour))
        }
        AppGraph.init(app)
    }

    private fun appIcon(app: Application, colour: Int): android.graphics.drawable.Drawable {
        val b = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(b); val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        p.color = colour; c.drawRoundRect(0f, 0f, 192f, 192f, 52f, 52f, p)
        p.color = android.graphics.Color.WHITE; c.drawCircle(96f, 96f, 38f, p)
        p.color = colour; c.drawCircle(96f, 96f, 14f, p)
        return android.graphics.drawable.BitmapDrawable(app.resources, b)
    }

    private fun shoot(name: String, appearance: Appearance, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { TuneTheme(appearance) { content() } }
        repeat(8) { Thread.sleep(100); rule.mainClock.advanceTimeBy(250) }
        val view = rule.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(android.graphics.Canvas(bitmap)) }
        File(out).apply { mkdirs() }.resolve("$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val light = Appearance(mode = Appearance.Mode.LIGHT)
    private val dark = Appearance(mode = Appearance.Mode.DARK)

    @Composable
    private fun Mixer() {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
                .wrapContentWidth().widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MixerCard(APPS[0].first, 62, "Playing · volume buttons", true, ControlMode.MIX, false, {}, {})
            MixerCard(APPS[1].first, 85, "Playing", true, ControlMode.SWITCH, true, {}, {})
            MixerCard(APPS[2].first, 40, "Saved for Speaker", false, null, false, {}, {})
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

    private companion object {
        val APPS = listOf(
            Triple("com.example.waves", "Waves Music", 0xFF1DB98A.toInt()),
            Triple("com.example.reel", "Reel Video", 0xFFE5484D.toInt()),
            Triple("com.example.pods", "Podcasts", 0xFF8E4EC6.toInt()),
        )
    }
}
