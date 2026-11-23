// ui/tree/WeatherView.kt
package com.hambaksanta.merrycri.ui.tree

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 하늘 위에 겹쳐 그리는 날씨 효과. 담당: A
 * - 내리는 눈: 단계마다 양이 다르다 (Lv1이 가장 많이, 춥게)
 * - 반짝이는 별: Lv4(저녁)·Lv5(밤)만
 * 좌표는 그림 기준(320 x 380)으로 계산하고 뷰 크기에 맞춰 늘린다. 기기에서 애니메이션을 끄면 멈춘 한 장면만 그린다.
 */
class WeatherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private class Flake(val x: Float, val y: Float, val r: Float, val speed: Float, val amp: Float, val freq: Float, val phase: Float, val alpha: Int)
    private class Twinkle(val x: Float, val y: Float, val period: Float, val phase: Float, val sparkle: Boolean)

    private val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeCap = Paint.Cap.ROUND
    }

    private var flakes: List<Flake> = emptyList()
    private var twinkles: List<Twinkle> = emptyList()
    private var startTime = SystemClock.uptimeMillis()
    private var running = false

    /** 트리 단계(1~5)에 맞춰 눈 양과 별을 정한다. */
    fun setStage(stage: Int) {
        val rnd = Random(97 + stage)
        val count = SNOW_COUNT.getOrElse(stage - 1) { 40 }
        flakes = List(count) {
            Flake(
                x = rnd.nextFloat() * ART_W, y = rnd.nextFloat() * ART_H,
                r = 0.8f + rnd.nextFloat() * 1.6f, speed = 14f + rnd.nextFloat() * 26f,
                amp = 6f + rnd.nextFloat() * 8f, freq = 0.3f + rnd.nextFloat() * 0.5f,
                phase = rnd.nextFloat() * TWO_PI, alpha = (140 + rnd.nextInt(115)),
            )
        }
        twinkles = if (stage >= 4) {
            val maxY = if (stage >= 5) 200f else 150f
            List(14) {
                Twinkle(8f + rnd.nextFloat() * 304f, 6f + rnd.nextFloat() * maxY, 1.5f + rnd.nextFloat() * 1.5f, rnd.nextFloat() * TWO_PI, rnd.nextFloat() < 0.3f)
            }
        } else {
            emptyList()
        }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = ValueAnimator.areAnimatorsEnabled()
        startTime = SystemClock.uptimeMillis()
        if (running) postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        running = false
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        val shouldRun = isAttachedToWindow && isShown && ValueAnimator.areAnimatorsEnabled()
        if (shouldRun && !running) {
            running = true
            postInvalidateOnAnimation()
        } else if (!shouldRun) {
            running = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val t = if (running) (SystemClock.uptimeMillis() - startTime) / 1000f else 1.2f
        val sx = width / ART_W
        val sy = height / ART_H

        for (k in twinkles) {
            val a = 0.2f + 0.8f * (0.5f + 0.5f * sin(TWO_PI * t / k.period + k.phase))
            starPaint.alpha = (a * 255).toInt()
            starPaint.style = Paint.Style.FILL
            canvas.drawCircle(k.x * sx, k.y * sy, 1.3f * sx, starPaint)
            if (k.sparkle) {
                val arm = 4f * a * sx
                starPaint.strokeWidth = 0.8f * sx
                canvas.drawLine(k.x * sx - arm, k.y * sy, k.x * sx + arm, k.y * sy, starPaint)
                canvas.drawLine(k.x * sx, k.y * sy - arm, k.x * sx, k.y * sy + arm, starPaint)
            }
        }
        for (f in flakes) {
            val y = (f.y + f.speed * t) % (ART_H + 10f) - 5f
            val x = ((f.x + f.amp * sin(TWO_PI * f.freq * t + f.phase)) % ART_W + ART_W) % ART_W
            snowPaint.alpha = f.alpha
            canvas.drawCircle(x * sx, y * sy, f.r * sx, snowPaint)
        }
        if (running) postInvalidateOnAnimation()
    }

    private companion object {
        const val ART_W = 320f
        const val ART_H = 380f
        const val TWO_PI = (2 * PI).toFloat()
        val SNOW_COUNT = intArrayOf(70, 35, 30, 40, 45)
    }
}
