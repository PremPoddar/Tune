package com.prempoddar.tune.core

import kotlin.math.roundToInt

/**
 * Conversions between Tune's percent levels, Android stream indices and decibels.
 *
 * Android's media volume curve is close to linear in decibels across roughly 60 dB, so
 * percent maps onto that range. It does not have to be exact: it only decides how much a
 * Mix-mode app is attenuated relative to the stream volume.
 */
object VolumeMath {
    const val MUTE_DB = -96f
    private const val RANGE_DB = 60f

    /** Nearest stream index for a percent. Any audible percent keeps at least index 1. */
    fun pctToIndex(pct: Int, maxIndex: Int): Int {
        if (pct <= 0 || maxIndex <= 0) return 0
        return (pct / 100f * maxIndex).roundToInt().coerceIn(1, maxIndex)
    }

    fun indexToPct(index: Int, maxIndex: Int): Int {
        if (maxIndex <= 0) return 0
        return (index.toFloat() / maxIndex * 100f).roundToInt().coerceIn(0, 100)
    }

    fun pctToDb(pct: Int): Float {
        if (pct <= 0) return MUTE_DB
        return RANGE_DB * (pct.coerceAtMost(100) / 100f - 1f)
    }

    /** Gain to apply to an app at [level] while the stream plays at [master]. Never boosts. */
    fun gainDb(level: Int, master: Int): Float {
        if (master <= 0) return 0f
        if (level <= 0) return MUTE_DB
        return (pctToDb(level) - pctToDb(master)).coerceIn(MUTE_DB, 0f)
    }
}
