package com.prempoddar.tune.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProfileResolverTest {
    private fun lv(media: Int) = Levels(media, 40, 40, 70)
    private fun p(pkg: String, route: String, media: Int, at: Long, pinned: Boolean = false) =
        AppProfile(pkg, route, lv(media), pinned, at)

    @Test fun exactProfileWins() {
        val all = listOf(p("spotify", "speaker", 30, 1), p("spotify", "wired", 70, 2))
        val r = ProfileResolver.resolve("spotify", "speaker", all)
        assertThat(r.levels.media).isEqualTo(30)
        assertThat(r.source).isEqualTo(LevelSource.EXACT)
    }

    @Test fun newRouteInheritsNewestOtherRouteIncludingPin() {
        val all = listOf(
            p("spotify", "speaker", 30, 1),
            p("spotify", "wired", 70, 5, pinned = true),
        )
        val r = ProfileResolver.resolve("spotify", "bt:AA", all)
        assertThat(r.levels.media).isEqualTo(70)
        assertThat(r.pinned).isTrue()
        assertThat(r.source).isEqualTo(LevelSource.INHERITED)
    }

    @Test fun unknownAppGetsDefaultForRoute() {
        val all = listOf(p(DEFAULT_PKG, "speaker", 20, 1), p(DEFAULT_PKG, "wired", 45, 1))
        val r = ProfileResolver.resolve("chrome", "wired", all)
        assertThat(r.levels.media).isEqualTo(45)
        assertThat(r.pinned).isFalse()
        assertThat(r.source).isEqualTo(LevelSource.DEFAULT)
    }

    @Test fun defaultFallsBackToSpeakerDefault() {
        val all = listOf(p(DEFAULT_PKG, "speaker", 20, 1))
        assertThat(ProfileResolver.resolve("chrome", "bt:AA", all).levels.media).isEqualTo(20)
    }

    @Test fun fallbackWhenNothingStored() {
        assertThat(ProfileResolver.resolve("chrome", "speaker", emptyList()).levels)
            .isEqualTo(Levels.FALLBACK)
    }

    @Test fun resolvingDefaultItselfNeverInheritsFromApps() {
        val all = listOf(p("spotify", "speaker", 90, 1))
        val r = ProfileResolver.resolve(DEFAULT_PKG, "speaker", all)
        assertThat(r.levels).isEqualTo(Levels.FALLBACK)
        assertThat(r.source).isEqualTo(LevelSource.DEFAULT)
    }
}
