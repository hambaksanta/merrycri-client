// core/sensor/ShakeDetector.kt
package com.hambaksanta.merrycri.core.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import kotlin.math.sqrt

/**
 * 흔들기 감지. AVD 에서는 Extended Controls → Virtual sensors 로 테스트. 담당: B
 * start()/stop() 은 화면에 들어오고 나갈 때 (DisposableEffect) 부른다.
 */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {
    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var lastShakeAt = 0L

    fun start() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
        val now = SystemClock.elapsedRealtime()
        if (acceleration > SHAKE_THRESHOLD && now - lastShakeAt >= SHAKE_COOLDOWN_MS) {
            lastShakeAt = now
            Log.d(TAG, "Shake detected: acceleration=$acceleration")
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val TAG = "ShakeDetector"
        const val SHAKE_THRESHOLD = 12f
        const val SHAKE_COOLDOWN_MS = 2_000L
    }
}
