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

/**
 * Every colour role from one seed, using Material's own colour science. Text and controls take
 * their "on" roles from here, so they always contrast with what they sit on — in light, dark and
 * black, whatever accent is chosen.
 */
fun seedScheme(seed: Int, dark: Boolean, pureBlack: Boolean = false): ColorScheme {
    // "Content" keeps the seed's own vividness for the primary colour, so a brand colour stays itself.
    val s = SchemeContent(Hct.fromInt(seed), dark, 0.0)
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
