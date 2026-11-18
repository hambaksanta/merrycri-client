// notify/GiftOpenWorker.kt
package com.hambaksanta.merrycri.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** 선물을 열 수 있다는 로컬 알림. 담당: B */
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
