package com.prempoddar.tune.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MixPlannerTest {
    private val media = mutableMapOf<String, Int>()
    private val levelsFor: (String) -> Levels = { pkg ->
        Levels(media[pkg] ?: 50, ring = if (pkg == DEFAULT_PKG) 11 else 40, notification = 40, alarm = 70)
    }

    private fun input(
        fg: String?,
        players: List<Player> = emptyList(),
        sessions: Map<Int, String> = emptyMap(),
    ) = MixInput(fg, players, sessions, levelsFor, mediaMaxIndex = 15)

    @Test fun noPlayersFollowsForeground() {
        media["chrome"] = 40
        val plan = MixPlanner.plan(input("chrome"))
        assertThat(plan.mediaIndex).isEqualTo(6)
        assertThat(plan.masterMediaPct).isEqualTo(40)
        assertThat(plan.ring).isEqualTo(40)
        assertThat(plan.alarm).isEqualTo(70)
        assertThat(plan.target).isEqualTo("chrome")
    }

    @Test fun mixPlayersShareLoudestMasterAndAttenuateTheRest() {
        media["spotify"] = 30; media["pods"] = 80
        val plan = MixPlanner.plan(
            input(
                "launcher",
                players = listOf(Player("spotify", 1), Player("pods", 2)),
                sessions = mapOf(10 to "spotify", 20 to "pods"),
            ),
        )
        assertThat(plan.mediaIndex).isEqualTo(12)
        assertThat(plan.masterMediaPct).isEqualTo(80)
        assertThat(plan.sessionGains[10]!!).isWithin(0.01f).of(-30f)
        assertThat(plan.sessionGains[20]).isEqualTo(0f)
        assertThat(plan.modes).containsExactly("spotify", ControlMode.MIX, "pods", ControlMode.MIX)
    }

    @Test fun switchPlayerSetsMasterExactlyAndMixPlayersAreCapped() {
        media["youtube"] = 33; media["spotify"] = 80
        val plan = MixPlanner.plan(
            input(
                "youtube",
                players = listOf(Player("youtube", 1), Player("spotify", 2)),
                sessions = mapOf(10 to "spotify"),
            ),
        )
        assertThat(plan.mediaIndex).isEqualTo(5)
        assertThat(plan.masterMediaPct).isEqualTo(33)
        assertThat(plan.sessionGains[10]).isEqualTo(0f)
        assertThat(plan.modes["youtube"]).isEqualTo(ControlMode.SWITCH)
    }

    @Test fun newestSwitchPlayerWins() {
        media["a"] = 20; media["b"] = 60
        val plan = MixPlanner.plan(input(null, players = listOf(Player("a", 5), Player("b", 9))))
        assertThat(plan.masterMediaPct).isEqualTo(60)
    }

    @Test fun pausedKnownSessionStillGetsGain() {
        media["chrome"] = 100; media["spotify"] = 50
        val plan = MixPlanner.plan(input("chrome", sessions = mapOf(7 to "spotify")))
        assertThat(plan.masterMediaPct).isEqualTo(100)
        assertThat(plan.sessionGains[7]!!).isWithin(0.01f).of(-30f)
        assertThat(plan.modes).isEmpty()
    }

    @Test fun targetPrefersForegroundPlayerThenNewestPlayer() {
        val players = listOf(Player("a", 1), Player("b", 2))
        assertThat(MixPlanner.plan(input("a", players)).target).isEqualTo("a")
        assertThat(MixPlanner.plan(input("chrome", players)).target).isEqualTo("b")
        assertThat(MixPlanner.plan(input(null)).target).isNull()
    }

    @Test fun noForegroundUsesDefaultForOtherStreams() {
        assertThat(MixPlanner.plan(input(null)).ring).isEqualTo(11)
    }
}
