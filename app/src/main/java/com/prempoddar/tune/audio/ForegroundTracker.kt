package com.prempoddar.tune.audio

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app the user is looking at. Tune itself, the system UI and the keyboard are skipped, so
 * opening Tune or pulling down the shade keeps the app underneath as the foreground app.
 */
class ForegroundTracker(private val context: Context) {

    private val _foreground = MutableStateFlow<String?>(null)
    val foreground: StateFlow<String?> = _foreground.asStateFlow()

    fun onWindow(pkg: String?) {
        if (pkg.isNullOrBlank() || pkg in ignored()) return
        _foreground.value = pkg
    }

    private fun ignored(): Set<String> {
        val ime = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.substringBefore('/')
        return setOfNotNull(context.packageName, "com.android.systemui", "android", ime)
    }
}
