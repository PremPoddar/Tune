package com.prempoddar.tune.core

/**
 * Remembers the volume changes Tune makes itself, so the change broadcast they trigger is not
 * mistaken for the user moving the volume (and learned as a new level).
 */
class SelfChangeGuard(
    private val clock: () -> Long = System::currentTimeMillis,
    private val ttlMs: Long = 1_000,
) {
    private val expected = mutableMapOf<StreamKind, Pair<Int, Long>>()

    @Synchronized
    fun expect(kind: StreamKind, index: Int) {
        expected[kind] = index to clock()
    }

    @Synchronized
    fun isSelf(kind: StreamKind, index: Int): Boolean {
        val (want, at) = expected[kind] ?: return false
        if (clock() - at > ttlMs) {
            expected.remove(kind)
            return false
        }
        if (want != index) return false
        expected.remove(kind)
        return true
    }
}
