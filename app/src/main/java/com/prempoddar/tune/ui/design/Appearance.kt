@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.prempoddar.tune.ui.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Appearance, chosen by picture rather than by name: the themes as little phones drawn in their
 * real colours, the accents as swatches of the colours they make, the fonts written in themselves,
 * the corners as shapes. Toggles for the rest sit in rounded groups below.
 */
@Composable
fun AppearanceScreen(appearance: Appearance, brand: Int, onChange: (Appearance) -> Unit, onBack: () -> Unit) {
    val a = appearance
    var fonts by remember { mutableStateOf(false) }
    ExpressiveScaffold("Appearance", onBack = onBack) { pad ->
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp)) {
            item { ThemePicker(a, brand, onChange) }
            item { AccentPicker(a, brand, onChange) }
            if (!(a.dynamic && dynamicColourSupported)) item { PalettePicker(a, brand, onChange) }
            item {
                Group("Colour & type") {
                    ToggleRow(
                        Icons.Rounded.Wallpaper, "Material You",
                        if (dynamicColourSupported) "Colours from your wallpaper" else "Needs Android 12 or newer",
                        a.dynamic && dynamicColourSupported, enabled = dynamicColourSupported,
                    ) { onChange(a.copy(dynamic = it)) }
                    PickRow(Icons.Rounded.TextFields, "Font", a.font.label, onClick = { fonts = true }) {
                        Text("Aa", style = MaterialTheme.typography.headlineSmall.let { s -> familyFor(a.font)?.let { s.copy(fontFamily = it) } ?: s }, fontWeight = FontWeight.SemiBold)
                    }
                    SegmentRow(Icons.Rounded.FormatSize, "Text size", Appearance.TextSize.entries.map { it to it.label }, a.textSize) { onChange(a.copy(textSize = it)) }
                    SegmentRow(Icons.Rounded.Contrast, "Contrast", Appearance.Contrast.entries.map { it to it.label }, a.contrast, enabled = !(a.dynamic && dynamicColourSupported)) { onChange(a.copy(contrast = it)) }
                }
            }
            item { CornerPicker(a, onChange) }
            item {
                Group("Motion & feel") {
                    ToggleRow(Icons.Rounded.TouchApp, "Haptic feedback", "Gentle taps on buttons, switches and sliders", a.haptics) { onChange(a.copy(haptics = it)) }
                    ToggleRow(Icons.Rounded.UnfoldLess, "Compact", "Tighter spacing, more on screen", a.compact) { onChange(a.copy(compact = it)) }
                }
            }
            item {
                Group(null) {
                    PickRow(Icons.Rounded.RestartAlt, "Reset appearance", "The app's own colours, font and shapes", onClick = { Haptics.confirm(); onChange(Appearance()) })
                }
            }
        }
    }
    if (fonts) FontSheet(a, onPick = { onChange(a.copy(font = it)) }, onDismiss = { fonts = false })
}

// ------------------------------------------------------------ building blocks

/** A coloured heading over rows joined into one rounded card with hairline gaps. */
@Composable
private fun Group(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (title != null) Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 10.dp))
        Column(Modifier.clip(MaterialTheme.shapes.extraLarge), verticalArrangement = Arrangement.spacedBy(2.dp), content = content)
    }
}

/** The tray a picker's choices sit in. */
@Composable
private fun Tray(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.extraLarge, modifier = modifier.fillMaxWidth()) { content() }
}

@Composable
private fun RowShell(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, summary: String?, enabled: Boolean = true, onClick: (() -> Unit)?, trailing: @Composable () -> Unit) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconTile(icon, Modifier.then(if (enabled) Modifier else Modifier.background(Color.Transparent)), size = 42.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
        trailing()
    }
}

@Composable
private fun ToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, summary: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    RowShell(icon, title, summary, enabled, onClick = { Haptics.toggle(!checked); onChange(!checked) }) {
        IconSwitch(checked, enabled) { Haptics.toggle(it); onChange(it) }
    }
}

@Composable
private fun PickRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, summary: String?, onClick: () -> Unit, trailing: @Composable () -> Unit = {}) {
    RowShell(icon, title, summary, onClick = { Haptics.tap(); onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailing()
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A titled row with its options as a connected segmented bar under it. */
@Composable
private fun <T> SegmentRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, options: List<Pair<T, String>>, selected: T, enabled: Boolean = true, onPick: (T) -> Unit) {
    val s = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().background(s.surfaceContainer).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconTile(icon, size = 42.dp)
            Text(title, style = MaterialTheme.typography.titleMedium, color = s.onSurface.copy(alpha = if (enabled) 1f else 0.45f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            options.forEachIndexed { i, (value, label) ->
                val on = value == selected
                val outer = 20.dp; val inner = 6.dp
                val corner by animateDpAsState(if (on) outer else inner, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "seg")
                val shape = when {
                    on -> RoundedCornerShape(corner)
                    i == 0 -> RoundedCornerShape(topStart = outer, bottomStart = outer, topEnd = inner, bottomEnd = inner)
                    i == options.lastIndex -> RoundedCornerShape(topStart = inner, bottomStart = inner, topEnd = outer, bottomEnd = outer)
                    else -> RoundedCornerShape(inner)
                }
                Surface(
                    onClick = { if (!on) Haptics.select(); onPick(value) }, enabled = enabled, shape = shape,
                    color = if (on) s.primary else s.surfaceContainerHighest, contentColor = if (on) s.onPrimary else s.onSurfaceVariant,
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                }
            }
        }
    }
}

/** A choice in a picker: springs rounder and gains a ring and a tick when it's the one chosen. */
@Composable
private fun Choice(on: Boolean, label: String, modifier: Modifier = Modifier, onClick: () -> Unit, picture: @Composable (Shape) -> Unit) {
    val r by animateDpAsState(if (on) 26.dp else 14.dp, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow), label = "r")
    val ring by animateDpAsState(if (on) 3.dp else 0.dp, label = "ring")
    val shape = RoundedCornerShape(r)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().clip(shape).border(ring, MaterialTheme.colorScheme.primary, shape).clickable { if (!on) Haptics.select(); onClick() }) {
            picture(shape)
            if (on) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp), maxLines = 1)
    }
}

/** The colours a choice would give, worked out the same way the app's theme is. */
@Composable
private fun schemeFor(a: Appearance, brand: Int, dark: Boolean): ColorScheme {
    val context = LocalContext.current
    return remember(a, brand, dark) {
        val base = if (a.dynamic && dynamicColourSupported) (if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context))
        else seedScheme(a.accent ?: brand, dark, false, a.palette, a.contrast.level)
        when { dark && a.pureBlack -> blacken(base); dark && a.dusk -> dusken(base); else -> base }
    }
}

// ------------------------------------------------------------ pickers

private enum class Look(val label: String) { AUTO("Auto"), LIGHT("Light"), DUSK("Dusk"), DARK("Dark"), OLED("OLED") }

@Composable
private fun ThemePicker(a: Appearance, brand: Int, onChange: (Appearance) -> Unit) {
    val current = when {
        a.mode == Appearance.Mode.SYSTEM -> Look.AUTO
        a.mode == Appearance.Mode.LIGHT -> Look.LIGHT
        a.pureBlack -> Look.OLED
        a.dusk -> Look.DUSK
        else -> Look.DARK
    }
    val light = schemeFor(a.copy(dusk = false, pureBlack = false), brand, false)
    val dusk = schemeFor(a.copy(dusk = true, pureBlack = false), brand, true)
    val dark = schemeFor(a.copy(dusk = false, pureBlack = false), brand, true)
    val oled = schemeFor(a.copy(dusk = false, pureBlack = true), brand, true)
    // Auto uses whichever dark style was picked last, so show that one at night.
    val night = when { a.pureBlack -> oled; a.dusk -> dusk; else -> dark }
    Group("Theme") {
        Tray {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Look.entries.forEach { look ->
                    Choice(look == current, look.label, Modifier.weight(1f), onClick = {
                        onChange(when (look) {
                            Look.AUTO -> a.copy(mode = Appearance.Mode.SYSTEM)
                            Look.LIGHT -> a.copy(mode = Appearance.Mode.LIGHT)
                            Look.DUSK -> a.copy(mode = Appearance.Mode.DARK, dusk = true, pureBlack = false)
                            Look.DARK -> a.copy(mode = Appearance.Mode.DARK, dusk = false, pureBlack = false)
                            Look.OLED -> a.copy(mode = Appearance.Mode.DARK, dusk = false, pureBlack = true)
                        })
                    }) {
                        when (look) {
                            Look.AUTO -> Row(Modifier.fillMaxWidth().aspectRatio(0.6f)) {
                                Box(Modifier.weight(1f).fillMaxHeight()) { MiniPhone(light, clipRight = true) }
                                Box(Modifier.weight(1f).fillMaxHeight()) { MiniPhone(night, clipLeft = true) }
                            }
                            Look.LIGHT -> MiniPhone(light)
                            Look.DUSK -> MiniPhone(dusk)
                            Look.DARK -> MiniPhone(dark)
                            Look.OLED -> MiniPhone(oled)
                        }
                    }
                }
            }
        }
    }
}

/** A tiny screen in [s]'s colours: a search bar, two tiles, lines of text and a button. */
@Composable
private fun MiniPhone(s: ColorScheme, clipLeft: Boolean = false, clipRight: Boolean = false) {
    Box(Modifier.fillMaxWidth().then(if (clipLeft || clipRight) Modifier.fillMaxHeight() else Modifier.aspectRatio(0.6f)).background(s.surface).padding(horizontal = if (clipLeft || clipRight) 4.dp else 7.dp, vertical = 7.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(s.surfaceContainerHigh))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(5.dp)).background(s.primaryContainer))
                if (!(clipLeft || clipRight)) Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(5.dp)).background(s.tertiaryContainer))
            }
            Box(Modifier.fillMaxWidth(0.75f).height(5.dp).clip(CircleShape).background(s.onSurface.copy(alpha = 0.5f)))
            Box(Modifier.fillMaxWidth(0.5f).height(5.dp).clip(CircleShape).background(s.onSurface.copy(alpha = 0.3f)))
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(12.dp).clip(CircleShape).background(s.primary))
    }
}

/** Accents as round swatches showing the main, soft and third colours each would give. */
@Composable
private fun AccentPicker(a: Appearance, brand: Int, onChange: (Appearance) -> Unit) {
    val dark = isDark(a)
    val usingAccent = !(a.dynamic && dynamicColourSupported)
    Group("Accent") {
        Tray {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                (listOf<Int?>(null) + ACCENTS.filter { it != brand }).forEach { c ->
                    val on = usingAccent && a.accent == c
                    val s = schemeFor(a.copy(accent = c, dynamic = false), brand, dark)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val shape: Shape = if (on) CookieShape else CircleShape
                        Box(Modifier.size(58.dp).clip(shape).clickable { if (!on) Haptics.select(); onChange(a.copy(accent = c, dynamic = false)) }, contentAlignment = Alignment.Center) {
                            Swatch3(s)
                            if (on) Box(Modifier.size(22.dp).clip(CircleShape).background(s.surface), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, Modifier.size(15.dp), tint = s.primary) }
                        }
                        Text(if (c == null) "App" else "", style = MaterialTheme.typography.labelMedium, color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        if (!usingAccent) Text("Material You is on; picking an accent turns it off.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
    }
}

/** A circle split into a scheme's primary (top), soft secondary and tertiary (bottom halves). */
@Composable
private fun Swatch3(s: ColorScheme) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f).background(s.primary))
        Row(Modifier.fillMaxWidth().weight(1f)) {
            Box(Modifier.weight(1f).fillMaxHeight().background(s.secondaryContainer))
            Box(Modifier.weight(1f).fillMaxHeight().background(s.tertiary))
        }
    }
}

/** How colours are drawn from the accent, each shown as the swatch it makes. */
@Composable
private fun PalettePicker(a: Appearance, brand: Int, onChange: (Appearance) -> Unit) {
    val dark = isDark(a)
    Group("Colour style") {
        Tray {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Appearance.Palette.entries.forEach { p ->
                    val on = a.palette == p
                    val s = schemeFor(a.copy(palette = p), brand, dark)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                        Box(Modifier.size(58.dp).clip(if (on) CookieShape else CircleShape).clickable { if (!on) Haptics.select(); onChange(a.copy(palette = p)) }, contentAlignment = Alignment.Center) {
                            Swatch3(s)
                            if (on) Box(Modifier.size(22.dp).clip(CircleShape).background(s.surface), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, Modifier.size(15.dp), tint = s.primary) }
                        }
                        Text(p.label, style = MaterialTheme.typography.labelMedium, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Corner styles drawn as the cards they'd make. */
@Composable
private fun CornerPicker(a: Appearance, onChange: (Appearance) -> Unit) {
    val s = MaterialTheme.colorScheme
    Group("Shapes") {
        Tray {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Appearance.Corners.entries.forEach { c ->
                    Choice(a.corners == c, c.label, Modifier.weight(1f), onClick = { onChange(a.copy(corners = c)) }) {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(s.surfaceContainerHigh).padding(10.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp * c.scale)).background(s.primaryContainer))
                                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp * c.scale)).background(s.primary))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Every font, each written in itself, with a tick by the one in use. */
@Composable
private fun FontSheet(a: Appearance, onPick: (Appearance.Font) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Font", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
            Appearance.Font.entries.forEach { f ->
                val on = a.font == f
                val r by animateDpAsState(if (on) 28.dp else 16.dp, spring(Spring.DampingRatioMediumBouncy), label = "font")
                val family = familyFor(f)
                Surface(
                    onClick = { if (!on) Haptics.select(); onPick(f) }, shape = RoundedCornerShape(r),
                    color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.label, style = MaterialTheme.typography.titleLarge.let { s -> family?.let { s.copy(fontFamily = it) } ?: s }, fontWeight = FontWeight.Bold)
                            Text("The quick brown fox jumps over the lazy dog", style = MaterialTheme.typography.bodyMedium.let { s -> family?.let { s.copy(fontFamily = it) } ?: s }, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                        if (on) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
