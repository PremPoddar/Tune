package com.prempoddar.tune.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.core.DEFAULT_PKG
import com.prempoddar.tune.data.TuneSettings
import com.prempoddar.tune.service.PermissionState
import com.prempoddar.tune.ui.components.RouteChips
import com.prempoddar.tune.ui.components.ShizukuCard
import com.prempoddar.tune.ui.components.StreamSliders
import com.prempoddar.tune.ui.design.ChoiceChips
import com.prempoddar.tune.ui.design.ExpressiveScaffold
import com.prempoddar.tune.ui.design.SectionLabel
import com.prempoddar.tune.ui.design.SettingsGroup
import com.prempoddar.tune.ui.design.SettingsRow
import com.prempoddar.tune.ui.design.SwitchRow
import com.prempoddar.tune.ui.onboarding.AdbCommandCard
import com.prempoddar.tune.ui.onboarding.open
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onAppearance: () -> Unit, onAbout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = AppGraph.engine
    val state by engine.state.collectAsStateWithLifecycle()
    val settings by AppGraph.settings.settings.collectAsStateWithLifecycle(TuneSettings())
    val seen by AppGraph.profiles.routes.collectAsStateWithLifecycle(emptyList())
    var perms by remember { mutableStateOf(PermissionState.read(context)) }
    LifecycleResumeEffect(Unit) {
        perms = PermissionState.read(context)
        engine.shizuku.recheck()
        onPauseOrDispose { }
    }
    fun update(f: (TuneSettings) -> TuneSettings) = scope.launch { AppGraph.settings.update(f) }

    val routes = (listOf(state.route) + seen).distinctBy { it.key }
    var route by rememberSaveable { mutableStateOf(state.route.key) }

    ExpressiveScaffold("Settings", onBack = onBack) { pad ->
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp)) {
            item {
                SettingsGroup("Engine") {
                    SwitchRow(
                        "Use Shizuku when available", "Exact volume for every app, including ones that don't share their audio",
                        Icons.Rounded.Bolt, settings.useShizuku,
                    ) { on -> update { it.copy(useShizuku = on) } }
                }
                if (settings.useShizuku) ShizukuCard(state.shizuku, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            if (!state.perApp) item {
                SectionLabel("Without Shizuku")
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Apps that don't share their audio, like YouTube and browsers, can only use the phone's " +
                            "volume. Grant one extra permission over adb and Tune can mix them too.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    AdbCommandCard(granted = perms.dump)
                }
            }
            item {
                SectionLabel("Default levels")
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "For apps you haven't set a volume for yet.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    if (routes.size > 1) RouteChips(routes, route, state.route.key, onSelect = { route = it })
                    StreamSliders(
                        state.resolved(DEFAULT_PKG, route).levels, MaterialTheme.colorScheme.secondaryContainer,
                        onChange = { kind, pct -> engine.setLevel(DEFAULT_PKG, kind, pct, route) },
                    )
                }
            }
            item {
                SettingsGroup("Volume buttons") {
                    SwitchRow(
                        "Tune's volume panel", "The buttons change the app you're using, not the whole phone",
                        Icons.Rounded.VolumeUp, settings.panelEnabled,
                    ) { on -> update { it.copy(panelEnabled = on) } }
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Each press changes", style = MaterialTheme.typography.titleSmall)
                        ChoiceChips(listOf(2 to "2%", 5 to "5%", 10 to "10%"), settings.stepPct, { step -> update { it.copy(stepPct = step) } }, Modifier.padding(top = 6.dp))
                    }
                }
            }
            item {
                SettingsGroup("Permissions") {
                    PermissionRow("Accessibility", "Which app is open, volume buttons", Icons.Rounded.Accessibility, perms.accessibility) {
                        context.open(PermissionState.accessibilityIntent())
                    }
                    PermissionRow("Notification access", "Music playing in the background", Icons.Rounded.Notifications, perms.notifications) {
                        context.open(PermissionState.notificationIntent(context))
                    }
                    PermissionRow("Do Not Disturb access", "Ring and notification volume", Icons.Rounded.DoNotDisturbOn, perms.dnd) {
                        context.open(PermissionState.dndIntent())
                    }
                    PermissionRow("Battery optimisation", "Stop Android from closing Tune", Icons.Rounded.BatterySaver, null) {
                        context.open(PermissionState.batteryIntent())
                    }
                }
            }
            item {
                SettingsGroup("General") {
                    SettingsRow("Appearance", "Theme, colours and black backgrounds", Icons.Rounded.Palette, onClick = onAppearance) { Chevron() }
                    SettingsRow("About Tune", "Version, licences", Icons.Rounded.Info, onClick = onAbout) { Chevron() }
                }
            }
        }
    }
}

@Composable
private fun Chevron() = Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)

/** [granted] null means the state can't be read and the row only links to the setting. */
@Composable
private fun PermissionRow(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector, granted: Boolean?, onOpen: () -> Unit) {
    SettingsRow(
        title,
        when (granted) {
            true -> "On · $body"
            false -> "Off · $body"
            null -> body
        },
        icon, onClick = onOpen,
    ) {
        when (granted) {
            true -> Text("On", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            false -> Text("Turn on", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
            null -> Chevron()
        }
    }
}
