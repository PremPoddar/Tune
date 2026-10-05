package com.prempoddar.tune.audio

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.os.Process
import androidx.core.content.ContextCompat
import com.prempoddar.tune.core.DumpParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Knows which audio sessions belong to which apps. Players announce their sessions for audio
 * effects; with the optional DUMP permission the system's playback list fills in the rest.
 */
class SessionTracker(private val context: Context) {

    private val announced = mutableMapOf<Int, String>()
    private var dumped = emptyMap<Int, String>()

    private val _sessions = MutableStateFlow<Map<Int, String>>(emptyMap())
    val sessions: StateFlow<Map<Int, String>> = _sessions.asStateFlow()

    private val _dumpStarted = MutableStateFlow<Set<String>>(emptySet())
    /** Packages the playback list reports as playing right now (only with DUMP). */
    val dumpStarted: StateFlow<Set<String>> = _dumpStarted.asStateFlow()

    val hasDump: Boolean
        get() = context.checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED

    private var pollJob: Job? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            val session = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
            val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
            if (session <= 0 || pkg.isNullOrBlank()) return
            synchronized(this@SessionTracker) {
                when (intent.action) {
                    AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> announced[session] = pkg
                    AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> announced.remove(session)
                }
                publish()
            }
        }
    }

    fun start(scope: CoroutineScope) {
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        pollJob = scope.launch {
            while (isActive) {
                if (hasDump) poll()
                delay(POLL_MS)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        runCatching { context.unregisterReceiver(receiver) }
    }

    private suspend fun poll() {
        val text = withContext(Dispatchers.IO) {
            runCatching {
                val proc = Runtime.getRuntime().exec(arrayOf("dumpsys", "audio"))
                proc.inputStream.bufferedReader().use { it.readText() }.also { proc.waitFor() }
            }.getOrNull()
        } ?: return
        val pm = context.packageManager
        val myUid = Process.myUid()
        val entries = DumpParser.parsePlayback(text)
            .filter { it.uid >= Process.FIRST_APPLICATION_UID && it.uid != myUid }
        val pkgOfUid = entries.map { it.uid }.distinct()
            .associateWith { uid -> pm.getPackagesForUid(uid)?.firstOrNull() }
        synchronized(this) {
            dumped = entries.mapNotNull { e -> pkgOfUid[e.uid]?.let { e.session to it } }.toMap()
            _dumpStarted.value = entries.filter { it.started }.mapNotNull { pkgOfUid[it.uid] }.toSet()
            publish()
        }
    }

    private fun publish() {
        _sessions.value = dumped + announced
    }

    private companion object {
        const val POLL_MS = 3_000L
    }
}
