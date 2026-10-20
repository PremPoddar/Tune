package com.prempoddar.tune.ui.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeContent
import com.materialkolor.scheme.SchemeExpressive
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeTonalSpot
import com.materialkolor.scheme.SchemeVibrant

/**
 * Every colour role from one seed, using Material's own colour science. Text and controls take
 * their "on" roles from here, so they always contrast with what they sit on — in light, dark and
 * black, whatever accent is chosen.
 */
fun seedScheme(seed: Int, dark: Boolean, pureBlack: Boolean = false, palette: Appearance.Palette = Appearance.Palette.APP, contrast: Double = 0.0): ColorScheme {
    val hct = Hct.fromInt(seed)
    // "Content" keeps the seed's own vividness for the primary colour, so a brand colour stays itself.
    val s = when (palette) {
        Appearance.Palette.APP -> SchemeContent(hct, dark, contrast)
        Appearance.Palette.TONAL -> SchemeTonalSpot(hct, dark, contrast)
        Appearance.Palette.VIBRANT -> SchemeVibrant(hct, dark, contrast)
        Appearance.Palette.EXPRESSIVE -> SchemeExpressive(hct, dark, contrast)
        Appearance.Palette.MONO -> SchemeMonochrome(hct, dark, contrast)
    }
    val m = MaterialDynamicColors()
    fun c(d: DynamicColor) = Color(d.getArgb(s))
    val scheme = if (dark) darkColorScheme(
        primary = c(m.primary()), onPrimary = c(m.onPrimary()), primaryContainer = c(m.primaryContainer()), onPrimaryContainer = c(m.onPrimaryContainer()),
        inversePrimary = c(m.inversePrimary()),
        secondary = c(m.secondary()), onSecondary = c(m.onSecondary()), secondaryContainer = c(m.secondaryContainer()), onSecondaryContainer = c(m.onSecondaryContainer()),
        tertiary = c(m.tertiary()), onTertiary = c(m.onTertiary()), tertiaryContainer = c(m.tertiaryContainer()), onTertiaryContainer = c(m.onTertiaryContainer()),
        background = c(m.background()), onBackground = c(m.onBackground()),
        surface = c(m.surface()), onSurface = c(m.onSurface()), surfaceVariant = c(m.surfaceVariant()), onSurfaceVariant = c(m.onSurfaceVariant()),
        surfaceTint = c(m.surfaceTint()), inverseSurface = c(m.inverseSurface()), inverseOnSurface = c(m.inverseOnSurface()),
        error = c(m.error()), onError = c(m.onError()), errorContainer = c(m.errorContainer()), onErrorContainer = c(m.onErrorContainer()),
        outline = c(m.outline()), outlineVariant = c(m.outlineVariant()), scrim = c(m.scrim()),
        surfaceBright = c(m.surfaceBright()), surfaceDim = c(m.surfaceDim()),
        surfaceContainer = c(m.surfaceContainer()), surfaceContainerHigh = c(m.surfaceContainerHigh()), surfaceContainerHighest = c(m.surfaceContainerHighest()),
        surfaceContainerLow = c(m.surfaceContainerLow()), surfaceContainerLowest = c(m.surfaceContainerLowest()),
    ) else lightColorScheme(
        primary = c(m.primary()), onPrimary = c(m.onPrimary()), primaryContainer = c(m.primaryContainer()), onPrimaryContainer = c(m.onPrimaryContainer()),
        inversePrimary = c(m.inversePrimary()),
        secondary = c(m.secondary()), onSecondary = c(m.onSecondary()), secondaryContainer = c(m.secondaryContainer()), onSecondaryContainer = c(m.onSecondaryContainer()),
        tertiary = c(m.tertiary()), onTertiary = c(m.onTertiary()), tertiaryContainer = c(m.tertiaryContainer()), onTertiaryContainer = c(m.onTertiaryContainer()),
        background = c(m.background()), onBackground = c(m.onBackground()),
        surface = c(m.surface()), onSurface = c(m.onSurface()), surfaceVariant = c(m.surfaceVariant()), onSurfaceVariant = c(m.onSurfaceVariant()),
        surfaceTint = c(m.surfaceTint()), inverseSurface = c(m.inverseSurface()), inverseOnSurface = c(m.inverseOnSurface()),
        error = c(m.error()), onError = c(m.onError()), errorContainer = c(m.errorContainer()), onErrorContainer = c(m.onErrorContainer()),
        outline = c(m.outline()), outlineVariant = c(m.outlineVariant()), scrim = c(m.scrim()),
        surfaceBright = c(m.surfaceBright()), surfaceDim = c(m.surfaceDim()),
        surfaceContainer = c(m.surfaceContainer()), surfaceContainerHigh = c(m.surfaceContainerHigh()), surfaceContainerHighest = c(m.surfaceContainerHighest()),
        surfaceContainerLow = c(m.surfaceContainerLow()), surfaceContainerLowest = c(m.surfaceContainerLowest()),
    )
    return if (dark && pureBlack) blacken(scheme) else scheme
}

/** True black for OLED screens: the page goes black and the raised surfaces step down with it. */
fun blacken(s: ColorScheme): ColorScheme = s.copy(
    background = Color.Black, surface = Color.Black, surfaceDim = Color.Black, surfaceContainerLowest = Color.Black,
    surfaceContainerLow = lerp(s.surfaceContainerLow, Color.Black, 0.6f),
    surfaceContainer = lerp(s.surfaceContainer, Color.Black, 0.5f),
    surfaceContainerHigh = lerp(s.surfaceContainerHigh, Color.Black, 0.4f),
    surfaceContainerHighest = lerp(s.surfaceContainerHighest, Color.Black, 0.3f),
)

/**
 * Dusk: a dark mode that's softer than the usual near-black, lifted a little and tinted with the
 * accent, for evening reading without the hard contrast of black.
 */
fun dusken(s: ColorScheme): ColorScheme {
    val base = lerp(lerp(s.surfaceContainerHigh, s.surface, 0.35f), s.primary, 0.06f)
    return s.copy(
        background = base, surface = base, surfaceDim = lerp(base, Color.Black, 0.15f),
        surfaceContainerLowest = lerp(base, Color.Black, 0.2f),
        surfaceContainerLow = lerp(base, s.surfaceContainerHighest, 0.25f),
        surfaceContainer = lerp(base, s.surfaceContainerHighest, 0.45f),
        surfaceContainerHigh = lerp(base, s.surfaceContainerHighest, 0.7f),
        surfaceContainerHighest = lerp(s.surfaceContainerHighest, s.onSurface, 0.06f),
        surfaceBright = lerp(s.surfaceBright, s.onSurface, 0.05f),
    )
}
