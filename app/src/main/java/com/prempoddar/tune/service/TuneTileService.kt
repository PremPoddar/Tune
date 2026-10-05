package com.prempoddar.tune.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.prempoddar.tune.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick Settings tile that pauses and resumes Tune. */
class TuneTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listening: Job? = null

    override fun onStartListening() {
        listening = scope.launch {
            AppGraph.settings.settings.collect { s ->
                qsTile?.apply {
                    state = if (s.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    subtitle = if (s.enabled) "On" else "Paused"
                    updateTile()
                }
            }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
    }

    override fun onClick() {
        scope.launch { AppGraph.settings.update { it.copy(enabled = !it.enabled) } }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
