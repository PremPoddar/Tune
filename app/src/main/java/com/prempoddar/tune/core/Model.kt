package com.prempoddar.tune.core

/** The four Android volume streams Tune manages. */
enum class StreamKind { MEDIA, RING, NOTIFICATION, ALARM }

/** Volume levels in percent (0–100), one per stream. */
data class Levels(val media: Int, val ring: Int, val notification: Int, val alarm: Int) {

    operator fun get(kind: StreamKind): Int = when (kind) {
        StreamKind.MEDIA -> media
        StreamKind.RING -> ring
        StreamKind.NOTIFICATION -> notification
        StreamKind.ALARM -> alarm
    }

    fun with(kind: StreamKind, pct: Int): Levels {
        val v = pct.coerceIn(0, 100)
        return when (kind) {
            StreamKind.MEDIA -> copy(media = v)
            StreamKind.RING -> copy(ring = v)
            StreamKind.NOTIFICATION -> copy(notification = v)
            StreamKind.ALARM -> copy(alarm = v)
        }
    }

    companion object {
        /** Used when nothing at all has been stored, not even a Default profile. */
        val FALLBACK = Levels(media = 50, ring = 60, notification = 60, alarm = 80)
    }
}

/** A stored set of levels for one app on one output route. */
data class AppProfile(
    val pkg: String,
    val route: String,
    val levels: Levels,
    val pinned: Boolean,
    val updatedAt: Long,
)

/**
 * How an app's media volume is controlled.
 * MIX: Tune knows its audio session and attenuates it on its own.
 * SWITCH: Tune can only move the phone's media volume for it.
 */
enum class ControlMode { MIX, SWITCH }

/** Package name under which the Default profile is stored. */
const val DEFAULT_PKG = "*default"

const val SPEAKER_ROUTE = "speaker"
