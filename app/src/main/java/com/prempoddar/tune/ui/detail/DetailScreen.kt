package com.prempoddar.tune.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.core.ControlMode
import com.prempoddar.tune.core.LevelSource
import com.prempoddar.tune.ui.components.AppIcon
import com.prempoddar.tune.ui.components.RouteChips
import com.prempoddar.tune.ui.components.SectionLabel
import com.prempoddar.tune.ui.components.StreamSliders
import com.prempoddar.tune.ui.components.appLabel
import com.prempoddar.tune.ui.theme.rememberAppTint
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(pkg: String, onBack: () -> Unit) {
    val engine = AppGraph.engine
    val state by engine.state.collectAsStateWithLifecycle()
    val seen by AppGraph.profiles.routes.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val tint = rememberAppTint(pkg)
    val label = appLabel(pkg)

    // Outputs this app has levels for, plus the one playing now.
    val ownRoutes = state.profiles.filter { it.pkg == pkg }.map { it.route }.toSet() + state.route.key
    val routes = (listOf(state.route) + seen).distinctBy { it.key }.filter { it.key in ownRoutes }
    var selected by rememberSaveable { mutableStateOf(state.route.key) }
    val resolved = state.resolved(pkg, selected)
    var confirmForget by rememberSaveable { mutableStateOf(false) }

    val insets = WindowInsets.safeDrawing.asPaddingValues()
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
            .padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding() + 24.dp)
            .padding(horizontal = 16.dp),
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(104.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                AppIcon(pkg, size = 60.dp)
            }
            Spacer(Modifier.height(14.dp))
            Text(label, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                modeLine(state.plan?.modes?.get(pkg), resolved.source),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
            )
        }

        if (routes.size > 1) {
            SectionLabel("Output")
            RouteChips(routes, selected, state.route.key, onSelect = { selected = it })
        }

        SectionLabel("Levels")
        StreamSliders(resolved.levels, tint, onChange = { kind, pct -> engine.setLevel(pkg, kind, pct, selected) })

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Keep these levels", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Volume buttons still work while you use $label, but it goes back to these levels next time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = resolved.pinned,
                onCheckedChange = { pinned ->
                    scope.launch { AppGraph.profiles.setPinned(pkg, selected, pinned, resolved.levels) }
                },
                modifier = Modifier.padding(start = 12.dp),
            )
        }

        TextButton(onClick = { confirmForget = true }, modifier = Modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally)) {
            Text("Forget $label", color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text("Forget $label?") },
            text = { Text("Its saved levels for every output are removed. It will use your Default levels until you set new ones.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmForget = false
                    scope.launch { AppGraph.profiles.forget(pkg) }
                    onBack()
                }) { Text("Forget") }
            },
            dismissButton = { TextButton(onClick = { confirmForget = false }) { Text("Cancel") } },
        )
    }
}

private fun modeLine(mode: ControlMode?, source: LevelSource): String = when {
    mode == ControlMode.MIX -> "Playing at its own volume alongside other apps."
    mode == ControlMode.SWITCH -> "Playing through the phone's volume. This app doesn't share its audio with Tune."
    source == LevelSource.DEFAULT -> "Using your Default levels."
    source == LevelSource.INHERITED -> "First time on this output, so these are its levels from last time."
    else -> "Saved levels"
}

