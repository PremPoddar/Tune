package com.prempoddar.tune.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Process
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method
import kotlin.math.pow

/**
 * Exact per-app volume through Shizuku, the way Android's own audio policy ducks apps: the audio
 * service is asked (as the shell user) for every player, and each player's volume is scaled on
 * its own. Works for every app, not only those that expose an audio session.
 */
class ShizukuBackend(private val context: Context) {

    enum class Status { NOT_INSTALLED, NOT_RUNNING, NEEDS_PERMISSION, READY, FAILED }

    private val _status = MutableStateFlow(Status.NOT_INSTALLED)
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _playing = MutableStateFlow<Map<String, Long>>(emptyMap())
    /** Apps with a started media player, with when Tune first saw each one. */
    val playing: StateFlow<Map<String, Long>> = _playing.asStateFlow()

    private val thread = HandlerThread("tune-shizuku").apply { start() }
    private val handler = Handler(thread.looper)
    private val am = context.getSystemService(AudioManager::class.java)
    private val pm = context.packageManager

    private var service: Any? = null
    private var getConfigs: Method? = null
    private var desired: Map<String, Float> = emptyMap()
    /** Volume last sent to each player, by player id, so unchanged players aren't touched. */
    private val sent = mutableMapOf<Int, Float>()
    private var started = false

    private val callback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            handler.post { refresh() }
        }
    }
    private val binderReceived = Shizuku.OnBinderReceivedListener { handler.post { updateStatus() } }
    private val binderDead = Shizuku.OnBinderDeadListener { handler.post { service = null; updateStatus() } }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> handler.post { updateStatus() } }

    fun start() {
        if (started) return
        started = true
        Shizuku.addBinderReceivedListenerSticky(binderReceived, handler)
        Shizuku.addBinderDeadListener(binderDead, handler)
        Shizuku.addRequestPermissionResultListener(permissionResult, handler)
        am.registerAudioPlaybackCallback(callback, handler)
        handler.post { updateStatus() }
    }

    fun stop() {
        if (!started) return
        started = false
        am.unregisterAudioPlaybackCallback(callback)
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        handler.post {
            desired = emptyMap()
            refresh()
            _playing.value = emptyMap()
        }
    }

    /** Re-reads whether Shizuku is installed, running and allowed; call after returning to Tune. */
    fun recheck() = handler.post { updateStatus() }

    /** Shows Shizuku's own prompt. Only valid while the status is [Status.NEEDS_PERMISSION]. */
    fun requestPermission() {
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
    }

    /** Sets each app's gain in dB; apps not listed play at full volume. */
    fun apply(gainsDb: Map<String, Float>) {
        val linear = gainsDb.mapValues { (_, db) -> if (db <= -90f) 0f else 10f.pow(db / 20f) }
        handler.post {
            desired = linear
            refresh()
        }
    }

    private fun updateStatus() {
        _status.value = when {
            !installed() -> Status.NOT_INSTALLED
            !Shizuku.pingBinder() -> Status.NOT_RUNNING
            Shizuku.isPreV11() -> Status.FAILED
            Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> Status.NEEDS_PERMISSION
            connect() -> Status.READY
            else -> Status.FAILED
        }
        refresh()
    }

    private fun installed() = runCatching { pm.getPackageInfo(SHIZUKU_PKG, 0); true }.getOrDefault(false)

    private fun connect(): Boolean {
        if (service != null) return true
        return try {
            HiddenApiBypass.addHiddenApiExemptions("")
            val raw = SystemServiceHelper.getSystemService(Context.AUDIO_SERVICE) ?: return false
            val stub = Class.forName("android.media.IAudioService\$Stub")
            val svc = stub.getMethod("asInterface", IBinder::class.java).invoke(null, ShizukuBinderWrapper(raw))!!
            getConfigs = svc.javaClass.getMethod("getActivePlaybackConfigurations")
            service = svc
            true
        } catch (e: Throwable) {
            Log.w(TAG, "Shizuku audio service unavailable", e)
            false
        }
    }

    /** Lists players and brings each one to the volume its app should have. */
    private fun refresh() {
        val svc = service
        if (_status.value != Status.READY || svc == null) {
            if (_playing.value.isNotEmpty()) _playing.value = emptyMap()
            return
        }
        val configs = try {
            @Suppress("UNCHECKED_CAST")
            getConfigs!!.invoke(svc) as List<AudioPlaybackConfiguration>
        } catch (e: Throwable) {
            Log.w(TAG, "Cannot list players", e)
            service = null
            _status.value = Status.FAILED
            return
        }
        val now = SystemClock.elapsedRealtime()
        val seen = _playing.value
        val playingNow = mutableMapOf<String, Long>()
        val alive = mutableSetOf<Int>()
        for (c in configs) {
            val usage = c.audioAttributes.usage
            if (usage != AudioAttributes.USAGE_MEDIA && usage != AudioAttributes.USAGE_GAME && usage != AudioAttributes.USAGE_UNKNOWN) continue
            val uid = hidden<Int>(c, "getClientUid") ?: continue
            if (uid < Process.FIRST_APPLICATION_UID || uid == Process.myUid()) continue
            val pkg = pm.getPackagesForUid(uid)?.firstOrNull() ?: continue
            val id = hidden<Int>(c, "getPlayerInterfaceId") ?: continue
            alive += id
            if (hidden<Int>(c, "getPlayerState") == PLAYER_STATE_STARTED) playingNow[pkg] = seen[pkg] ?: now
            val volume = desired[pkg] ?: 1f
            if (sent[id] == volume) continue
            val proxy = hidden<Any>(c, "getPlayerProxy") ?: continue
            try {
                proxy.javaClass.getMethod("setVolume", Float::class.javaPrimitiveType).invoke(proxy, volume)
                sent[id] = volume
            } catch (e: Throwable) {
                Log.w(TAG, "Cannot set volume for $pkg", e)
            }
        }
        sent.keys.retainAll(alive)
        if (playingNow != seen) _playing.value = playingNow
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> hidden(target: Any, name: String): T? =
        runCatching { target.javaClass.getMethod(name).invoke(target) as T }.getOrNull()

    private companion object {
        const val TAG = "Tune"
        const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
        const val REQUEST_CODE = 41
        const val PLAYER_STATE_STARTED = 2
    }
}
