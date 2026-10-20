package com.prempoddar.tune.ui.home

import com.prempoddar.tune.ui.design.tap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.FilledTonalIconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.audio.EngineState
import com.prempoddar.tune.audio.ShizukuBackend
import com.prempoddar.tune.core.ControlMode
import com.prempoddar.tune.core.DEFAULT_PKG
import com.prempoddar.tune.core.RouteInfo
import com.prempoddar.tune.core.StreamKind
import com.prempoddar.tune.data.TuneSettings
import com.prempoddar.tune.service.PermissionState
import com.prempoddar.tune.ui.components.Banner
import com.prempoddar.tune.ui.components.MixerCard
import com.prempoddar.tune.ui.components.PhoneVolumes
import com.prempoddar.tune.ui.components.ShizukuCard
import com.prempoddar.tune.ui.components.engineSummary
import com.prempoddar.tune.ui.design.EmptyState
import com.prempoddar.tune.ui.design.PillTextField
import com.prempoddar.tune.ui.onboarding.open
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onOpenApp: (String) -> Unit, onOpenSettings: () -> Unit, onFixPermissions: () -> Unit) {
    val engine = AppGraph.engine
    val state by engine.state.collectAsStateWithLifecycle()
    val settings by AppGraph.settings.settings.collectAsStateWithLifecycle(TuneSettings())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var perms by remember { mutableStateOf(PermissionState.read(context)) }
    LifecycleResumeEffect(Unit) {
        perms = PermissionState.read(context)
        engine.shizuku.recheck()
        onPauseOrDispose { }
    }
    var query by rememberSaveable { mutableStateOf("") }

    val playing = state.players.sortedByDescending { it.startedAt }
    val playingPkgs = playing.map { it.pkg }.toSet()
    // Playing apps Tune can only move with the phone's volume, not on their own.
    val shared = playing.map { it.pkg }.filter { state.plan?.modes?.get(it) == ControlMode.SWITCH }
        .let { sw -> if (sw.isNotEmpty() && playing.size >= 2) playing.map { it.pkg } else emptyList() }
    val allSaved = savedApps(state, "")
    val apps = savedApps(state, query).filter { it !in playingPkgs }
    val insets = WindowInsets.safeDrawing.asPaddingValues()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            // Wide screens and landscape keep a comfortable column instead of stretched cards.
            Modifier.widthIn(max = 640.dp).fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = insets.calculateTopPadding() + 8.dp,
                bottom = insets.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                Header(
                    route = state.route,
                    engineLine = engineSummary(state.shizuku, settings.useShizuku, state.perApp),
                    enabled = settings.enabled,
                    onToggle = { scope.launch { AppGraph.settings.update { it.copy(enabled = !it.enabled) } } },
                    onSettings = onOpenSettings,
                )
            }

            // At most one notice at a time, the one that blocks the most.
            item(key = "banner") {
                when {
                    !perms.allRequired -> Banner(
                        "Tune needs access to work",
                        "Some permissions were turned off, so Tune can't follow your apps right now.",
                        "Fix", onFixPermissions, warning = true,
                    )
                    !state.running -> Banner(
                        "Tune isn't running",
                        "Android stopped Tune's service, often to save battery. Turn Tune off and on again in " +
                            "Accessibility, and exclude it from battery optimisation.",
                        "Open Accessibility", { context.open(PermissionState.accessibilityIntent()) }, warning = true,
                    )
                    !settings.enabled -> Banner(
                        "Tune is paused",
                        "Volumes stay wherever they are until you resume.",
                        "Resume", { scope.launch { AppGraph.settings.update { it.copy(enabled = true) } } },
                    )
                    state.ringBlocked -> Banner(
                        "Ring volume can't be changed",
                        "Android needs Do Not Disturb access before Tune can set ring and notification volume.",
                        "Allow", { context.open(PermissionState.dndIntent()) }, warning = true,
                    )
                    // Two apps playing that can only share the phone's one volume: say so, and how to fix it.
                    !state.perApp && shared.size >= 2 && settings.useShizuku ->
                        ShizukuCard(state.shizuku, headline = "${shared.take(2).joinToString(" and ") { AppGraph.appInfo.label(it) }} share one volume")
                    !state.perApp && shared.size >= 2 -> Banner(
                        "${shared.take(2).joinToString(" and ") { AppGraph.appInfo.label(it) }} share one volume",
                        "Turn on Shizuku in Settings to give every app its own volume.",
                        "Settings", onOpenSettings,
                    )
                    settings.useShizuku && state.shizuku == ShizukuBackend.Status.NEEDS_PERMISSION -> ShizukuCard(state.shizuku)
                    else -> Unit
                }
            }

            item(key = "playing-label") { Label("Playing now") }
            if (playing.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        Icons.Rounded.GraphicEq, "Nothing is playing",
                        "Start music, a podcast or a video and it shows up here with its own volume.",
                    )
                }
            } else {
                items(playing, key = { "p-" + it.pkg }) { player ->
                    MixerCard(
                        pkg = player.pkg,
                        level = state.levelsFor(player.pkg).media,
                        status = if (player.pkg == state.plan?.target) "Playing · volume buttons" else "Playing",
                        playing = true,
                        mode = state.plan?.modes?.get(player.pkg),
                        pinned = state.resolved(player.pkg).pinned,
                        onLevel = { engine.setLevel(player.pkg, StreamKind.MEDIA, it) },
                        onClick = { onOpenApp(player.pkg) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            item(key = "phone-label") { Label("Phone volumes") }
            item(key = "phone") { PhoneVolumes() }

            if (allSaved.any { it !in playingPkgs }) {
                item(key = "apps-label") { Label("Your apps") }
                if (allSaved.size > 6) {
                    item(key = "search") {
                        PillTextField(
                            query, { query = it }, "Search apps", leading = Icons.Rounded.Search,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                    }
                }
                items(apps, key = { "a-$it" }) { pkg ->
                    val resolved = state.resolved(pkg)
                    MixerCard(
                        pkg = pkg,
                        level = resolved.levels.media,
                        status = "Saved for ${state.route.name}",
                        playing = false,
                        mode = null,
                        pinned = resolved.pinned,
                        onLevel = { engine.setLevel(pkg, StreamKind.MEDIA, it) },
                        onClick = { onOpenApp(pkg) },
                        modifier = Modifier.animateItem(),
                    )
                }
            } else if (playing.isEmpty()) {
                item(key = "apps-empty") {
                    Text(
                        "Apps show up here once you change their volume. Open an app, set the volume " +
                            "you like, and Tune remembers it for each speaker and headphones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) = Text(
    text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 6.dp, top = 14.dp),
)

/** Every app with a saved level, most recently changed first, matching [query]. */
private fun savedApps(state: EngineState, query: String): List<String> {
    val info = AppGraph.appInfo
    return state.profiles
        .filter { it.pkg != DEFAULT_PKG }
        .groupBy { it.pkg }
        .mapValues { (_, rows) -> rows.maxOf { it.updatedAt } }
        .entries.sortedByDescending { it.value }
        .map { it.key }
        .filter { query.isBlank() || info.label(it).contains(query.trim(), ignoreCase = true) }
}

@Composable
private fun Header(route: RouteInfo, engineLine: String, enabled: Boolean, onToggle: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Tune", style = MaterialTheme.typography.displaySmall)
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(routeIcon(route), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${route.name} · $engineLine", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FilledTonalIconButton(onClick = tap(onToggle)) {
            Icon(if (enabled) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = if (enabled) "Pause Tune" else "Resume Tune")
        }
        Spacer(Modifier.width(4.dp))
        FilledTonalIconButton(onClick = tap(onSettings)) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
    }
}

fun routeIcon(route: RouteInfo): ImageVector = when {
    route.key.startsWith("bt:") -> Icons.Rounded.Bluetooth
    route.key == "usb" -> Icons.Rounded.Usb
    route.key == "wired" -> Icons.Rounded.Headphones
    else -> Icons.Rounded.Speaker
}
