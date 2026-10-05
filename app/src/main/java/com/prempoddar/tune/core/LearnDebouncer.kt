package com.prempoddar.tune.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Saves a level only once the user has stopped moving it, so dragging a slider or holding a
 * volume key produces one write instead of dozens.
 */
class LearnDebouncer(
    private val scope: CoroutineScope,
    private val delayMs: Long = 1_500,
    private val save: suspend (pkg: String, kind: StreamKind, pct: Int) -> Unit,
) {
    private val pending = mutableMapOf<Pair<String, StreamKind>, Job>()

    @Synchronized
    fun onUserChange(pkg: String, kind: StreamKind, pct: Int) {
        val key = pkg to kind
        pending.remove(key)?.cancel()
        pending[key] = scope.launch {
            delay(delayMs)
            save(pkg, kind, pct)
            synchronized(this@LearnDebouncer) { pending.remove(key) }
        }
    }

    @Synchronized
    fun cancelAll() {
        pending.values.forEach { it.cancel() }
        pending.clear()
    }
}
