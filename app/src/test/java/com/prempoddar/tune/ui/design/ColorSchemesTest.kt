package com.prempoddar.tune.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class ColorSchemesTest {
    private fun contrast(a: Color, b: Color): Double {
        val la = a.luminance() + 0.05; val lb = b.luminance() + 0.05
        return maxOf(la, lb).toDouble() / minOf(la, lb)
    }

    // Brand colours of every app, the accent palette, and awkward extremes.
    private val seeds = ACCENTS + listOf(0xC4553A, 0x00897B, 0x1F3B63, 0xE8763A, 0x39FF6A, 0xFFFF00, 0x000000, 0xFFFFFF, 0x808080)

    @Test fun textAlwaysContrastsWithWhatItSitsOn() {
        for (seed in seeds) for (dark in listOf(false, true)) for (black in listOf(false, true)) {
            if (black && !dark) continue
            val s = seedScheme(seed, dark, black)
            val where = "seed #%06X dark=$dark black=$black".format(seed)
            // Body text: WCAG AA (4.5:1).
            listOf(
                "onSurface/surface" to contrast(s.onSurface, s.surface),
                "onSurface/surfaceContainer" to contrast(s.onSurface, s.surfaceContainer),
                "onSurface/surfaceContainerHighest" to contrast(s.onSurface, s.surfaceContainerHighest),
                "onBackground/background" to contrast(s.onBackground, s.background),
                "onPrimary/primary" to contrast(s.onPrimary, s.primary),
                "onPrimaryContainer/primaryContainer" to contrast(s.onPrimaryContainer, s.primaryContainer),
                "onSecondaryContainer/secondaryContainer" to contrast(s.onSecondaryContainer, s.secondaryContainer),
                "onSurfaceVariant/surface" to contrast(s.onSurfaceVariant, s.surface),
                "onSurfaceVariant/surfaceContainerHigh" to contrast(s.onSurfaceVariant, s.surfaceContainerHigh),
            ).forEach { (pair, ratio) -> assertWithMessage("$pair at $where").that(ratio).isAtLeast(4.5) }
            // Controls and outlines: 3:1 against the page.
            assertWithMessage("primary/surface at $where").that(contrast(s.primary, s.surface)).isAtLeast(3.0)
        }
    }
}
