@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.prempoddar.tune.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Theme, black backgrounds, wallpaper colours and accent, with a live preview at the top. */
@Composable
fun AppearanceScreen(appearance: Appearance, brand: Int, onChange: (Appearance) -> Unit, onBack: () -> Unit) {
    ExpressiveScaffold("Appearance", onBack = onBack) { pad ->
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp)) {
            item { Preview() }
            item {
                SectionLabel("Theme")
                ChoiceChips(
                    listOf(Appearance.Mode.SYSTEM to "Follow system", Appearance.Mode.LIGHT to "Light", Appearance.Mode.DARK to "Dark"),
                    appearance.mode, { onChange(appearance.copy(mode = it)) },
                )
                Spacer(Modifier.height(12.dp))
                SettingsGroup {
                    SwitchRow(
                        "Pure black", "Black backgrounds in dark mode, easy on OLED screens", Icons.Rounded.DarkMode,
                        checked = appearance.pureBlack, enabled = appearance.mode != Appearance.Mode.LIGHT,
                    ) { onChange(appearance.copy(pureBlack = it)) }
                }
            }
            item {
                SettingsGroup("Colour") {
                    SwitchRow(
                        "Match wallpaper",
                        if (dynamicColourSupported) "Take colours from your wallpaper" else "Needs Android 12 or newer",
                        Icons.Rounded.Wallpaper, checked = appearance.dynamic && dynamicColourSupported, enabled = dynamicColourSupported,
                    ) { onChange(appearance.copy(dynamic = it)) }
                }
            }
            item {
                val usingAccent = !(appearance.dynamic && dynamicColourSupported)
                SectionLabel("Accent")
                FlowRow(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Swatch(Color(0xFF000000.toInt() or brand), usingAccent && appearance.accent == null, "App colour") { onChange(appearance.copy(accent = null, dynamic = false)) }
                    ACCENTS.filter { it != brand }.forEach { c ->
                        Swatch(Color(0xFF000000.toInt() or c), usingAccent && appearance.accent == c, "Accent #%06X".format(c)) { onChange(appearance.copy(accent = c, dynamic = false)) }
                    }
                }
                Text(
                    if (usingAccent) "The first colour is the app's own." else "Wallpaper colours are on; picking an accent turns them off.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/** A small mock of the app in the current colours. */
@Composable
private fun Preview() {
    val s = MaterialTheme.colorScheme
    Surface(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = s.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Contrast)
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Preview", style = MaterialTheme.typography.titleMedium, color = s.onSurface)
                    Text("This is how the app looks now", style = MaterialTheme.typography.bodySmall, color = s.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = s.primary, contentColor = s.onPrimary) {
                    Text("Button", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
                Surface(shape = RoundedCornerShape(20.dp), color = s.secondaryContainer, contentColor = s.onSecondaryContainer) {
                    Text("Tonal", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
                Surface(shape = RoundedCornerShape(20.dp), color = s.tertiaryContainer, contentColor = s.onTertiaryContainer) {
                    Text("Accent", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
            }
        }
    }
}

/** A library or font the app is built with, and its licence. */
data class Credit(val name: String, val licence: String)

/** The launcher icon drawn from its two vector layers (adaptive icons can't be painted directly). */
@Composable
fun LayeredIcon(background: Int, foreground: Int) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(background), null, Modifier.fillMaxSize())
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(foreground), null, Modifier.fillMaxSize())
    }
}

/** Name, version, what the app is, and what it's built with. */
@Composable
fun AboutScreen(appName: String, tagline: String, icon: @Composable () -> Unit, credits: List<Credit>, onBack: () -> Unit, extra: (@Composable () -> Unit)? = null) {
    val context = LocalContext.current
    val version = runCatching {
        @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "—"
    ExpressiveScaffold("About", onBack = onBack) { pad ->
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.foundation.layout.Box(Modifier.size(96.dp).clip(RoundedCornerShape(30.dp))) { icon() }
                    Spacer(Modifier.height(16.dp))
                    Text(appName, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(tagline, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp))
                    Spacer(Modifier.height(10.dp))
                    AssistChip(onClick = {}, label = { Text("Version $version") })
                }
            }
            if (extra != null) item { extra() }
            item {
                SettingsGroup("Built with") {
                    credits.forEach { SettingsRow(it.name, it.licence) }
                }
            }
        }
    }
}
