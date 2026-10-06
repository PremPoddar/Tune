package com.prempoddar.tune.ui.panel

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.prempoddar.tune.MainActivity

/**
 * Tune's replacement for the system volume popup. Lives in an accessibility overlay window, so it
 * needs no draw-over-apps permission. The window exists only while the panel is on screen.
 */
class PanelWindow(private val context: Context) {

    private val wm = context.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    private val visible = mutableStateOf(false)
    private val expanded = mutableStateOf(false)
    private val hide = Runnable { visible.value = false }

    fun show() {
        attach()
        visible.value = true
        poke()
    }

    /** Restarts the auto-hide timer; called on every interaction. */
    private fun poke() {
        handler.removeCallbacks(hide)
        handler.postDelayed(hide, if (expanded.value) EXPANDED_MS else COLLAPSED_MS)
    }

    private fun attach() {
        if (view != null) return
        val o = OverlayOwner().also { it.create() }
        val v = ComposeView(context).apply {
            setViewTreeLifecycleOwner(o)
            setViewTreeSavedStateRegistryOwner(o)
            setViewTreeViewModelStoreOwner(o)
            setContent {
                VolumePanel(
                    visible = visible.value,
                    expanded = expanded.value,
                    onExpand = {
                        expanded.value = !expanded.value
                        poke()
                    },
                    onInteract = ::poke,
                    onOpenTune = {
                        visible.value = false
                        context.startActivity(
                            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    onHidden = { handler.post(::detach) },
                    onDismiss = { visible.value = false },
                )
            }
        }
        // A fixed full-screen window: the panel animates inside it. Sizing the window to the panel made
        // every frame of the expand animation resize the window, which crawled.
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP }
        wm.addView(v, params)
        view = v
        owner = o
    }

    private fun detach() {
        if (visible.value) return
        expanded.value = false
        view?.let { runCatching { wm.removeView(it) } }
        owner?.destroy()
        view = null
        owner = null
    }

    fun destroy() {
        handler.removeCallbacks(hide)
        visible.value = false
        detach()
    }

    private companion object {
        const val COLLAPSED_MS = 2_500L
        const val EXPANDED_MS = 5_000L
    }
}
