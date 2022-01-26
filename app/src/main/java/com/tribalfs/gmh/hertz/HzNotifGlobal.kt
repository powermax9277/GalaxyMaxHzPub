package com.tribalfs.gmh.hertz

import android.app.Notification
import android.app.NotificationChannel

object HzNotifGlobal {
    internal const val CHANNEL_ID_HZ = "GMH"
    internal const val NOTIFICATION_ID_HZ = 5
    internal var hznotificationChannel: NotificationChannel? = null
    internal var hznotificationBuilder: Notification.Builder? = null
}