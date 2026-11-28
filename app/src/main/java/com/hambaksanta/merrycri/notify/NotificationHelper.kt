// notify/NotificationHelper.kt
package com.hambaksanta.merrycri.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.MainActivity

/** 로컬 알림 채널 생성과 표시. 담당: B */
object NotificationHelper {
    private const val CHANNEL_ID = "gift_notifications"
    private const val GIFT_OPEN_NOTIFICATION_ID = 1225
    private const val NEW_LETTER_NOTIFICATION_ID = 1226

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

    fun showGiftOpenNotification(context: Context) = showNotification(
        context,
        GIFT_OPEN_NOTIFICATION_ID,
        R.string.notification_gift_title,
        R.string.notification_gift_body,
    )

    fun showNewLetterNotification(context: Context) = showNotification(
        context,
        NEW_LETTER_NOTIFICATION_ID,
        R.string.notification_new_letter_title,
        R.string.notification_new_letter_body,
    )

    private fun showNotification(
        context: Context,
        notificationId: Int,
        @StringRes title: Int,
        @StringRes body: Int,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val openApp = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
