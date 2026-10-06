package com.prempoddar.tune.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.prempoddar.tune.ui.design.AppTheme
import com.prempoddar.tune.ui.design.Appearance
import com.prempoddar.tune.ui.design.isDark

/** Tune's lilac. */
const val TUNE_BRAND = 0x7C6CF2

/** True when Tune is drawn dark, whatever the reason. */
val LocalDark = staticCompositionLocalOf { false }

@Composable
fun TuneTheme(appearance: Appearance = Appearance(), content: @Composable () -> Unit) {
    val dark = isDark(appearance)
    CompositionLocalProvider(LocalDark provides dark) {
        AppTheme(appearance, TUNE_BRAND, TuneTypography, content)
    }
}
