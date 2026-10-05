package com.prempoddar.tune.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import kotlin.math.roundToInt

/**
 * Makes a surface work as a horizontal level control. Dragging moves the level by how far the
 * finger travels (it never jumps to where the finger landed), so a card can be grabbed anywhere.
 * Exposed to accessibility services as an adjustable percentage.
 */
@Composable
fun Modifier.levelDrag(
    value: Int,
    label: String,
    enabled: Boolean = true,
    onChange: (Int) -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onChange)
    return this
        .semantics(mergeDescendants = true) {
            stateDescription = "$label $value percent"
            progressBarRangeInfo = ProgressBarRangeInfo(value / 100f, 0f..1f, steps = 99)
            if (enabled) {
                setProgress { target ->
                    change((target * 100).roundToInt().coerceIn(0, 100))
                    true
                }
            }
        }
        .then(
            if (!enabled) Modifier else Modifier.pointerInput(Unit) {
                var exact = 0f
                detectHorizontalDragGestures(
                    onDragStart = { exact = current.toFloat() },
                ) { change, dx ->
                    change.consume()
                    val before = exact.roundToInt()
                    exact = (exact + dx / size.width * 100f).coerceIn(0f, 100f)
                    val after = exact.roundToInt()
                    if (after != before) {
                        // A tick at every tenth and at both ends, so levels can be felt.
                        if (after / 10 != before / 10 || after == 0 || after == 100) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        change(after)
                    }
                }
            },
        )
}
