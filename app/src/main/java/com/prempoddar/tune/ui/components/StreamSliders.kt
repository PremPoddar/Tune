package com.prempoddar.tune.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RingVolume
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.prempoddar.tune.core.Levels
import com.prempoddar.tune.core.StreamKind

/** The four stream sliders for one profile. */
@Composable
fun StreamSliders(levels: Levels, tint: Color, onChange: (StreamKind, Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StreamSlider("Media", Icons.Rounded.MusicNote, levels.media, tint, { onChange(StreamKind.MEDIA, it) })
        StreamSlider("Ring", Icons.Rounded.RingVolume, levels.ring, tint, { onChange(StreamKind.RING, it) })
        StreamSlider("Notifications", Icons.Rounded.Notifications, levels.notification, tint, { onChange(StreamKind.NOTIFICATION, it) })
        StreamSlider("Alarm", Icons.Rounded.Alarm, levels.alarm, tint, { onChange(StreamKind.ALARM, it) })
    }
}
