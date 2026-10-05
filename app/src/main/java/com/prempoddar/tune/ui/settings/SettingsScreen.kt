package com.prempoddar.tune.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.core.DEFAULT_PKG
import com.prempoddar.tune.data.ThemeMode
import com.prempoddar.tune.data.TuneSettings
import com.prempoddar.tune.service.PermissionState
import com.prempoddar.tune.ui.components.RouteChips
import com.prempoddar.tune.ui.components.SectionLabel
import com.prempoddar.tune.ui.components.StreamSliders
import com.prempoddar.tune.ui.onboarding.AdbCommandCard
import com.prempoddar.tune.ui.onboarding.open
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = AppGraph.engine
    val state by engine.state.collectAsStateWithLifecycle()
    val settings by AppGraph.settings.settings.collectAsStateWithLifecycle(TuneSettings())
    val seen by AppGraph.profiles.routes.collectAsStateWithLifecycle(emptyList())
    var perms by remember { mutableStateOf(PermissionState.read(context)) }
    LifecycleResumeEffect(Unit) {
        perms = PermissionState.read(context)
        onPauseOrDispose { }
    }
    fun update(f: (TuneSettings) -> TuneSettings) = scope.launch { AppGraph.settings.update(f) }

    val routes = (listOf(state.route) + seen).distinctBy { it.key }
    var route by rememberSaveable { mutableStateOf(state.route.key) }
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState())
            .padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding() + 32.dp)
            .padding(horizontal = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 4.dp))
        }

        SectionLabel("Appearance")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
            options.forEachIndexed { i, (mode, name) ->
                SegmentedButton(
                    selected = settings.theme == mode,
                    onClick = { update { it.copy(theme = mode) } },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                ) { Text(name) }
            }
        }

        SectionLabel("Default levels")
        Text(
            "Used for apps you haven't set a volume for yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
        )
        if (routes.size > 1) {
            RouteChips(routes, route, state.route.key, onSelect = { route = it }, modifier = Modifier.padding(bottom = 10.dp))
        }
        StreamSliders(
            state.resolved(DEFAULT_PKG, route).levels,
            MaterialTheme.colorScheme.primaryContainer,
            onChange = { kind, pct -> engine.setLevel(DEFAULT_PKG, kind, pct, route) },
        )

        SectionLabel("Volume buttons")
        Card {
            ToggleRow(
                "Use Tune's volume panel",
                "The buttons change the volume of the app you're using, not the whole phone.",
                settings.panelEnabled,
            ) { on -> update { it.copy(panelEnabled = on) } }
            Text("Each press changes", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 14.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                val steps = listOf(2, 5, 10)
                steps.forEachIndexed { i, step ->
                    SegmentedButton(
                        selected = settings.stepPct == step,
                        onClick = { update { it.copy(stepPct = step) } },
                        shape = SegmentedButtonDefaults.itemShape(i, steps.size),
                    ) { Text("$step%") }
                }
            }
        }

        SectionLabel("Mix more apps")
        Text(
            "Apps that don't share their audio, like YouTube and browsers, can only use the phone's " +
                "volume. Grant one extra permission over adb and Tune can mix them too.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
        )
        AdbCommandCard(granted = perms.dump)

        SectionLabel("Permissions")
        Card {
            PermissionRow("Accessibility", "Which app is open, volume buttons", perms.accessibility) {
                context.open(PermissionState.accessibilityIntent())
            }
            PermissionRow("Notification access", "Music playing in the background", perms.notifications) {
                context.open(PermissionState.notificationIntent(context))
            }
            PermissionRow("Do Not Disturb access", "Ring and notification volume", perms.dnd) {
                context.open(PermissionState.dndIntent())
            }
            PermissionRow("Battery optimisation", "Stop Android from closing Tune", null) {
                context.open(PermissionState.batteryIntent())
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Tune ${versionName(context)} · Everything stays on your phone.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
private fun ToggleRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange, Modifier.padding(start = 12.dp))
    }
}

/** [granted] null means the state can't be read and the row only links to the setting. */
@Composable
private fun PermissionRow(title: String, body: String, granted: Boolean?, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onOpen).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dot: Color = when (granted) {
            true -> MaterialTheme.colorScheme.primary
            false -> MaterialTheme.colorScheme.error
            null -> MaterialTheme.colorScheme.outline
        }
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                when (granted) {
                    true -> "On · $body"
                    false -> "Off · $body"
                    null -> body
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun versionName(context: android.content.Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
