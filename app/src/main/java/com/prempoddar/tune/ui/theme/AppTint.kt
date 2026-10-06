package com.prempoddar.tune.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.prempoddar.tune.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The soft fill an app's level is drawn with: its icon colour, washed toward the card colour so
 * every app gets a different pastel that still sits quietly in Tune's palette.
 */
fun pastelOf(seed: Color, card: Color, dark: Boolean): Color =
    lerp(seed, card, if (dark) 0.68f else 0.58f)

@Composable
fun rememberAppTint(pkg: String): Color {
    val dark = LocalDark.current
    val card = MaterialTheme.colorScheme.surface
    val fallback = MaterialTheme.colorScheme.primary
    val seed by produceState<Color?>(initialValue = null, pkg) {
        value = withContext(Dispatchers.Default) { AppGraph.appInfo.seedColor(pkg)?.let(::Color) }
    }
    return pastelOf(seed ?: fallback, card, dark)
}

/** The app's own colour at full strength, for slider tracks and accents rather than backgrounds. */
@Composable
fun rememberAppAccent(pkg: String): Color {
    val fallback = MaterialTheme.colorScheme.primary
    val seed by produceState<Color?>(initialValue = null, pkg) {
        value = withContext(Dispatchers.Default) { AppGraph.appInfo.seedColor(pkg)?.let(::Color) }
    }
    val dark = LocalDark.current
    // A black icon on a dark screen, or a white one on a light screen, would vanish.
    return seed?.takeIf { val l = it.luminance(); if (dark) l > 0.12f else l < 0.75f } ?: fallback
}
