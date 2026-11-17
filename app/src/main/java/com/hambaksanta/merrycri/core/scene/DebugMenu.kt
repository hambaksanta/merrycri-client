// core/scene/DebugMenu.kt
package com.hambaksanta.merrycri.core.scene

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.hambaksanta.merrycri.BuildConfig
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.sensor.DebugTiltController
import com.hambaksanta.merrycri.notify.GiftOpenWorker
import com.hambaksanta.merrycri.ui.theme.MerryColors
import kotlinx.coroutines.launch

/** 디버그 빌드에서만 보이는 센서·시연 대체 메뉴. 담당: B */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DebugMenu(
    presence: PresenceViewModel,
    modifier: Modifier = Modifier,
) {
    if (!BuildConfig.DEBUG) return

    val context = LocalContext.current
    val app = context.applicationContext as MerryCriApp
    val scope = rememberCoroutineScope()
    var isOpen by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            GiftOpenWorker.scheduleDebug(context)
            Toast.makeText(context, R.string.debug_notification_scheduled, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.debug_notification_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MerryColors.Card.copy(alpha = 0.85f))
            .combinedClickable(
                onClick = {},
                onLongClick = { isOpen = true },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.debug_menu_icon))
    }

    if (!isOpen) return
    Dialog(onDismissRequest = { isOpen = false }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MerryColors.Card,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.debug_menu_title))
                Button(
                    onClick = {
                        presence.sendSnowstorm()
                        Toast.makeText(context, R.string.debug_snowstorm_started, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.debug_snowstorm))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            DebugTiltController.steerLeft()
                            Toast.makeText(context, R.string.debug_sled_left_sent, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.debug_sled_left))
                    }
                    Button(
                        onClick = {
                            DebugTiltController.steerRight()
                            Toast.makeText(context, R.string.debug_sled_right_sent, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.debug_sled_right))
                    }
                }
                Button(
                    onClick = {
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            GiftOpenWorker.scheduleDebug(context)
                            Toast.makeText(
                                context,
                                R.string.debug_notification_scheduled,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.debug_gift_notification))
                }
                Button(
                    onClick = {
                        scope.launch {
                            app.settings.setDemoMode(true)
                            Toast.makeText(context, R.string.debug_demo_enabled, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.debug_demo_mode))
                }
                Button(
                    onClick = { isOpen = false },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}
