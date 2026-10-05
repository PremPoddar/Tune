package com.prempoddar.tune.service

import android.service.notification.NotificationListenerService

/**
 * Tune reads no notifications. Android only lets an app see other apps' media sessions while
 * it holds notification access, which is why this service exists.
 */
class TuneNotificationListener : NotificationListenerService()
