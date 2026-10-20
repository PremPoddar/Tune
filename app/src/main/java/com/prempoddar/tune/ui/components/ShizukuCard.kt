package com.prempoddar.tune.ui.components

import com.prempoddar.tune.ui.design.tap
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.audio.ShizukuBackend.Status
import com.prempoddar.tune.ui.design.IconTile
import com.prempoddar.tune.ui.design.TonalCard

/** One line for the main screen: how Tune is controlling volumes right now. */
fun engineSummary(status: Status, useShizuku: Boolean, perApp: Boolean): String = when {
    perApp -> "Shizuku connected"
    !useShizuku -> "Standard engine"
    status == Status.NEEDS_PERMISSION -> "Shizuku needs permission"
    status == Status.NOT_RUNNING -> "Shizuku not running"
    else -> "Standard engine"
}

/**
 * Explains Shizuku and offers the single next step: install it, start it, or allow Tune. Shizuku
 * is optional; without it Tune still works with its standard engine.
 */
@Composable
fun ShizukuCard(status: Status, modifier: Modifier = Modifier, headline: String? = null) {
    val context = LocalContext.current
    val ready = status == Status.READY
    TonalCard(modifier.fillMaxWidth(), color = if (ready) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(if (ready) Icons.Rounded.CheckCircle else Icons.Rounded.Bolt)
            Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (headline != null && !ready) headline else when (status) {
                        Status.READY -> "Exact per-app volume is on"
                        Status.NEEDS_PERMISSION -> "Allow Tune in Shizuku"
                        Status.NOT_RUNNING -> "Start Shizuku"
                        Status.NOT_INSTALLED -> "Exact volume for every app"
                        Status.FAILED -> "Shizuku couldn't be used"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (ready) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (headline != null && !ready) "To give them separate volumes, Tune needs the free Shizuku app. " + when (status) {
                        Status.NOT_INSTALLED -> "Install it, open it and tap Start (with Wireless debugging, no computer needed), then come back and allow Tune."
                        Status.NOT_RUNNING -> "It's installed: open it and tap Start, then come back."
                        Status.NEEDS_PERMISSION -> "It's running: tap Allow."
                        else -> "It couldn't be used on this phone."
                    } else when (status) {
                        Status.READY -> "Tune sets each app's own player through Shizuku, so every app keeps its level, YouTube and browsers included."
                        Status.NEEDS_PERMISSION -> "Shizuku is running. Allow Tune once and every app gets its own exact volume."
                        Status.NOT_RUNNING -> "Shizuku is installed but not running. Start it from the Shizuku app, then come back."
                        Status.NOT_INSTALLED -> "Optional. With the free Shizuku app, Tune can set any app's volume exactly. Without it Tune still works for most apps."
                        Status.FAILED -> "This Android version or Shizuku build isn't supported. Tune keeps using its standard engine."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ready) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val action: Pair<String, () -> Unit>? = when (status) {
            Status.NEEDS_PERMISSION -> "Allow" to { AppGraph.engine.shizuku.requestPermission() }
            Status.NOT_RUNNING -> "Open Shizuku" to { context.openShizuku() }
            Status.NOT_INSTALLED -> "Get Shizuku" to { context.openStore() }
            else -> null
        }
        if (action != null) {
            Row(Modifier.fillMaxWidth().padding(start = 54.dp, top = 12.dp)) {
                FilledTonalButton(onClick = tap(action.second)) { Text(action.first) }
            }
        }
    }
}

private fun Context.openShizuku() {
    val intent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api") ?: return
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun Context.openStore() {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(market) }.onFailure { openUrl("https://shizuku.rikka.app/download/") }
}

private fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
