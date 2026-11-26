// notify/DebugMenu.kt
package com.hambaksanta.merrycri.notify

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.firebase.FirebaseApp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hambaksanta.merrycri.BuildConfig
import com.hambaksanta.merrycri.R

/** 트리 탭 길게 누르기로 여는 시연 메뉴. 담당: B */
object DebugMenu {
    fun show(activity: AppCompatActivity) {
        if (!BuildConfig.DEBUG) return

        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.open_debug_title)
            .setMessage(R.string.open_debug_instructions)
            .setPositiveButton(R.string.open_debug_console) { _, _ ->
                openConsoleWithTimestamp(activity)
            }
            .setNeutralButton(R.string.open_debug_test_notification) { _, _ ->
                GiftOpenWorker.scheduleDebug(activity)
                Toast.makeText(activity, R.string.open_debug_notification_scheduled, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openConsoleWithTimestamp(activity: AppCompatActivity) {
        val projectId = FirebaseApp.getApps(activity).firstOrNull()?.options?.projectId
        if (projectId == null) {
            Toast.makeText(activity, R.string.open_debug_console_unavailable, Toast.LENGTH_SHORT).show()
            return
        }

        activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(
            ClipData.newPlainText("openAt", System.currentTimeMillis().toString()),
        )
        val console = "https://console.firebase.google.com/project/$projectId/database".toUri()
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, console))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.open_debug_console_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}
