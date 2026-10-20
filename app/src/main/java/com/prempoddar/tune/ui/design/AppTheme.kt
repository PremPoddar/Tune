package com.prempoddar.tune.ui.design

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import com.prempoddar.tune.R

/** How the app looks; every app saves one of these with its settings. */
data class Appearance(
    val mode: Mode = Mode.SYSTEM,
    /** Colours from the wallpaper (Android 12+). */
    val dynamic: Boolean = false,
    /** RGB of a chosen accent; null means the app's own colour. */
    val accent: Int? = null,
    /** Black backgrounds in dark mode. */
    val pureBlack: Boolean = false,
    /** Softer, lighter dark mode with a hint of the accent ("dusk"); ignored with [pureBlack]. */
    val dusk: Boolean = false,
    val font: Font = Font.APP,
    val palette: Palette = Palette.APP,
    val contrast: Contrast = Contrast.STANDARD,
    val corners: Corners = Corners.ROUND,
    /** Tighter spacing to fit more on screen. */
    val compact: Boolean = false,
    val textSize: TextSize = TextSize.DEFAULT,
    val haptics: Boolean = true,
) {
    enum class Mode { SYSTEM, LIGHT, DARK }
    enum class Font(val label: String) { APP("App's own"), SYSTEM("System"), GROTESK("Grotesk"), ROUNDED("Rounded"), GEOMETRIC("Geometric"), SERIF("Serif") }
    /** How colours are drawn from the accent. */
    enum class Palette(val label: String) { APP("Faithful"), TONAL("Calm"), VIBRANT("Vibrant"), EXPRESSIVE("Playful"), MONO("Monochrome") }
    enum class Contrast(val label: String, val level: Double) { STANDARD("Standard", 0.0), MEDIUM("Medium", 0.5), HIGH("High", 1.0) }
    enum class Corners(val label: String, val scale: Float) { SHARP("Sharp", 0.4f), SOFT("Soft", 0.75f), ROUND("Round", 1f), PILL("Extra round", 1.4f) }
    enum class TextSize(val label: String, val scale: Float) { SMALL("Small", 0.92f), DEFAULT("Default", 1f), LARGE("Large", 1.1f), HUGE("Huge", 1.22f) }

    /** The settings added after the first four, as one line for an app's preferences. */
    fun extras(): String = listOf(
        "dusk=${if (dusk) 1 else 0}", "font=${font.name}", "palette=${palette.name}", "contrast=${contrast.name}",
        "corners=${corners.name}", "compact=${if (compact) 1 else 0}", "text=${textSize.name}", "haptics=${if (haptics) 1 else 0}",
    ).joinToString(";")

    /** These settings with the extras saved by [extras] read back; unknown or missing ones keep their defaults. */
    fun withExtras(line: String?): Appearance {
        if (line.isNullOrBlank()) return this
        val m = line.split(';').mapNotNull { p -> p.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
        fun <E : Enum<E>> pick(key: String, all: Array<E>, default: E) = m[key]?.let { v -> all.firstOrNull { it.name == v } } ?: default
        return copy(
            dusk = m["dusk"]?.let { it == "1" } ?: dusk,
            font = pick("font", Font.entries.toTypedArray(), font),
            palette = pick("palette", Palette.entries.toTypedArray(), palette),
            contrast = pick("contrast", Contrast.entries.toTypedArray(), contrast),
            corners = pick("corners", Corners.entries.toTypedArray(), corners),
            compact = m["compact"]?.let { it == "1" } ?: compact,
            textSize = pick("text", TextSize.entries.toTypedArray(), textSize),
            haptics = m["haptics"]?.let { it == "1" } ?: haptics,
        )
    }
}

/** Accent choices offered in Appearance (RGB). */
val ACCENTS = listOf(
    0xD84315, 0xE8763A, 0xF2A900, 0x7CB342, 0x2E7D32, 0x00897B,
    0x00ACC1, 0x1E88E5, 0x3949AB, 0x6D4AFF, 0x8E24AA, 0xD81B60,
)

val dynamicColourSupported get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val LocalAppearance = staticCompositionLocalOf { Appearance() }

@Composable
fun isDark(a: Appearance) = when (a.mode) {
    Appearance.Mode.SYSTEM -> isSystemInDarkTheme()
    Appearance.Mode.LIGHT -> false
    Appearance.Mode.DARK -> true
}

/** Rounder than default Material: friendly, touchable, Android 16-like. */
val ExpressiveShapes = shapes(1f)

/** The app's corner sizes, scaled for the chosen roundness. */
fun shapes(scale: Float) = Shapes(
    extraSmall = RoundedCornerShape(8.dp * scale),
    small = RoundedCornerShape(12.dp * scale),
    medium = RoundedCornerShape(20.dp * scale),
    large = RoundedCornerShape(28.dp * scale),
    extraLarge = RoundedCornerShape(32.dp * scale),
)

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun look(res: Int) = FontFamily(
    listOf(400, 500, 600, 700).map { w -> androidx.compose.ui.text.font.Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) },
)

/** The family for a chosen font, or null for the app's own. */
fun familyFor(font: Appearance.Font): FontFamily? = when (font) {
    Appearance.Font.APP -> null
    Appearance.Font.SYSTEM -> FontFamily.Default
    Appearance.Font.GROTESK -> look(R.font.look_grotesk)
    Appearance.Font.ROUNDED -> look(R.font.look_rounded)
    Appearance.Font.GEOMETRIC -> look(R.font.look_geometric)
    Appearance.Font.SERIF -> FontFamily.Serif
}

/** Every style in [t] set in [family]; monospaced text stays monospaced. */
fun Typography.inFamily(family: FontFamily): Typography {
    fun TextStyle.f() = if (fontFamily == FontFamily.Monospace) this else copy(fontFamily = family)
    return Typography(
        displayLarge.f(), displayMedium.f(), displaySmall.f(), headlineLarge.f(), headlineMedium.f(), headlineSmall.f(),
        titleLarge.f(), titleMedium.f(), titleSmall.f(), bodyLarge.f(), bodyMedium.f(), bodySmall.f(),
        labelLarge.f(), labelMedium.f(), labelSmall.f(),
    )
}

/**
 * The app's theme: colours from the wallpaper, a chosen accent or the app's [brand] colour, in
 * light, dark or black; system bar icons follow so they're always visible.
 */
@Composable
fun AppTheme(appearance: Appearance, brand: Int, typography: Typography, content: @Composable () -> Unit) {
    val dark = isDark(appearance)
    val context = LocalContext.current
    val scheme = remember(appearance, dark, brand) {
        val s = if (appearance.dynamic && dynamicColourSupported) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else seedScheme(appearance.accent ?: brand, dark, false, appearance.palette, appearance.contrast.level)
        when {
            dark && appearance.pureBlack -> blacken(s)
            dark && appearance.dusk -> dusken(s)
            else -> s
        }
    }
    val type = remember(typography, appearance.font) { familyFor(appearance.font)?.let { typography.inFamily(it) } ?: typography }
    val shapeSet = remember(appearance.corners) { shapes(appearance.corners.scale) }
    SideEffect { Haptics.enabled = appearance.haptics }
    val density = LocalDensity.current
    val looked = remember(density, appearance.compact, appearance.textSize) {
        Density(density.density * (if (appearance.compact) 0.9f else 1f), density.fontScale * appearance.textSize.scale)
    }
    val activity = context as? Activity
    DisposableEffect(dark) {
        (activity as? ComponentActivity)?.let { a ->
            val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            a.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        onDispose { }
    }
    CompositionLocalProvider(LocalAppearance provides appearance, LocalDensity provides looked) {
        MaterialTheme(colorScheme = scheme, typography = type, shapes = shapeSet) {
            // Text and icons drawn outside a Surface would otherwise default to black, which
            // vanishes in dark mode. Give everything the theme's text colour by default.
            CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
        }
    }
}
