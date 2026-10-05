package com.prempoddar.tune.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SelfChangeGuardTest {
    private var now = 0L
    private val guard = SelfChangeGuard(clock = { now }, ttlMs = 1_000)

    @Test fun expectedChangeIsSelfOnce() {
        guard.expect(StreamKind.MEDIA, 7)
        assertThat(guard.isSelf(StreamKind.MEDIA, 7)).isTrue()
        assertThat(guard.isSelf(StreamKind.MEDIA, 7)).isFalse()
    }

    @Test fun unexpectedChangeIsUser() {
        guard.expect(StreamKind.MEDIA, 7)
        assertThat(guard.isSelf(StreamKind.MEDIA, 8)).isFalse()
        assertThat(guard.isSelf(StreamKind.RING, 7)).isFalse()
    }

    @Test fun expectationExpires() {
        guard.expect(StreamKind.MEDIA, 7)
        now = 1_001
        assertThat(guard.isSelf(StreamKind.MEDIA, 7)).isFalse()
    }
}
