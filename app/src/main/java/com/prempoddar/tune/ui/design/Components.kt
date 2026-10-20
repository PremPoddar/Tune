@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.prempoddar.tune.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A screen with a big headline that shrinks into the top bar as content scrolls under it.
 * [content] gets the padding to apply so nothing sits under the bars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    // How far the title has folded away is kept in pixels, so it starts afresh when the screen's
    // density changes (Compact, text size): an old amount can be more than the new bar's height.
    val state = androidx.compose.runtime.key(androidx.compose.ui.platform.LocalDensity.current.density) { rememberTopAppBarState() }
    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state)
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(behavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { if (onBack != null) IconButton(onClick = tap(onBack)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = actions,
                scrollBehavior = behavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = floatingActionButton,
        bottomBar = bottomBar,
        content = content,
    )
}

/** A small coloured heading above a group. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

/**
 * Rows grouped into one rounded card, with thin gaps between them, as in Android's own settings.
 */
@Composable
fun SettingsGroup(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) SectionLabel(title)
        Column(
            Modifier.padding(horizontal = 16.dp).clip(MaterialTheme.shapes.large),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

/** An icon on a soft scalloped badge, the way settings rows show them. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, container: Color = MaterialTheme.colorScheme.primaryContainer, tint: Color = MaterialTheme.colorScheme.onPrimaryContainer, size: Dp = 40.dp) {
    Box(modifier.size(size).clip(CookieShape).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.52f))
    }
}

/**
 * A circle with gently scalloped edges, like a cookie or a flower: Material 3 Expressive's
 * friendliest shape. [bumps] around the edge, each [depth] of the radius deep.
 */
class Scallop(private val bumps: Int = 9, private val depth: Float = 0.07f) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path()
        val cx = size.width / 2; val cy = size.height / 2
        val r = minOf(cx, cy) / (1 + depth)
        val steps = bumps * 16
        for (i in 0..steps) {
            val a = (i.toDouble() / steps) * 2 * Math.PI
            val rr = r * (1 + depth * kotlin.math.cos(bumps * a)).toFloat()
            val x = cx + rr * kotlin.math.cos(a - Math.PI / 2).toFloat()
            val y = cy + rr * kotlin.math.sin(a - Math.PI / 2).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

val CookieShape = Scallop()

/**
 * A big two-line toggle, two to a row: filled with the accent when on. For the handful of
 * settings people change most, at the top of a settings screen.
 */
@Composable
fun QuickToggle(title: String, icon: ImageVector, checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val s = MaterialTheme.colorScheme
    val container by androidx.compose.animation.animateColorAsState(if (checked) s.primary else s.surfaceContainerHigh, label = "toggle")
    val content by androidx.compose.animation.animateColorAsState(if (checked) s.onPrimary else s.onSurface, label = "toggleText")
    Surface(
        onClick = { Haptics.toggle(!checked); onChange(!checked) }, enabled = enabled,
        shape = RoundedCornerShape(if (checked) 32.dp else 22.dp), color = container, contentColor = content,
        modifier = modifier.heightIn(min = 76.dp).then(if (enabled) Modifier else Modifier.alpha(0.45f)),
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, Modifier.size(22.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                Text(if (checked) "On" else "Off", style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.75f))
            }
        }
    }
}

/**
 * A large tonal card that leads somewhere, with a scalloped icon badge and a soft shape in the
 * corner: the "everything else" entries of a settings screen.
 */
@Composable
fun FeatureCard(
    title: String, summary: String?, icon: ImageVector, modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer, content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    footer: (@Composable () -> Unit)? = null, onClick: () -> Unit,
) {
    Surface(onClick = tap(onClick), shape = MaterialTheme.shapes.extraLarge, color = container, contentColor = content, modifier = modifier) {
        Box {
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 0.dp).size(120.dp).alpha(0.18f).clip(Scallop(7, 0.12f)).background(content))
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                IconTile(icon, container = content.copy(alpha = 0.12f), tint = content, size = 44.dp)
                Spacer(Modifier.size(6.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall)
                if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium, color = content.copy(alpha = 0.8f))
                footer?.invoke()
            }
        }
    }
}

/** One row in a [SettingsGroup]. Long titles and summaries wrap instead of being cut off. */
@Composable
fun SettingsRow(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = tap(onClick)) else Modifier)
            .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) IconTile(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
        trailing?.invoke()
    }
}

@Composable
fun SwitchRow(title: String, summary: String? = null, icon: ImageVector? = null, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
            .then(if (enabled) Modifier.clickable { Haptics.toggle(!checked); onChange(!checked) } else Modifier)
            .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val alpha = if (enabled) 1f else 0.45f
        if (icon != null) IconTile(icon)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
            if (summary != null) Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
        }
        IconSwitch(checked, enabled, toggled(onChange))
    }
}

/** Filled, rounded text field with no underline; a clear button appears once there's text. */
@Composable
fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    leading: ImageVector? = null,
    clearable: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
) {
    TextField(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(label) },
        supportingText = supporting?.let { { Text(it) } },
        isError = isError, singleLine = singleLine, minLines = minLines,
        leadingIcon = leading?.let { { Icon(it, null) } },
        trailingIcon = trailing ?: if (clearable && value.isNotEmpty() && enabled) {
            { IconButton(onClick = { onValueChange("") }) { Icon(Icons.Rounded.Cancel, "Clear") } }
        } else null,
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, visualTransformation = visualTransformation,
        shape = RoundedCornerShape(20.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
            errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent, errorIndicatorColor = Color.Transparent,
        ),
    )
}

private val BigButtonHeight = 56.dp

/** The main action on a screen: tall, rounded, impossible to miss. */
@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(tap(onClick), modifier.defaultMinSize(minHeight = BigButtonHeight).fillMaxWidth(), enabled = enabled, shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)) {
        ButtonContent(text, icon)
    }
}

@Composable
fun TonalBigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    FilledTonalButton(tap(onClick), modifier.defaultMinSize(minHeight = BigButtonHeight).fillMaxWidth(), enabled = enabled, shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)) {
        ButtonContent(text, icon)
    }
}

@Composable
fun OutlinedBigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    OutlinedButton(tap(onClick), modifier.defaultMinSize(minHeight = BigButtonHeight).fillMaxWidth(), enabled = enabled, shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)) {
        ButtonContent(text, icon)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) { Icon(icon, null, Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.width(ButtonDefaults.IconSpacing)) }
    Text(text, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

/** Pick one of a few options; wraps onto more lines on narrow screens. */
@Composable
fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Surface(
                onClick = { if (!on) Haptics.select(); onSelect(value) }, shape = RoundedCornerShape(14.dp),
                color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                contentColor = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp))
            }
        }
    }
}

/** Shown where a list would be when there's nothing in it. */
@Composable
fun EmptyState(icon: ImageVector, title: String, body: String? = null, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IconTile(icon, size = 72.dp, container = MaterialTheme.colorScheme.secondaryContainer, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.widthIn(max = 360.dp))
        action?.invoke()
    }
}

/** A raised, rounded card in the page's tonal colours. */
@Composable
fun TonalCard(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.surfaceContainer, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    if (onClick != null) Surface(onClick = tap(onClick), modifier = modifier, shape = shape, color = color) { Column(Modifier.padding(20.dp), content = content) }
    else Surface(modifier = modifier, shape = shape, color = color) { Column(Modifier.padding(20.dp), content = content) }
}

/** Round colour swatch; [selected] draws a ring around it. */
@Composable
fun Swatch(color: Color, selected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent)
            .clickable(onClickLabel = label, onClick = { if (!selected) Haptics.select(); onClick() })
            .padding(if (selected) 3.dp else 0.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
            .padding(if (selected) 3.dp else 0.dp).clip(CircleShape).background(color),
    )
}

/** A switch whose knob shows a tick when on and a cross when off, so it reads at a glance. */
@Composable
fun IconSwitch(checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChange, enabled = enabled,
        thumbContent = {
            Icon(
                if (checked) Icons.Rounded.Check else Icons.Rounded.Close, null,
                Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize),
            )
        },
    )
}
