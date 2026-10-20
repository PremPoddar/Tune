package com.prempoddar.tune.ui.components

import com.prempoddar.tune.ui.design.tap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prempoddar.tune.core.ControlMode

/** Says how Tune controls an app: on its own (Mix) or through the phone's volume (Switch). */
@Composable
fun ModeBadge(mode: ControlMode, modifier: Modifier = Modifier) {
    val (text, bg, fg) = when (mode) {
        ControlMode.MIX -> Triple("Mix", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        ControlMode.SWITCH -> Triple("Switch", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = modifier.clip(CircleShape).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 6.dp, top = 22.dp, bottom = 10.dp),
    )
}

/** A calm notice with one action that fixes what it describes. */
@Composable
fun Banner(
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    warning: Boolean = false,
) {
    val bg: Color = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val fg: Color = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(bg)
            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = fg)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = fg.copy(alpha = 0.8f), modifier = Modifier.padding(end = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = tap(onAction)) { Text(action, color = fg) }
        }
    }
}
