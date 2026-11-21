// ui/tree/AuroraView.kt
package com.hambaksanta.merrycri.ui.tree

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

/**
 * Lv5 밤하늘의 오로라. 띠 3개가 파도처럼 물결치며 옆으로 흘러간다. 담당: A
 * 그림 기준(320 x 380) 좌표로 계산해서 뷰 크기에 맞춰 늘린다. 기기에서 애니메이션을 끄면 멈춘 한 장면만 그린다.
 */
class AuroraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    /** 띠 하나: 기준 높이, 물결 높이, 두께, 색(RGB), 최대 불투명도, 위상 */
    private class Ribbon(val y0: Float, val amp: Float, val thick: Float, val rgb: Int, val alpha: Float, val phase: Float)

    private val ribbons = listOf(
        Ribbon(64f, 22f, 70f, 0x6FF2C0, 0.55f, 0.0f),
        Ribbon(104f, 18f, 58f, 0x6FD8F2, 0.35f, 1.7f),
        Ribbon(52f, 14f, 46f, 0xA48BFF, 0.38f, 3.1f),
    )
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val xs = FloatArray(STEPS + 1) { -20f + it * (360f / STEPS) }
    private val tops = FloatArray(STEPS + 1)

    private var shaders: List<LinearGradient> = emptyList()
    private var startTime = SystemClock.uptimeMillis()
    private var running = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTime = SystemClock.uptimeMillis()
        running = ValueAnimator.areAnimatorsEnabled() && visibility == VISIBLE
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val sy = h / ART_H
        // 세로 그라디언트(위·아래는 투명, 35% 지점이 가장 진함)는 크기가 바뀔 때만 만든다
        shaders = ribbons.map { r ->
            val color = ((r.alpha * 255).toInt() shl 24) or r.rgb
            LinearGradient(
                0f, (r.y0 - r.amp - 6f) * sy, 0f, (r.y0 + r.amp + r.thick + 16f) * sy,
                intArrayOf(r.rgb, color, r.rgb), floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP,
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0 || shaders.isEmpty()) return
        val t = if (running) (SystemClock.uptimeMillis() - startTime) / 1000f * SPEED else 0f
        val sx = width / ART_W
        val sy = height / ART_H

        ribbons.forEachIndexed { index, r ->
            // 윗선: 큰 물결 + 반대로 흐르는 잔물결 → 파도처럼 일렁임
            for (i in xs.indices) {
                val x = xs[i]
                tops[i] = r.y0 + r.amp * sin(x / 52f + r.phase + t * 0.9f) + 6f * sin(x / 23f - t * 1.3f + r.phase)
            }
            path.reset()
            path.moveTo(xs[0] * sx, tops[0] * sy)
            for (i in 1 until xs.size) path.lineTo(xs[i] * sx, tops[i] * sy)
            for (i in xs.indices.reversed()) {
                val x = xs[i]
                val bottom = tops[i] + r.thick + 10f * sin(x / 31f + r.phase - t * 0.7f)
                path.lineTo(x * sx, bottom * sy)
            }
            path.close()

            paint.shader = shaders[index]
            paint.alpha = ((0.8f + 0.2f * sin(t * 0.6f + r.phase)) * 255).toInt() // 천천히 밝아졌다 어두워짐
            canvas.drawPath(path, paint)
        }
        if (running) postInvalidateOnAnimation()
    }

    private companion object {
        const val ART_W = 320f
        const val ART_H = 380f
        const val STEPS = 36

        /** 물결 속도 배율. 숫자가 작을수록 느리다 (0.5 = 처음 만든 것보다 2배 느리게). */
        const val SPEED = 0.5f
    }
}
