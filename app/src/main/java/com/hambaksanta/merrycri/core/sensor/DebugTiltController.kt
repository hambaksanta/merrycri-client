// core/sensor/DebugTiltController.kt
package com.hambaksanta.merrycri.core.sensor

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** AVD 시연에서 실제 기울기 센서 대신 쓰는 썰매 조향 입력. 담당: B */
object DebugTiltController {
    private val _tilt = MutableStateFlow(0f)
    val tilt: StateFlow<Float> = _tilt.asStateFlow()

    fun steerLeft() {
        _tilt.value = -1f
        Log.d(TAG, "tilt=-1.0")
    }

    fun steerRight() {
        _tilt.value = 1f
        Log.d(TAG, "tilt=1.0")
    }

    fun center() {
        _tilt.value = 0f
        Log.d(TAG, "tilt=0.0")
    }

    private const val TAG = "DebugTiltController"
}
