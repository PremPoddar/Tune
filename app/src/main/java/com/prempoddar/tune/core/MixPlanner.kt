package com.prempoddar.tune.core

/** An app currently making sound. [startedAt] orders players from oldest to newest. */
data class Player(val pkg: String, val startedAt: Long)

data class MixInput(
    val foreground: String?,
    val players: List<Player>,
    /** Every known audio session, playing or paused, mapped to its package. */
    val sessions: Map<Int, String>,
    /** Levels for a package on the current route; unknown packages get the Default levels. */
    val levelsFor: (String) -> Levels,
    val mediaMaxIndex: Int,
    /** True when every app's own player volume can be set (Shizuku), not just audio sessions. */
    val perApp: Boolean = false,
)

data class MixPlan(
    val mediaIndex: Int,
    val masterMediaPct: Int,
    val ring: Int,
    val notification: Int,
    val alarm: Int,
    /** Gain in dB for every known session. */
    val sessionGains: Map<Int, Float>,
    /** Gain in dB for every playing app, when apps can be set one by one. */
    val appGains: Map<String, Float> = emptyMap(),
    val modes: Map<String, ControlMode>,
    /** The app the volume keys adjust; null means the Default profile. */
    val target: String?,
)

/** Turns who is on screen and who is playing into the volumes Tune should apply. */
object MixPlanner {

    fun plan(input: MixInput): MixPlan {
        val fgLevels = input.levelsFor(input.foreground ?: DEFAULT_PKG)
        val mixPkgs = input.sessions.values.toSet()
        val modes = input.players.associate { player ->
            player.pkg to if (input.perApp || player.pkg in mixPkgs) ControlMode.MIX else ControlMode.SWITCH
        }

        // A Switch-mode app can only be set through the stream itself, so it owns the master
        // exactly. Mix-mode apps can only be turned down from the master, never up.
        val newestSwitch = input.players
            .filter { modes[it.pkg] == ControlMode.SWITCH }
            .maxByOrNull { it.startedAt }
        val mixPlayers = input.players.filter { modes[it.pkg] == ControlMode.MIX }
        val masterPct = when {
            newestSwitch != null -> input.levelsFor(newestSwitch.pkg).media
            mixPlayers.isNotEmpty() -> mixPlayers.maxOf { input.levelsFor(it.pkg).media }
            else -> fgLevels.media
        }
        val mediaIndex = VolumeMath.pctToIndex(masterPct, input.mediaMaxIndex)
        val masterQ = VolumeMath.indexToPct(mediaIndex, input.mediaMaxIndex)

        val gains = if (input.perApp) emptyMap() else input.sessions.mapValues { (_, pkg) ->
            VolumeMath.gainDb(input.levelsFor(pkg).media, masterQ)
        }
        val appGains = if (!input.perApp) emptyMap() else input.players.associate { p ->
            p.pkg to VolumeMath.gainDb(input.levelsFor(p.pkg).media, masterQ)
        }

        val target = when {
            input.players.any { it.pkg == input.foreground } -> input.foreground
            input.players.isNotEmpty() -> input.players.maxBy { it.startedAt }.pkg
            else -> input.foreground
        }

        return MixPlan(
            mediaIndex = mediaIndex,
            masterMediaPct = masterQ,
            ring = fgLevels.ring,
            notification = fgLevels.notification,
            alarm = fgLevels.alarm,
            sessionGains = gains,
            appGains = appGains,
            modes = modes,
            target = target,
        )
    }
}
