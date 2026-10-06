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

/** How the app looks; every app saves one of these with its settings. */
data class Appearance(
    val mode: Mode = Mode.SYSTEM,
    /** Colours from the wallpaper (Android 12+). */
    val dynamic: Boolean = false,
    /** RGB of a chosen accent; null means the app's own colour. */
    val accent: Int? = null,
    /** Black backgrounds in dark mode. */
    val pureBlack: Boolean = false,
) {
    enum class Mode { SYSTEM, LIGHT, DARK }
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
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * The app's theme: colours from the wallpaper, a chosen accent or the app's [brand] colour, in
 * light, dark or black; system bar icons follow so they're always visible.
 */
@Composable
fun AppTheme(appearance: Appearance, brand: Int, typography: Typography, content: @Composable () -> Unit) {
    val dark = isDark(appearance)
    val context = LocalContext.current
    val scheme = remember(appearance, dark, brand) {
        if (appearance.dynamic && dynamicColourSupported) {
            val s = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (dark && appearance.pureBlack) blacken(s) else s
        } else seedScheme(appearance.accent ?: brand, dark, appearance.pureBlack)
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
    CompositionLocalProvider(LocalAppearance provides appearance) {
        MaterialTheme(colorScheme = scheme, typography = typography, shapes = ExpressiveShapes) {
            // Text and icons drawn outside a Surface would otherwise default to black, which
            // vanishes in dark mode. Give everything the theme's text colour by default.
            CompositionLocalProvider(LocalContentColor provides scheme.onSurface, content = content)
        }
    }
}
