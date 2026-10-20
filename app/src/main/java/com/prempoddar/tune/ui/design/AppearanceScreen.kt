@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.prempoddar.tune.ui.design

import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.RestartAlt
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

/**
 * Look & feel: theme, colours, font, shapes, spacing and touch feedback, with a live preview at
 * the top. The settings people change most are big toggles; the rest are pick-one chips.
 */
@Composable
fun AppearanceScreen(appearance: Appearance, brand: Int, onChange: (Appearance) -> Unit, onBack: () -> Unit) {
    val a = appearance
    ExpressiveScaffold("Look & feel", onBack = onBack) { pad ->
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp)) {
            item { Preview() }
            item {
                SectionLabel("Quick toggles")
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickToggle("Haptics", Icons.Rounded.Vibration, a.haptics, Modifier.weight(1f)) { onChange(a.copy(haptics = it)) }
                        QuickToggle("Material You", Icons.Rounded.Wallpaper, a.dynamic && dynamicColourSupported, Modifier.weight(1f), enabled = dynamicColourSupported) { onChange(a.copy(dynamic = it)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QuickToggle("Pure black", Icons.Rounded.DarkMode, a.pureBlack, Modifier.weight(1f), enabled = a.mode != Appearance.Mode.LIGHT) { onChange(a.copy(pureBlack = it, dusk = if (it) false else a.dusk)) }
                        QuickToggle("Compact", Icons.Rounded.UnfoldLess, a.compact, Modifier.weight(1f)) { onChange(a.copy(compact = it)) }
                    }
                }
            }
            item {
                SectionLabel("Theme")
                ChoiceChips(
                    listOf(Appearance.Mode.SYSTEM to "Follow system", Appearance.Mode.LIGHT to "Light", Appearance.Mode.DARK to "Dark"),
                    a.mode, { onChange(a.copy(mode = it)) },
                )
                if (a.mode != Appearance.Mode.LIGHT) {
                    SectionLabel("Dark style")
                    val style = when { a.pureBlack -> "black"; a.dusk -> "dusk"; else -> "standard" }
                    ChoiceChips(listOf("standard" to "Standard", "dusk" to "Dusk", "black" to "OLED black"), style, { v ->
                        onChange(a.copy(dusk = v == "dusk", pureBlack = v == "black"))
                    })
                }
            }
            item {
                val usingAccent = !(a.dynamic && dynamicColourSupported)
                SectionLabel("Accent")
                FlowRow(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Swatch(Color(0xFF000000.toInt() or brand), usingAccent && a.accent == null, "App colour") { onChange(a.copy(accent = null, dynamic = false)) }
                    ACCENTS.filter { it != brand }.forEach { c ->
                        Swatch(Color(0xFF000000.toInt() or c), usingAccent && a.accent == c, "Accent #%06X".format(c)) { onChange(a.copy(accent = c, dynamic = false)) }
                    }
                }
                Text(
                    if (usingAccent) "The first colour is the app's own." else "Material You is on; picking an accent turns it off.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
                if (usingAccent) {
                    SectionLabel("Colour style")
                    ChoiceChips(Appearance.Palette.entries.map { it to it.label }, a.palette, { onChange(a.copy(palette = it)) })
                    SectionLabel("Contrast")
                    ChoiceChips(Appearance.Contrast.entries.map { it to it.label }, a.contrast, { onChange(a.copy(contrast = it)) })
                }
            }
            item {
                SectionLabel("Font")
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Appearance.Font.entries.forEach { f ->
                        val on = f == a.font
                        Surface(
                            onClick = { if (!on) Haptics.select(); onChange(a.copy(font = f)) }, shape = RoundedCornerShape(14.dp),
                            color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            // Each name in its own font, so the choice can be seen before it's made.
                            val style = familyFor(f)?.let { MaterialTheme.typography.labelLarge.copy(fontFamily = it) } ?: MaterialTheme.typography.labelLarge
                            Text(f.label, style = style, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                        }
                    }
                }
                SectionLabel("Text size")
                ChoiceChips(Appearance.TextSize.entries.map { it to it.label }, a.textSize, { onChange(a.copy(textSize = it)) })
                SectionLabel("Corners")
                ChoiceChips(Appearance.Corners.entries.map { it to it.label }, a.corners, { onChange(a.copy(corners = it)) })
            }
            item {
                Spacer(Modifier.height(16.dp))
                SettingsGroup {
                    SettingsRow("Reset look & feel", "Back to the app's own colours, font and shapes", Icons.Rounded.RestartAlt, onClick = { Haptics.confirm(); onChange(Appearance()) })
                }
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
                Surface(shape = MaterialTheme.shapes.large, color = s.primary, contentColor = s.onPrimary) {
                    Text("Button", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
                Surface(shape = MaterialTheme.shapes.large, color = s.secondaryContainer, contentColor = s.onSecondaryContainer) {
                    Text("Tonal", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
                Surface(shape = MaterialTheme.shapes.large, color = s.tertiaryContainer, contentColor = s.onTertiaryContainer) {
                    Text("Accent", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
                }
            }
        }
    }
}

/** A library or font the app is built with, and its licence. */
data class Credit(val name: String, val licence: String)

/** Who makes the apps, and where to find them. The links are placeholders until those pages exist. */
object Developer {
    const val NAME = "Prem Poddar"
    const val GITHUB = "https://github.com/"
    const val SITE = "https://example.com/"
}

/** The launcher icon drawn from its two layers (adaptive icons and shape layers can't be painted directly). */
@Composable
fun LayeredIcon(background: Int, foreground: Int) {
    val context = LocalContext.current
    val layers = androidx.compose.runtime.remember(background, foreground) {
        listOfNotNull(androidx.core.content.ContextCompat.getDrawable(context, background), androidx.core.content.ContextCompat.getDrawable(context, foreground))
    }
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        drawIntoCanvas { c ->
            layers.forEach { d ->
                d.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                d.draw(c.nativeCanvas)
            }
        }
    }
}

/** Name, version, what the app is, who built it, optional notes (like privacy), and what it's built with. */
@Composable
fun AboutScreen(
    appName: String, tagline: String, icon: @Composable () -> Unit, credits: List<Credit>, onBack: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
    /** Short titled notes shown in their own group, such as how the app treats your data. */
    notes: List<Pair<String, String>> = emptyList(),
    notesTitle: String = "Privacy",
) {
    val context = LocalContext.current
    val version = runCatching {
        @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "—"
    fun open(url: String) = runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
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
            item {
                TonalCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Built with love by", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(Developer.NAME, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "Made to be fast, private and a pleasure to use. Thank you for using it.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
            item {
                SettingsGroup("Links") {
                    SettingsRow("Source code", "GitHub", androidx.compose.material.icons.Icons.Rounded.Code, onClick = { open(Developer.GITHUB) })
                    SettingsRow("Developer's site", Developer.SITE.removePrefix("https://").trimEnd('/'), androidx.compose.material.icons.Icons.Rounded.Language, onClick = { open(Developer.SITE) })
                }
            }
            if (notes.isNotEmpty()) item {
                SettingsGroup(notesTitle) {
                    notes.forEach { (title, body) -> SettingsRow(title, body, androidx.compose.material.icons.Icons.Rounded.Shield) }
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
