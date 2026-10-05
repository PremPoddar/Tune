package com.prempoddar.tune.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.audio.EngineState
import com.prempoddar.tune.core.DEFAULT_PKG
import com.prempoddar.tune.core.RouteInfo
import com.prempoddar.tune.core.StreamKind
import com.prempoddar.tune.data.TuneSettings
import com.prempoddar.tune.service.PermissionState
import com.prempoddar.tune.ui.components.AppIcon
import com.prempoddar.tune.ui.components.Banner
import com.prempoddar.tune.ui.components.LevelCard
import com.prempoddar.tune.ui.components.ModeBadge
import com.prempoddar.tune.ui.components.SectionLabel
import com.prempoddar.tune.ui.components.appLabel
import com.prempoddar.tune.ui.onboarding.open
import com.prempoddar.tune.ui.theme.LevelNumberSmall
import com.prempoddar.tune.ui.theme.rememberAppTint
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
        onPauseOrDispose { }
    }
    var query by rememberSaveable { mutableStateOf("") }

    val playing = state.players.sortedByDescending { it.startedAt }
    val apps = savedApps(state, query)

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 8.dp,
            bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            Header(
                route = state.route,
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
                else -> Unit
            }
        }

        item(key = "playing-label") { SectionLabel("Playing now") }
        if (playing.isEmpty()) {
            item(key = "empty") { EmptyPlaying() }
        } else {
            items(playing, key = { "p-" + it.pkg }) { player ->
                LevelCard(
                    pkg = player.pkg,
                    level = state.levelsFor(player.pkg).media,
                    onLevel = { engine.setLevel(player.pkg, StreamKind.MEDIA, it) },
                    onClick = { onOpenApp(player.pkg) },
                    mode = state.plan?.modes?.get(player.pkg),
                    caption = if (player.pkg == state.plan?.target) "Volume buttons" else null,
                    modifier = Modifier.animateItem(),
                )
            }
        }

        item(key = "apps-label") {
            Row(verticalAlignment = Alignment.Bottom) {
                SectionLabel("Your apps", Modifier.weight(1f))
            }
        }
        if (savedApps(state, "").size > 6) {
            item(key = "search") { SearchField(query) { query = it } }
        }
        items(apps, key = { "a-$it" }) { pkg ->
            AppRow(pkg, state, onClick = { onOpenApp(pkg) }, modifier = Modifier.animateItem())
        }
        if (apps.isEmpty() && query.isBlank()) {
            item(key = "apps-empty") {
                Text(
                    "Apps show up here once you change their volume. Open an app, set the volume " +
                        "you like, and Tune remembers it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

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
private fun Header(route: RouteInfo, enabled: Boolean, onToggle: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Tune", style = MaterialTheme.typography.displaySmall)
            Row(
                Modifier.padding(top = 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(routeIcon(route), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(route.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onToggle) {
            Icon(
                if (enabled) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                contentDescription = if (enabled) "Pause Tune" else "Resume Tune",
                modifier = Modifier.size(30.dp),
            )
        }
        IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
    }
}

fun routeIcon(route: RouteInfo): ImageVector = when {
    route.key.startsWith("bt:") -> Icons.Rounded.Bluetooth
    route.key == "usb" -> Icons.Rounded.Usb
    route.key == "wired" -> Icons.Rounded.Headphones
    else -> Icons.Rounded.Speaker
}

@Composable
private fun EmptyPlaying() {
    Column(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Text("Nothing is playing", style = MaterialTheme.typography.titleMedium)
        Text(
            "Start music, a podcast or a video and it shows up here with its own volume.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQuery,
        placeholder = { Text("Search apps") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = MaterialTheme.shapes.extraLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.surface,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AppRow(pkg: String, state: EngineState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val resolved = state.resolved(pkg)
    val level = resolved.levels.media
    val tint = rememberAppTint(pkg)
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(pkg, size = 36.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    appLabel(pkg), style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                if (resolved.pinned) {
                    Icon(
                        Icons.Rounded.PushPin, contentDescription = "Pinned",
                        modifier = Modifier.padding(start = 6.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                state.plan?.modes?.get(pkg)?.let { ModeBadge(it, Modifier.padding(start = 6.dp)) }
            }
            // A thin echo of the level card: the same pastel fill, at a glance.
            Box(
                Modifier.padding(top = 8.dp).fillMaxWidth().height(6.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(level / 100f).clip(CircleShape).background(tint))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text("$level%", style = LevelNumberSmall)
    }
}
