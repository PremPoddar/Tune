package com.prempoddar.tune.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.prempoddar.tune.core.ControlMode
import com.prempoddar.tune.ui.theme.LevelNumber
import com.prempoddar.tune.ui.theme.rememberAppTint

/**
 * Tune's signature control: the whole card is the slider. It fills with the app's own pastel up
 * to its level; drag sideways anywhere to change it, tap to open the app's page.
 */
@Composable
fun LevelCard(
    pkg: String,
    level: Int,
    onLevel: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mode: ControlMode? = null,
    caption: String? = null,
) {
    val label = appLabel(pkg)
    val tint = rememberAppTint(pkg)
    val fill by animateFloatAsState(
        targetValue = level / 100f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fill",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClickLabel = "Open $label", onClick = onClick)
            .levelDrag(level, label, onChange = onLevel),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fill).background(tint))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(pkg, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    label, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (mode != null) ModeBadge(mode)
                    if (caption != null) {
                        Text(
                            caption, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        )
                    }
                }
            }
            Text("$level", style = LevelNumber)
            Text(
                "%", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp),
            )
        }
    }
}
