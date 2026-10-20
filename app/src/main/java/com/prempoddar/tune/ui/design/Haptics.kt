package com.prempoddar.tune.ui.design

import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import java.lang.ref.WeakReference

/**
 * Touch feedback with meaning: a light tap for buttons, a distinct click for switches turning on
 * and off, soft ticks while dragging a slider, and a firmer buzz when something is done or
 * refused. It goes through the system, so it follows the phone's own "touch feedback" setting,
 * and it can be turned off in Look & feel.
 */
object Haptics {
    @Volatile var enabled = true
    private var view: WeakReference<View>? = null
    private var lastSlideStep = Int.MIN_VALUE
    private var lastSlideAt = 0L

    /** The activity's window, which plays the feedback. Call from onCreate. */
    fun attach(v: View) { view = WeakReference(v) }

    /** A button, menu item or chip was pressed. */
    fun tap() = perform(HapticFeedbackConstants.KEYBOARD_TAP)

    /** A switch or check box changed. */
    fun toggle(on: Boolean) = perform(
        if (Build.VERSION.SDK_INT >= 34) (if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF)
        else HapticFeedbackConstants.CLOCK_TICK,
    )

    /** One of a set of choices was picked. */
    fun select() = perform(HapticFeedbackConstants.CLOCK_TICK)

    /** Something finished well: saved, sent, done. */
    fun confirm() = perform(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)

    /** Something was refused or failed. */
    fun reject() = perform(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)

    /** A drag crossed a point where letting go does something (swipe to delete, pull to refresh). */
    fun threshold() = perform(if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE else HapticFeedbackConstants.CONTEXT_CLICK)

    /**
     * A slider moved to [fraction] of its range: a tick each twentieth of the way, never faster
     * than the finger can feel them apart.
     */
    fun slide(fraction: Float) {
        val step = (fraction.coerceIn(0f, 1f) * 20).toInt()
        val now = SystemClock.uptimeMillis()
        if (step == lastSlideStep || now - lastSlideAt < 35) return
        lastSlideStep = step; lastSlideAt = now
        perform(if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK else HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun perform(type: Int) {
        if (!enabled) return
        view?.get()?.performHapticFeedback(type)
    }
}

/** [block] with a tap felt first: `onClick = tap { … }`. */
fun tap(block: () -> Unit): () -> Unit = { Haptics.tap(); block() }

/** [block] with the switch's on or off felt first: `onCheckedChange = toggled { … }`. */
fun toggled(block: (Boolean) -> Unit): (Boolean) -> Unit = { Haptics.toggle(it); block(it) }

/** A slider's change with ticks as it moves through [range]: `onValueChange = sliding(0f..1f) { … }`. */
fun sliding(range: ClosedFloatingPointRange<Float> = 0f..1f, block: (Float) -> Unit): (Float) -> Unit = { v ->
    val span = range.endInclusive - range.start
    Haptics.slide(if (span > 0) (v - range.start) / span else 0f)
    block(v)
}
