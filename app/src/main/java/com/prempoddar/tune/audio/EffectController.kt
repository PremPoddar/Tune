package com.prempoddar.tune.audio

import android.media.audiofx.DynamicsProcessing
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Owns one DynamicsProcessing effect per audio session and uses its input gain as that app's
 * own volume. Every change is ramped so levels glide instead of clicking.
 */
class EffectController {

    private class Holder(val effect: DynamicsProcessing, var gain: Float, var ramp: Job? = null)

    private val scope = CoroutineScope(SupervisorJob() + Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val effects = mutableMapOf<Int, Holder>()

    private val _failed = MutableStateFlow<Set<Int>>(emptySet())
    /** Sessions Tune could not attach to; their apps fall back to Switch mode. */
    val failed: StateFlow<Set<Int>> = _failed.asStateFlow()

    fun apply(gains: Map<Int, Float>) {
        scope.launch {
            (effects.keys - gains.keys).forEach(::release)
            gains.forEach { (session, gain) ->
                if (session in _failed.value) return@forEach
                val holder = effects[session] ?: create(session) ?: return@forEach
                rampTo(session, holder, gain)
            }
        }
    }

    fun releaseAll() {
        scope.launch { effects.keys.toList().forEach(::release) }
    }

    private fun create(session: Int): Holder? = try {
        val config = DynamicsProcessing.Config.Builder(
            DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
            2,
            false, 0,
            false, 0,
            false, 0,
            false,
        ).build()
        val effect = DynamicsProcessing(0, session, config)
        effect.setInputGainAllChannelsTo(0f)
        effect.enabled = true
        effect.setControlStatusListener { _, hasControl ->
            if (!hasControl) scope.launch { markFailed(session) }
        }
        Holder(effect, 0f).also { effects[session] = it }
    } catch (e: Exception) {
        Log.w(TAG, "Cannot attach to session $session", e)
        markFailed(session)
        null
    }

    private fun rampTo(session: Int, holder: Holder, target: Float) {
        if (holder.gain == target && holder.ramp?.isActive != true) return
        holder.ramp?.cancel()
        val from = holder.gain
        holder.ramp = scope.launch {
            for (step in 1..RAMP_STEPS) {
                val g = from + (target - from) * step / RAMP_STEPS
                try {
                    holder.effect.setInputGainAllChannelsTo(g)
                    holder.gain = g
                } catch (e: Exception) {
                    markFailed(session)
                    return@launch
                }
                delay(RAMP_MS / RAMP_STEPS)
            }
        }
    }

    private fun markFailed(session: Int) {
        release(session)
        _failed.value = _failed.value + session
    }

    private fun release(session: Int) {
        effects.remove(session)?.let { h ->
            h.ramp?.cancel()
            runCatching { h.effect.enabled = false }
            runCatching { h.effect.release() }
        }
    }

    private companion object {
        const val TAG = "Tune"
        const val RAMP_STEPS = 10
        const val RAMP_MS = 150L
    }
}
