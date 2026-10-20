package com.prempoddar.tune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prempoddar.tune.data.TuneSettings
import com.prempoddar.tune.ui.TuneNavHost
import com.prempoddar.tune.ui.theme.TuneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.prempoddar.tune.ui.design.Haptics.attach(window.decorView)
        enableEdgeToEdge()
        setContent {
            // Null until settings load, so onboarding never flashes for returning users.
            val settings: TuneSettings? by AppGraph.settings.settings.collectAsStateWithLifecycle(null)
            val loaded = settings ?: return@setContent
            TuneTheme(loaded.appearance) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    TuneNavHost(onboarded = loaded.onboarded)
                }
            }
        }
    }
}
