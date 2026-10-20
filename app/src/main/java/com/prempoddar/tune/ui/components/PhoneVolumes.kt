package com.prempoddar.tune.ui.components

import com.prempoddar.tune.ui.design.sliding
import com.prempoddar.tune.ui.design.tap
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.prempoddar.tune.ui.design.TonalCard
import com.prempoddar.tune.ui.theme.LevelNumberSmall
import kotlin.math.roundToInt

private class PhoneStream(val label: String, val icon: ImageVector, val stream: Int)

private val phoneStreams = listOf(
    PhoneStream("Media", Icons.Rounded.MusicNote, AudioManager.STREAM_MUSIC),
    PhoneStream("Ring", Icons.Rounded.RingVolume, AudioManager.STREAM_RING),
    PhoneStream("Alarm", Icons.Rounded.Alarm, AudioManager.STREAM_ALARM),
    PhoneStream("Calls", Icons.Rounded.Call, AudioManager.STREAM_VOICE_CALL),
)

/**
 * The phone's own volumes, set directly: pick a stream, drag. Changes made here are treated like
 * the volume buttons, so Tune remembers them for the app you're using.
 */
@Composable
fun PhoneVolumes(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val am = remember { context.getSystemService(AudioManager::class.java) }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val stream = phoneStreams[selected]
    val max = remember(stream) { am.getStreamMaxVolume(stream.stream).coerceAtLeast(1) }
    val min = remember(stream) { am.getStreamMinVolume(stream.stream) }
    var index by remember(stream) { mutableIntStateOf(am.getStreamVolume(stream.stream)) }
    var dragging by remember(stream) { mutableStateOf<Float?>(null) }

    // Follow changes made with the volume buttons or anywhere else.
    DisposableEffect(stream) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (dragging == null) index = am.getStreamVolume(stream.stream)
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    TonalCard(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                phoneStreams.forEachIndexed { i, s ->
                    if (i > 0) Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = i == selected, onClick = tap { selected = i },
                        label = { Text(s.label, maxLines = 1) },
                        leadingIcon = if (i == selected) { { Icon(s.icon, null, Modifier.padding(0.dp)) } } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                }
            }
        }
        val shownPct = ((dragging ?: index.toFloat()) / max * 100).roundToInt()
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(stream.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = dragging ?: index.toFloat(),
                onValueChange = sliding(0f..1f) { v ->
                    dragging = v
                    val target = v.roundToInt().coerceIn(min, max)
                    if (target != index) {
                        try {
                            am.setStreamVolume(stream.stream, target, 0)
                            index = target
                        } catch (e: SecurityException) {
                            dragging = null
                            Toast.makeText(context, "Allow Do Not Disturb access in Settings to change this", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onValueChangeFinished = { dragging = null; index = am.getStreamVolume(stream.stream) },
                valueRange = 0f..max.toFloat(),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp).semantics { contentDescription = "${stream.label} volume" },
            )
            Text("$shownPct%", style = LevelNumberSmall)
        }
    }
}
