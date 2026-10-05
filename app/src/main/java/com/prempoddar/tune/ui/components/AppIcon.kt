package com.prempoddar.tune.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prempoddar.tune.AppGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An app's launcher icon; the Default profile gets Tune's own mark. */
@Composable
fun AppIcon(pkg: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, pkg) {
        value = withContext(Dispatchers.IO) { AppGraph.appInfo.icon(pkg)?.asImageBitmap() }
    }
    val image = bitmap
    if (image != null) {
        Image(image, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(
            modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Tune, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@Composable
fun appLabel(pkg: String): String {
    val label by produceState(initialValue = "", pkg) {
        value = withContext(Dispatchers.IO) { AppGraph.appInfo.label(pkg) }
    }
    return label
}
