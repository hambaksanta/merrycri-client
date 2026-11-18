// core/sensor/Haptics.kt
package com.hambaksanta.merrycri.core.sensor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/** 앱 전체에서 공통으로 사용하는 진동. 담당: B */
object Haptics {
    enum class Pattern(
        internal val timings: LongArray,
        internal val amplitudes: IntArray,
    ) {
        /** 눈덩이가 맞거나 버튼을 누를 때의 짧은 반응. */
        HIT(
            timings = longArrayOf(0, 45),
            amplitudes = intArrayOf(0, 180),
        ),

        /** 선물 상자를 열 때의 세 번 진동. */
        GIFT_OPEN(
            timings = longArrayOf(0, 40, 70, 80, 90, 160),
            amplitudes = intArrayOf(0, 100, 0, 180, 0, 255),
        ),
    }

    private const val TAG = "MerryCriHaptics"

    fun vibrate(context: Context, pattern: Pattern) {
        val vibrator = vibrator(context)

        Log.d(TAG, "vibrate pattern=${pattern.name}, hasVibrator=${vibrator?.hasVibrator() == true}")

        if (vibrator?.hasVibrator() != true) return

        vibrator.vibrate(
            VibrationEffect.createWaveform(
                pattern.timings,
                pattern.amplitudes,
                -1,
            ),
        )
    }

    /** 기존 호출부 호환을 위한 짧은 진동. */
    fun tick(context: Context, millis: Long = 30) {
        val vibrator = vibrator(context)

        Log.d(TAG, "tick millis=$millis, hasVibrator=${vibrator?.hasVibrator() == true}")

        if (vibrator?.hasVibrator() != true) return
        vibrator.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
