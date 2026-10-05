package com.prempoddar.tune.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.prempoddar.tune.core.SelfChangeGuard
import com.prempoddar.tune.core.StreamKind
import com.prempoddar.tune.core.VolumeMath
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Reads and sets the four global stream volumes, and reports changes the user makes. */
class StreamController(private val context: Context, private val guard: SelfChangeGuard) {

    private val am = context.getSystemService(AudioManager::class.java)
    private val _userChanges = MutableSharedFlow<Pair<StreamKind, Int>>(extraBufferCapacity = 32)

    /** Volume changes made outside Tune (system slider, other apps), as (stream, percent). */
    val userChanges: SharedFlow<Pair<StreamKind, Int>> = _userChanges.asSharedFlow()

    // Some phones tie notification volume to ring volume, so setting one fires a change for the
    // other. Changes arriving right after Tune applied something are treated as Tune's own.
    @Volatile private var lastApplyAt = 0L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val stream = intent.getIntExtra(EXTRA_STREAM_TYPE, -1)
            val index = intent.getIntExtra(EXTRA_STREAM_VALUE, -1)
            val kind = kindOf(stream) ?: return
            if (index < 0) return
            if (guard.isSelf(kind, index)) return
            if (SystemClock.elapsedRealtime() - lastApplyAt < QUIET_MS) return
            _userChanges.tryEmit(kind to VolumeMath.indexToPct(index, maxIndex(kind)))
        }
    }

    fun start() {
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(ACTION_VOLUME_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    fun stop() {
        runCatching { context.unregisterReceiver(receiver) }
    }

    fun maxIndex(kind: StreamKind): Int = am.getStreamMaxVolume(streamOf(kind))

    private fun minIndex(kind: StreamKind): Int = am.getStreamMinVolume(streamOf(kind))

    fun currentPct(kind: StreamKind): Int =
        VolumeMath.indexToPct(am.getStreamVolume(streamOf(kind)), maxIndex(kind))

    /** Sets a stream index. Returns false when Android refuses (ring changes without DND access). */
    fun apply(kind: StreamKind, index: Int): Boolean {
        val stream = streamOf(kind)
        val target = index.coerceIn(minIndex(kind), maxIndex(kind))
        if (am.getStreamVolume(stream) == target) return true
        guard.expect(kind, target)
        lastApplyAt = SystemClock.elapsedRealtime()
        return try {
            am.setStreamVolume(stream, target, 0)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    companion object {
        private const val ACTION_VOLUME_CHANGED = "android.media.VOLUME_CHANGED_ACTION"
        private const val EXTRA_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE"
        private const val EXTRA_STREAM_VALUE = "android.media.EXTRA_VOLUME_STREAM_VALUE"
        private const val QUIET_MS = 400L

        fun streamOf(kind: StreamKind): Int = when (kind) {
            StreamKind.MEDIA -> AudioManager.STREAM_MUSIC
            StreamKind.RING -> AudioManager.STREAM_RING
            StreamKind.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
            StreamKind.ALARM -> AudioManager.STREAM_ALARM
        }

        fun kindOf(stream: Int): StreamKind? = when (stream) {
            AudioManager.STREAM_MUSIC -> StreamKind.MEDIA
            AudioManager.STREAM_RING -> StreamKind.RING
            AudioManager.STREAM_NOTIFICATION -> StreamKind.NOTIFICATION
            AudioManager.STREAM_ALARM -> StreamKind.ALARM
            else -> null
        }
    }
}
