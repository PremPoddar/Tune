package com.prempoddar.tune.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.prempoddar.tune.core.DEFAULT_PKG
import java.util.concurrent.ConcurrentHashMap

/** Labels, icons and icon colours of installed apps, cached. */
class AppInfoCache(context: Context) {

    private val pm = context.packageManager
    private val labels = ConcurrentHashMap<String, String>()
    private val seeds = ConcurrentHashMap<String, Int>()
    private val icons = object : LruCache<String, Bitmap>(ICON_CACHE_SIZE) {}

    fun label(pkg: String): String {
        if (pkg == DEFAULT_PKG) return "Default"
        return labels.getOrPut(pkg) {
            try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                pkg
            }
        }
    }

    fun icon(pkg: String): Bitmap? {
        if (pkg == DEFAULT_PKG) return null
        icons.get(pkg)?.let { return it }
        return try {
            pm.getApplicationIcon(pkg).toBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888)
                .also { icons.put(pkg, it) }
        } catch (e: Exception) {
            // Missing apps, and the odd icon that can't be drawn, just show no icon.
            null
        }
    }

    /** The most characteristic colour of the app's icon, or null without an icon. */
    fun seedColor(pkg: String): Int? {
        seeds[pkg]?.let { return it }
        val bitmap = icon(pkg) ?: return null
        val palette = Palette.from(bitmap).generate()
        val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: return null
        return swatch.rgb.also { seeds[pkg] = it }
    }

    fun isInstalled(pkg: String): Boolean = try {
        pm.getApplicationInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        const val ICON_PX = 144
        const val ICON_CACHE_SIZE = 64
    }
}
