package com.prempoddar.tune.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prempoddar.tune.core.ControlMode
import com.prempoddar.tune.ui.design.TonalCard
import com.prempoddar.tune.ui.theme.LevelNumberSmall
import com.prempoddar.tune.ui.theme.rememberAppAccent
import kotlin.math.roundToInt

/**
 * One app in the mixer: its icon, name and state, and a large slider in the app's own colour
 * that applies the moment it moves. Tapping the card opens the app's page.
 */
@Composable
fun MixerCard(
    pkg: String,
    level: Int,
    status: String,
    playing: Boolean,
    mode: ControlMode?,
    pinned: Boolean,
    onLevel: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = appLabel(pkg)
    val accent = rememberAppAccent(pkg)
    // While dragging, show the finger's value at once rather than waiting for the engine.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging?.roundToInt() ?: level
    TonalCard(
        modifier.fillMaxWidth(),
        color = if (playing) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
    ) {
        Column(Modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(pkg, size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            label, style = MaterialTheme.typography.titleMedium, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                        )
                        if (pinned) {
                            Icon(
                                Icons.Rounded.PushPin, contentDescription = "Levels kept",
                                modifier = Modifier.padding(start = 6.dp).size(14.dp), tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (mode != null) ModeBadge(mode)
                        Text(
                            status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text("$shown%", style = LevelNumberSmall)
            }
            Slider(
                value = dragging ?: level.toFloat(),
                onValueChange = { v ->
                    dragging = v
                    onLevel(v.roundToInt())
                },
                onValueChangeFinished = { dragging = null },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = accent, activeTrackColor = accent,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).semantics { contentDescription = "$label volume" },
            )
        }
    }
}
