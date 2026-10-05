package com.prempoddar.tune.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Tune's palette: a cool mist base so every app's own pastel reads clearly on top of it.
private val Mist = Color(0xFFF2F3F7)
private val Paper = Color(0xFFFFFFFF)
private val Ink = Color(0xFF17161C)
private val InkMuted = Color(0xFF6E6B7B)
private val Lilac = Color(0xFF7C6CF2)
private val LilacSoft = Color(0xFFE6E2FF)
private val Track = Color(0xFFE7E7EF)

private val Night = Color(0xFF121118)
private val NightCard = Color(0xFF1C1A24)
private val NightTrack = Color(0xFF282534)
private val Moon = Color(0xFFEEEDF3)
private val MoonMuted = Color(0xFF9D99AC)
private val LilacNight = Color(0xFFB8AEFF)
private val LilacDeep = Color(0xFF332C5C)

val LightColors = lightColorScheme(
    primary = Lilac,
    onPrimary = Color.White,
    primaryContainer = LilacSoft,
    onPrimaryContainer = Color(0xFF2A1F7A),
    secondary = InkMuted,
    background = Mist,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Track,
    onSurfaceVariant = InkMuted,
    surfaceContainer = Paper,
    surfaceContainerHigh = Paper,
    outline = Color(0xFFD5D4DE),
    outlineVariant = Color(0xFFE4E3EB),
    error = Color(0xFFC4314B),
    errorContainer = Color(0xFFFFE3E7),
    onErrorContainer = Color(0xFF5E0B1C),
)

val DarkColors = darkColorScheme(
    primary = LilacNight,
    onPrimary = Color(0xFF1E1650),
    primaryContainer = LilacDeep,
    onPrimaryContainer = Color(0xFFE6E2FF),
    secondary = MoonMuted,
    background = Night,
    onBackground = Moon,
    surface = NightCard,
    onSurface = Moon,
    surfaceVariant = NightTrack,
    onSurfaceVariant = MoonMuted,
    surfaceContainer = NightCard,
    surfaceContainerHigh = Color(0xFF242130),
    outline = Color(0xFF3A3648),
    outlineVariant = Color(0xFF2C2938),
    error = Color(0xFFFF8FA3),
    errorContainer = Color(0xFF4A1422),
    onErrorContainer = Color(0xFFFFD9DF),
)
