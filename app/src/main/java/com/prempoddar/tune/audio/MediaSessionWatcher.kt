package com.prempoddar.tune.audio

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.prempoddar.tune.service.TuneNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tracks which apps have a media session that is playing, and since when. */
class MediaSessionWatcher(private val context: Context) {

    private val msm = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, TuneNotificationListener::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val controllers = mutableMapOf<MediaSession.Token, Pair<MediaController, MediaController.Callback>>()
    private val startedAt = mutableMapOf<String, Long>()

    private val _playing = MutableStateFlow<Map<String, Long>>(emptyMap())
    /** Package → time it started playing (elapsed realtime). */
    val playing: StateFlow<Map<String, Long>> = _playing.asStateFlow()

    private val listener = MediaSessionManager.OnActiveSessionsChangedListener { sync(it.orEmpty()) }

    fun start() {
        try {
            msm.addOnActiveSessionsChangedListener(listener, component, handler)
            sync(msm.getActiveSessions(component))
        } catch (e: SecurityException) {
            // Notification access not granted yet; onboarding asks for it.
        }
    }

    fun stop() {
        runCatching { msm.removeOnActiveSessionsChangedListener(listener) }
        controllers.values.forEach { (c, cb) -> c.unregisterCallback(cb) }
        controllers.clear()
        startedAt.clear()
        _playing.value = emptyMap()
    }

    private fun sync(active: List<MediaController>) {
        val tokens = active.map { it.sessionToken }.toSet()
        controllers.keys.filter { it !in tokens }.forEach { token ->
            controllers.remove(token)?.let { (c, cb) -> c.unregisterCallback(cb) }
        }
        active.filter { it.sessionToken !in controllers }.forEach { c ->
            val cb = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = recompute()
                override fun onSessionDestroyed() = recompute()
            }
            c.registerCallback(cb, handler)
            controllers[c.sessionToken] = c to cb
        }
        recompute()
    }

    private fun recompute() {
        val now = SystemClock.elapsedRealtime()
        val playingPkgs = controllers.values
            .map { it.first }
            .filter { it.playbackState?.state in PLAYING_STATES }
            .map { it.packageName }
            .toSet()
        startedAt.keys.retainAll(playingPkgs)
        playingPkgs.forEach { startedAt.getOrPut(it) { now } }
        _playing.value = startedAt.toMap()
    }

    private companion object {
        val PLAYING_STATES = setOf(PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING)
    }
}
