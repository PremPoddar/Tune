package com.prempoddar.tune.ui.onboarding

import com.prempoddar.tune.ui.design.tap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prempoddar.tune.ui.design.BigButton
import com.prempoddar.tune.ui.components.ShizukuCard
import com.prempoddar.tune.audio.ShizukuBackend
import com.prempoddar.tune.AppGraph
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.prempoddar.tune.service.PermissionState
import com.prempoddar.tune.service.Permissions

private enum class Step { WELCOME, ACCESSIBILITY, NOTIFICATIONS, DND, MORE }

/** Walks through the three permissions Tune needs, then offers the optional adb step. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var perms by remember { mutableStateOf(PermissionState.read(context)) }
    var started by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        perms = PermissionState.read(context)
        onPauseOrDispose { }
    }
    val step = when {
        !started -> Step.WELCOME
        !perms.accessibility -> Step.ACCESSIBILITY
        !perms.notifications -> Step.NOTIFICATIONS
        !perms.dnd -> Step.DND
        else -> Step.MORE
    }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding(),
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
            },
            label = "step",
        ) { s ->
            when (s) {
                Step.WELCOME -> StepPage(
                    icon = Icons.Rounded.Tune,
                    title = "Every app, its own volume",
                    body = "Tune remembers how loud you like each app and switches as you move between them. " +
                        "Music and podcasts can even play together at different levels.\n\n" +
                        "It needs three permissions. Each one is explained before you grant it.",
                    primary = "Get started",
                    onPrimary = { started = true },
                    progress = null,
                )
                Step.ACCESSIBILITY -> StepPage(
                    icon = Icons.Rounded.Accessibility,
                    title = "See which app is open",
                    body = "Turn on Tune under Accessibility, in Downloaded apps or Installed services. " +
                        "Tune uses it only to know which app is on screen and to show its volume panel " +
                        "when you press the volume buttons. It reads nothing on your screen.",
                    primary = "Open Accessibility settings",
                    onPrimary = { context.open(PermissionState.accessibilityIntent()) },
                    progress = 1,
                    restrictedHint = true,
                )
                Step.NOTIFICATIONS -> StepPage(
                    icon = Icons.Rounded.MusicNote,
                    title = "Find music playing in the background",
                    body = "Allow notification access so Tune can tell when Spotify or a podcast is " +
                        "playing with the screen off. Tune doesn't read or store your notifications.",
                    primary = "Allow notification access",
                    onPrimary = { context.open(PermissionState.notificationIntent(context)) },
                    progress = 2,
                    restrictedHint = true,
                )
                Step.DND -> StepPage(
                    icon = Icons.Rounded.DoNotDisturbOn,
                    title = "Change ring and notification volume",
                    body = "Android only lets apps change the ring and notification volume with " +
                        "Do Not Disturb access. Tune never turns Do Not Disturb on or off.",
                    primary = "Allow Do Not Disturb access",
                    onPrimary = { context.open(PermissionState.dndIntent()) },
                    progress = 3,
                )
                Step.MORE -> MoreAppsPage(perms, onDone)
            }
        }
    }
}

@Composable
private fun StepPage(
    icon: ImageVector,
    title: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
    progress: Int?,
    restrictedHint: Boolean = false,
    extra: @Composable () -> Unit = {},
    secondary: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        if (progress != null) Dots(progress) else Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(56.dp))
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(32.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(14.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        extra()
        if (restrictedHint && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(start = 16.dp, top = 14.dp, end = 4.dp),
            ) {
                Text("Is the switch greyed out?", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Android blocks this setting for apps installed outside the Play Store. " +
                        "Open App info, tap ⋮ in the corner, choose Allow restricted settings, then try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, end = 12.dp),
                )
                TextButton(
                    onClick = tap { context.open(PermissionState.appInfoIntent(context)) },
                    modifier = Modifier.align(Alignment.End),
                ) { Text("Open App info") }
            }
        }
        Spacer(Modifier.weight(1f).height(32.dp))
        BigButton(primary, onPrimary)
        secondary?.let {
            Spacer(Modifier.height(8.dp))
            it()
        }
    }
}

@Composable
private fun MoreAppsPage(perms: Permissions, onDone: () -> Unit) {
    val shizuku = AppGraph.engine.shizuku
    val status by shizuku.status.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        shizuku.start()
        shizuku.recheck()
        onPauseOrDispose { }
    }
    StepPage(
        icon = Icons.Rounded.GraphicEq,
        title = "Exact volume for every app",
        body = "Optional. Music players that share their audio already get their own volume. Apps like " +
            "YouTube and browsers don't, so Tune switches the phone's volume for them.\n\nWith the free " +
            "Shizuku app, Tune can set every app exactly. No Shizuku? One adb command from a computer " +
            "lets Tune mix most of those apps too.",
        primary = if (status == ShizukuBackend.Status.READY || perms.dump) "Done" else "Skip for now",
        onPrimary = onDone,
        progress = null,
        extra = {
            Spacer(Modifier.height(20.dp))
            ShizukuCard(status)
            if (status != ShizukuBackend.Status.READY) {
                Spacer(Modifier.height(12.dp))
                AdbCommandCard(granted = perms.dump)
            }
        },
    )
}

/** The adb command with a copy button; also used in Settings. */
@Composable
fun AdbCommandCard(granted: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
    ) {
        Text(
            if (granted) "Granted. Tune can mix every app it finds." else "Run on your computer with the phone connected:",
            style = MaterialTheme.typography.labelMedium,
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    PermissionState.ADB_COMMAND,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f).padding(top = 6.dp),
                )
                IconButton(onClick = tap {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("adb command", PermissionState.ADB_COMMAND))
                }) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy command") }
            }
        }
    }
}

@Composable
private fun Dots(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..3).forEach { i ->
            Box(
                Modifier.height(6.dp).width(if (i == current) 24.dp else 6.dp).clip(CircleShape)
                    .background(if (i <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
            )
        }
    }
}

fun Context.open(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
