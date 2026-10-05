package com.prempoddar.tune.service

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

data class Permissions(
    val accessibility: Boolean,
    val notifications: Boolean,
    val dnd: Boolean,
    val dump: Boolean,
) {
    val allRequired: Boolean get() = accessibility && notifications && dnd
}

/** Reads Tune's permissions and opens the settings page for each one. */
object PermissionState {

    fun read(context: Context): Permissions = Permissions(
        accessibility = accessibilityEnabled(context),
        notifications = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context),
        dnd = context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted,
        dump = context.checkSelfPermission(Manifest.permission.DUMP) == PackageManager.PERMISSION_GRANTED,
    )

    private fun accessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val me = ComponentName(context, TuneAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun accessibilityIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun notificationIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(context, TuneNotificationListener::class.java).flattenToString(),
            )
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }

    fun dndIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    /** App info, where Android 13+ hides "Allow restricted settings" for sideloaded apps. */
    fun appInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    fun batteryIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    const val ADB_COMMAND = "adb shell pm grant com.prempoddar.tune android.permission.DUMP"
}
