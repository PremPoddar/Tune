package com.prempoddar.tune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prempoddar.tune.data.TuneSettings
import com.prempoddar.tune.ui.TuneNavHost
import com.prempoddar.tune.ui.theme.TuneTheme
import com.prempoddar.tune.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Null until settings load, so onboarding never flashes for returning users.
            val settings: TuneSettings? by AppGraph.settings.settings.collectAsStateWithLifecycle(null)
            val loaded = settings ?: return@setContent
            val dark = isDark(loaded.theme)
            DisposableEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            TuneTheme(loaded.theme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    TuneNavHost(onboarded = loaded.onboarded)
                }
            }
        }
    }
}
