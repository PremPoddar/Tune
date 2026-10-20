package com.prempoddar.tune.ui.components

import com.prempoddar.tune.ui.design.tap
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prempoddar.tune.core.RouteInfo
import com.prempoddar.tune.ui.home.routeIcon

/** One chip per output; [current] is the output playing now. */
@Composable
fun RouteChips(
    routes: List<RouteInfo>,
    selected: String,
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        routes.forEach { r ->
            FilterChip(
                selected = r.key == selected,
                onClick = tap { onSelect(r.key) },
                label = { Text(if (r.key == current) "${r.name} · now" else r.name) },
                leadingIcon = { Icon(routeIcon(r), contentDescription = null) },
            )
        }
    }
}
