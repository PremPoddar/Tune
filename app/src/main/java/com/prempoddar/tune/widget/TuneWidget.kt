package com.prempoddar.tune.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.prempoddar.tune.AppGraph
import com.prempoddar.tune.MainActivity
import com.prempoddar.tune.R
import com.prempoddar.tune.ui.design.seedScheme
import com.prempoddar.tune.ui.theme.TUNE_BRAND

/** Home screen widget: the app the volume buttons move right now, with − and + buttons. */
class TuneWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(colors = ColorProviders(light = seedScheme(TUNE_BRAND, dark = false, pureBlack = false), dark = seedScheme(TUNE_BRAND, dark = true, pureBlack = false))) {
                Content()
            }
        }
    }

    @Composable
    private fun Content() {
        val state by AppGraph.engine.state.collectAsState()
        val info = AppGraph.appInfo
        val target = state.target
        val level = state.levelsFor(target).media
        Row(
            GlanceModifier.fillMaxSize().cornerRadius(28.dp).background(GlanceTheme.colors.surface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = info.icon(target)
            Image(
                provider = if (icon != null) ImageProvider(icon) else ImageProvider(R.drawable.ic_tile),
                contentDescription = null,
                modifier = GlanceModifier.size(36.dp).clickable(actionStartActivity<MainActivity>()),
            )
            Spacer(GlanceModifier.width(12.dp))
            Column(GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>())) {
                Text(
                    info.label(target), maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                )
                Text(
                    when {
                        !state.running -> "Tap to turn on Tune"
                        !state.enabled -> "Paused"
                        else -> "$level%"
                    },
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                )
            }
            if (state.running && state.enabled) {
                StepButton("−", up = false)
                Spacer(GlanceModifier.width(8.dp))
                StepButton("+", up = true)
            }
        }
    }

    @Composable
    private fun StepButton(label: String, up: Boolean) {
        Box(
            GlanceModifier.size(44.dp).cornerRadius(22.dp).background(GlanceTheme.colors.primaryContainer)
                .clickable(actionRunCallback<StepAction>(actionParametersOf(UpKey to up))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 20.sp),
            )
        }
    }

    companion object {
        val UpKey = ActionParameters.Key<Boolean>("up")

        suspend fun refresh(context: Context) = TuneWidget().updateAll(context)
    }
}

class StepAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        AppGraph.engine.step(parameters[TuneWidget.UpKey] ?: true)
    }
}

class TuneWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TuneWidget()
}
