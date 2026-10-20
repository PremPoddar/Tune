@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.prempoddar.tune.ui.design

import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** A library or font the app is built with, and its licence. */
data class Credit(val name: String, val licence: String)

/** Who makes the apps, and where to find them. The links are placeholders until those pages exist. */
object Developer {
    const val NAME = "Prem Poddar"
    const val GITHUB = "https://github.com/"
    const val SITE = "https://example.com/"
}

/** The launcher icon drawn from its two layers (adaptive icons and shape layers can't be painted directly). */
@Composable
fun LayeredIcon(background: Int, foreground: Int) {
    val context = LocalContext.current
    val layers = androidx.compose.runtime.remember(background, foreground) {
        listOfNotNull(androidx.core.content.ContextCompat.getDrawable(context, background), androidx.core.content.ContextCompat.getDrawable(context, foreground))
    }
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        drawIntoCanvas { c ->
            layers.forEach { d ->
                d.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                d.draw(c.nativeCanvas)
            }
        }
    }
}

/** Name, version, what the app is, who built it, optional notes (like privacy), and what it's built with. */
@Composable
fun AboutScreen(
    appName: String, tagline: String, icon: @Composable () -> Unit, credits: List<Credit>, onBack: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
    /** Short titled notes shown in their own group, such as how the app treats your data. */
    notes: List<Pair<String, String>> = emptyList(),
    notesTitle: String = "Privacy",
) {
    val context = LocalContext.current
    val version = runCatching {
        @Suppress("DEPRECATION") context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "—"
    fun open(url: String) = runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
    ExpressiveScaffold("About", onBack = onBack) { pad ->
        LazyColumn(contentPadding = PaddingValues(top = pad.calculateTopPadding(), bottom = pad.calculateBottomPadding() + 32.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.foundation.layout.Box(Modifier.size(96.dp).clip(RoundedCornerShape(30.dp))) { icon() }
                    Spacer(Modifier.height(16.dp))
                    Text(appName, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(tagline, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp))
                    Spacer(Modifier.height(10.dp))
                    AssistChip(onClick = {}, label = { Text("Version $version") })
                }
            }
            item {
                TonalCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Built with love by", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(Developer.NAME, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            "Made to be fast, private and a pleasure to use. Thank you for using it.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
            item {
                SettingsGroup("Links") {
                    SettingsRow("Source code", "GitHub", androidx.compose.material.icons.Icons.Rounded.Code, onClick = { open(Developer.GITHUB) })
                    SettingsRow("Developer's site", Developer.SITE.removePrefix("https://").trimEnd('/'), androidx.compose.material.icons.Icons.Rounded.Language, onClick = { open(Developer.SITE) })
                }
            }
            if (notes.isNotEmpty()) item {
                SettingsGroup(notesTitle) {
                    notes.forEach { (title, body) -> SettingsRow(title, body, androidx.compose.material.icons.Icons.Rounded.Shield) }
                }
            }
            if (extra != null) item { extra() }
            item {
                SettingsGroup("Built with") {
                    credits.forEach { SettingsRow(it.name, it.licence) }
                }
            }
        }
    }
}
