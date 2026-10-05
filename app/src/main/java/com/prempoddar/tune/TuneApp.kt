package com.prempoddar.tune

import android.app.Application
import com.prempoddar.tune.widget.TuneWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TuneApp : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        // Keep the widget in step with the app the volume buttons control.
        scope.launch {
            AppGraph.engine.state
                .map { s -> listOf(s.running, s.enabled, s.target, s.levelsFor(s.target).media) }
                .distinctUntilChanged()
                .debounce(500)
                .collect { TuneWidget.refresh(this@TuneApp) }
        }
    }
}
