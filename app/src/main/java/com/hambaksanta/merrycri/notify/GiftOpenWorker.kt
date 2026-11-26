// notify/GiftOpenWorker.kt
package com.hambaksanta.merrycri.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** 편지 열람 시작을 알리는 로컬 알림. 담당: B */
class GiftOpenWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        NotificationHelper.showGiftOpenNotification(applicationContext)
        return Result.success()
    }

    companion object {
        private const val DEBUG_WORK_NAME = "debug_gift_open_notification"
        private const val CHRISTMAS_WORK_NAME = "letter_open_2026_kst"
        private val opensAt = LocalDate.of(2026, 12, 25)
            .atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()

        fun scheduleChristmas(context: Context) {
            val delay = opensAt - System.currentTimeMillis()
            if (delay <= 0) return
            val request = OneTimeWorkRequestBuilder<GiftOpenWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                CHRISTMAS_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun scheduleDebug(context: Context) {
            val request = OneTimeWorkRequestBuilder<GiftOpenWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                DEBUG_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
