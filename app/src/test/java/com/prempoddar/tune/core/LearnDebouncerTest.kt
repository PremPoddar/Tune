package com.prempoddar.tune.core

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearnDebouncerTest {
    private val saved = mutableListOf<Triple<String, StreamKind, Int>>()

    @Test fun savesAfterQuietPeriod() = runTest {
        val d = LearnDebouncer(backgroundScope, 1_500) { p, k, v -> saved += Triple(p, k, v) }
        d.onUserChange("spotify", StreamKind.MEDIA, 40)
        advanceTimeBy(1_400); runCurrent()
        assertThat(saved).isEmpty()
        advanceTimeBy(200); runCurrent()
        assertThat(saved).containsExactly(Triple("spotify", StreamKind.MEDIA, 40))
    }

    @Test fun burstSavesOnlyLastValue() = runTest {
        val d = LearnDebouncer(backgroundScope, 1_500) { p, k, v -> saved += Triple(p, k, v) }
        d.onUserChange("spotify", StreamKind.MEDIA, 40)
        advanceTimeBy(1_000)
        d.onUserChange("spotify", StreamKind.MEDIA, 45)
        advanceTimeBy(1_000)
        d.onUserChange("spotify", StreamKind.MEDIA, 50)
        advanceTimeBy(2_000); runCurrent()
        assertThat(saved).containsExactly(Triple("spotify", StreamKind.MEDIA, 50))
    }

    @Test fun differentKeysAreIndependent() = runTest {
        val d = LearnDebouncer(backgroundScope, 1_500) { p, k, v -> saved += Triple(p, k, v) }
        d.onUserChange("spotify", StreamKind.MEDIA, 40)
        d.onUserChange("spotify", StreamKind.RING, 10)
        d.onUserChange("chrome", StreamKind.MEDIA, 70)
        advanceTimeBy(2_000); runCurrent()
        assertThat(saved).hasSize(3)
    }

    @Test fun cancelAllDropsPending() = runTest {
        val d = LearnDebouncer(backgroundScope, 1_500) { p, k, v -> saved += Triple(p, k, v) }
        d.onUserChange("spotify", StreamKind.MEDIA, 40)
        d.cancelAll()
        advanceTimeBy(2_000); runCurrent()
        assertThat(saved).isEmpty()
    }
}
