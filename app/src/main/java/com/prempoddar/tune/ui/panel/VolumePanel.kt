package com.prempoddar.tune.ui.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.core.DEFAULT_PKG
import com.prempoddar.tune.core.StreamKind
import com.prempoddar.tune.ui.components.AppIcon
import com.prempoddar.tune.ui.components.StreamSlider
import com.prempoddar.tune.ui.components.appLabel
import com.prempoddar.tune.ui.components.levelDrag
import com.prempoddar.tune.ui.theme.LevelNumberSmall
import com.prempoddar.tune.ui.theme.TuneTheme
import com.prempoddar.tune.ui.theme.rememberAppTint

@Composable
fun VolumePanel(
    visible: Boolean,
    expanded: Boolean,
    onExpand: () -> Unit,
    onInteract: () -> Unit,
    onOpenTune: () -> Unit,
    onHidden: () -> Unit,
    onDismiss: () -> Unit,
) {
    val engine = AppGraph.engine
    val state by engine.state.collectAsStateWithLifecycle()
    val transition = remember { MutableTransitionState(false) }
    transition.targetState = visible
    LaunchedEffect(transition.isIdle, transition.currentState) {
        if (transition.isIdle && !transition.currentState) onHidden()
    }

    TuneTheme(engine.settings.theme) {
        // A tap anywhere outside the card closes the panel, like the system one.
        Box(Modifier.fillMaxSize().then(if (visible) Modifier.pointerInput(Unit) { detectTapGestures { onDismiss() } } else Modifier)) {
        AnimatedVisibility(
            visibleState = transition,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            Box(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 12.dp,
                    modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
                        .pointerInput(Unit) { detectTapGestures { } } // taps on the card stay on the card
                        .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val target = state.target
                        PanelRow(
                            pkg = target,
                            level = state.levelsFor(target).media,
                            onLevel = {
                                engine.setLevel(target, StreamKind.MEDIA, it)
                                onInteract()
                            },
                            trailing = {
                                Icon(
                                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = if (expanded) "Show less" else "Show all volumes",
                                    modifier = Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onExpand).padding(6.dp),
                                )
                            },
                        )
                        if (expanded) {
                            state.players.map { it.pkg }.filter { it != target }.forEach { pkg ->
                                PanelRow(pkg, state.levelsFor(pkg).media, onLevel = {
                                    engine.setLevel(pkg, StreamKind.MEDIA, it)
                                    onInteract()
                                })
                            }
                            val fg = state.foreground ?: DEFAULT_PKG
                            val fgLevels = state.levelsFor(fg)
                            val tint = MaterialTheme.colorScheme.primaryContainer
                            StreamSlider("Ring", Icons.Rounded.RingVolume, fgLevels.ring, tint, {
                                engine.setLevel(fg, StreamKind.RING, it); onInteract()
                            })
                            StreamSlider("Notifications", Icons.Rounded.Notifications, fgLevels.notification, tint, {
                                engine.setLevel(fg, StreamKind.NOTIFICATION, it); onInteract()
                            })
                            StreamSlider("Alarm", Icons.Rounded.Alarm, fgLevels.alarm, tint, {
                                engine.setLevel(fg, StreamKind.ALARM, it); onInteract()
                            })
                            TextButton(onClick = onOpenTune, modifier = Modifier.align(Alignment.End)) { Text("Open Tune") }
                        }
                    }
                }
            }
        }
        }
    }
}

/** A compact level card: the row fills with the app's pastel up to its level. */
@Composable
private fun PanelRow(pkg: String, level: Int, onLevel: (Int) -> Unit, trailing: @Composable () -> Unit = {}) {
    val tint = rememberAppTint(pkg)
    val label = appLabel(pkg)
    Box(
        Modifier.fillMaxWidth().height(64.dp).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant).levelDrag(level, label, onChange = onLevel),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(level / 100f).background(tint))
        Row(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(pkg, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("$level%", style = LevelNumberSmall)
            Spacer(Modifier.size(6.dp))
            trailing()
        }
    }
}
