// notify/NotificationHelper.kt
package com.hambaksanta.merrycri.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hambaksanta.merrycri.R

/** 로컬 알림 채널 생성과 표시. 담당: B */
object NotificationHelper {
    private const val CHANNEL_ID = "gift_notifications"
    private const val GIFT_OPEN_NOTIFICATION_ID = 1225

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_gifts),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    fun showGiftOpenNotification(context: Context) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_gift_title))
            .setContentText(context.getString(R.string.notification_gift_body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(GIFT_OPEN_NOTIFICATION_ID, notification)
    }
}
