// ui/game/BreakoutView.kt
package com.hambaksanta.merrycri.ui.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.hambaksanta.merrycri.R
import kotlin.math.abs
import kotlin.random.Random

/** 선물 벽돌깨기 결과를 전달받는 콜백. 담당: C */
interface BreakoutListener {
    fun onScoreChanged(score: Int)
    fun onLivesChanged(lives: Int)
    fun onTimeChanged(secondsLeft: Int)
    fun onBrickHit(golden: Boolean)
    fun onLifeLost()
    fun onGameOver(finalScore: Int)
}

/**
 * 크리스마스 테마 벽돌깨기(선물 상자 깨기) 게임 뷰.
 * 60초 제한시간 · 목숨 3개 · 일반 상자 10점 · 황금 상자 30점.
 * 공 3개가 동시에 움직여 스릴을 높이고, 모든 벽돌을 깨면 보너스 점수와 함께
 * 새 보드가 생성되고 공 속도가 올라간다.
 */
class BreakoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var listener: BreakoutListener? = null

    // ===== 게임 상수 =====
    private val totalMillis = GAME_DURATION_MS
    private val maxLives = 3
    private val rows = 6
    private val cols = 7
    private val goldenChance = 0.12
    private val baseSpeedPxPerSec = 680f
    private val speedStepUp = 1.12f
    private val maxSpeedSteps = 3
    private val clearBonusScore = 50
    private val normalBrickScore = 10
    private val goldenBrickScore = 30
    private val ballCount = 2
    private val brickAreaTopRatio = 0.07f
    private val brickAreaBottomRatio = 0.62f

    // ===== 상태 =====
    private var score = 0
    private var lives = maxLives
    private var remainingMillis = totalMillis
    private var speedSteps = 0
    private var running = false
    private var started = false
    private var gameOver = false

    private data class Brick(val rect: RectF, val golden: Boolean, var alive: Boolean = true)

    private data class Ball(var x: Float = 0f, var y: Float = 0f, var dx: Float = 0f, var dy: Float = 0f)

    private var bricks: MutableList<Brick> = mutableListOf()
    private val paddleRect = RectF()
    private var paddleWidth = 0f
    private var paddleHeight = 0f

    private var balls: MutableList<Ball> = mutableListOf()
    private var ballRadius = 0f

    private var lastSecondReported = -1
    private var lastFrameTime = 0L

    // ===== Paint =====
    private val paddlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
    }
    private val paddleStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.berry)
    }
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }
    private val ballCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
    }
    private val brickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.pine)
    }
    private val brickAltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.berry)
    }
    private val brickGoldenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.gold)
    }
    private val brickRibbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.gold)
        strokeWidth = 3f
    }
    private val brickRibbonGoldenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.night)
        strokeWidth = 3f
    }

    private val handler = Handler(Looper.getMainLooper())
    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            val now = SystemClock.elapsedRealtime()
            val dt = if (lastFrameTime == 0L) FRAME_DELAY_MS else (now - lastFrameTime)
            lastFrameTime = now
            update(dt.coerceAtMost(48L))
            invalidate()
            handler.postDelayed(this, FRAME_DELAY_MS)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        paddleWidth = w * 0.26f
        paddleHeight = h * 0.025f
        ballRadius = w * 0.018f
        resetPaddleAndBalls(w, h)
        if (bricks.isEmpty()) generateBricks(w)
    }

    private fun resetPaddleAndBalls(w: Int = width, h: Int = height) {
        val paddleY = h * 0.92f
        val paddleCenterX = (w / 2f).coerceIn(paddleWidth / 2f, w - paddleWidth / 2f)
        paddleRect.set(
            paddleCenterX - paddleWidth / 2f,
            paddleY,
            paddleCenterX + paddleWidth / 2f,
            paddleY + paddleHeight,
        )
        val speed = baseSpeedPxPerSec * speedMultiplier()
        val newBalls = mutableListOf<Ball>()
        for (i in 0 until ballCount) {
            // 패들 위에서 서로 다른 각도로 출발해 바로 퍼지도록 함
            val spread = (i - (ballCount - 1) / 2f) * 0.35f
            val startX = (paddleRect.centerX() + spread * paddleWidth * 0.3f)
                .coerceIn(ballRadius, w - ballRadius)
            newBalls.add(
                Ball(
                    x = startX,
                    y = paddleRect.top - ballRadius - 4f,
                    dx = speed * (spread.coerceIn(-1f, 1f)).let { if (it == 0f) (if (Random.nextBoolean()) 0.3f else -0.3f) else it },
                    dy = -speed,
                ),
            )
        }
        // dx/dy가 speed를 넘지 않도록 정규화
        for (ball in newBalls) {
            val mag = kotlin.math.hypot(ball.dx.toDouble(), ball.dy.toDouble()).toFloat()
            if (mag > 0f) {
                val scale = speed / mag
                ball.dx *= scale
                ball.dy *= scale
            }
        }
        balls = newBalls
    }

    private fun speedMultiplier(): Float {
        var m = 1f
        repeat(speedSteps) { m *= speedStepUp }
        return m
    }

    private fun rescaleBallsToCurrentSpeed() {
        val speed = baseSpeedPxPerSec * speedMultiplier()
        for (ball in balls) {
            val mag = kotlin.math.hypot(ball.dx.toDouble(), ball.dy.toDouble()).toFloat()
            if (mag > 0f) {
                val scale = speed / mag
                ball.dx *= scale
                ball.dy *= scale
            }
        }
    }

    private fun generateBricks(w: Int) {
        val marginX = w * 0.035f
        val top = height * brickAreaTopRatio
        val bottom = height * brickAreaBottomRatio
        val gapX = w * 0.015f
        val gapY = height * 0.012f
        val brickW = (w - marginX * 2 - gapX * (cols - 1)) / cols
        val brickH = (bottom - top - gapY * (rows - 1)) / rows
        val newBricks = mutableListOf<Brick>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val left = marginX + col * (brickW + gapX)
                val t = top + row * (brickH + gapY)
                val rect = RectF(left, t, left + brickW, t + brickH)
                val golden = Random.nextDouble() < goldenChance
                newBricks.add(Brick(rect, golden))
            }
        }
        bricks = newBricks
    }

    /** 처음 탭하면 호출. */
    fun start() {
        if (started) return
        started = true
        gameOver = false
        running = true
        lastFrameTime = 0L
        listener?.onScoreChanged(score)
        listener?.onLivesChanged(lives)
        listener?.onTimeChanged((remainingMillis / 1000).toInt())
        handler.post(frameRunnable)
    }

    fun pauseGame() {
        running = false
        handler.removeCallbacks(frameRunnable)
    }

    fun resumeGame() {
        if (started && !gameOver) {
            running = true
            lastFrameTime = 0L
            handler.post(frameRunnable)
        }
    }

    fun restart() {
        handler.removeCallbacks(frameRunnable)
        score = 0
        lives = maxLives
        remainingMillis = totalMillis
        speedSteps = 0
        started = false
        gameOver = false
        running = false
        lastSecondReported = -1
        if (width > 0) {
            generateBricks(width)
            resetPaddleAndBalls()
        }
        invalidate()
    }

    private fun update(dtMs: Long) {
        remainingMillis -= dtMs
        if (remainingMillis <= 0) {
            remainingMillis = 0
            endGame()
            return
        }
        val secLeft = (remainingMillis / 1000).toInt()
        if (secLeft != lastSecondReported) {
            lastSecondReported = secLeft
            listener?.onTimeChanged(secLeft)
        }

        val dtSec = dtMs / 1000f
        val fallenBalls = mutableListOf<Ball>()

        for (ball in balls) {
            ball.x += ball.dx * dtSec
            ball.y += ball.dy * dtSec

            // 좌우 벽
            if (ball.x - ballRadius < 0) {
                ball.x = ballRadius
                ball.dx = abs(ball.dx)
            } else if (ball.x + ballRadius > width) {
                ball.x = width - ballRadius
                ball.dx = -abs(ball.dx)
            }
            // 위쪽 벽
            if (ball.y - ballRadius < 0) {
                ball.y = ballRadius
                ball.dy = abs(ball.dy)
            }

            // 패들
            if (ball.dy > 0 &&
                ball.y + ballRadius >= paddleRect.top &&
                ball.y - ballRadius <= paddleRect.bottom &&
                ball.x >= paddleRect.left &&
                ball.x <= paddleRect.right
            ) {
                ball.y = paddleRect.top - ballRadius
                val hitOffset = (ball.x - paddleRect.centerX()) / (paddleWidth / 2f)
                val speed = kotlin.math.hypot(ball.dx.toDouble(), ball.dy.toDouble()).toFloat()
                val clampedOffset = hitOffset.coerceIn(-1f, 1f)
                ball.dx = speed * clampedOffset * 0.9f
                ball.dy = -kotlin.math.sqrt((speed * speed - ball.dx * ball.dx).coerceAtLeast(1f))
            }

            // 바닥으로 떨어짐 -> 이 공은 제거 (목숨은 공이 모두 사라졌을 때만 차감)
            if (ball.y - ballRadius > height) {
                fallenBalls.add(ball)
                continue
            }

            // 벽돌 충돌 (가장 먼저 겹치는 하나만 처리)
            for (brick in bricks) {
                if (!brick.alive) continue
                val expanded = RectF(
                    brick.rect.left - ballRadius,
                    brick.rect.top - ballRadius,
                    brick.rect.right + ballRadius,
                    brick.rect.bottom + ballRadius,
                )
                if (expanded.contains(ball.x, ball.y)) {
                    brick.alive = false
                    val gained = if (brick.golden) goldenBrickScore else normalBrickScore
                    score += gained
                    listener?.onScoreChanged(score)
                    listener?.onBrickHit(brick.golden)
                    val hitFromSide = ball.x < brick.rect.left || ball.x > brick.rect.right
                    if (hitFromSide) ball.dx = -ball.dx else ball.dy = -ball.dy
                    break
                }
            }
        }

        if (fallenBalls.isNotEmpty()) {
            balls.removeAll(fallenBalls)
        }

        if (balls.isEmpty()) {
            lives -= 1
            listener?.onLivesChanged(lives)
            listener?.onLifeLost()
            if (lives <= 0) {
                endGame()
                return
            }
            resetPaddleAndBalls()
            return
        }

        if (bricks.none { it.alive }) {
            score += clearBonusScore
            listener?.onScoreChanged(score)
            if (speedSteps < maxSpeedSteps) speedSteps += 1
            generateBricks(width)
            rescaleBallsToCurrentSpeed()
        }
    }

    private fun endGame() {
        running = false
        gameOver = true
        handler.removeCallbacks(frameRunnable)
        listener?.onGameOver(score)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!started) {
                    start()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val half = paddleWidth / 2f
                val centerX = event.x.coerceIn(half, width - half)
                paddleRect.set(centerX - half, paddleRect.top, centerX + half, paddleRect.bottom)
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (brick in bricks) {
            if (!brick.alive) continue
            val base = when {
                brick.golden -> brickGoldenPaint
                bricks.indexOf(brick) % 2 == 0 -> brickPaint
                else -> brickAltPaint
            }
            canvas.drawRoundRect(brick.rect, 10f, 10f, base)
            val ribbon = if (brick.golden) brickRibbonGoldenPaint else brickRibbonPaint
            val cx = brick.rect.centerX()
            canvas.drawLine(cx, brick.rect.top, cx, brick.rect.bottom, ribbon)
            canvas.drawLine(brick.rect.left, brick.rect.centerY(), brick.rect.right, brick.rect.centerY(), ribbon)
        }

        canvas.drawRoundRect(paddleRect, paddleHeight / 2f, paddleHeight / 2f, paddlePaint)
        val stripeW = paddleWidth / 7f
        var sx = paddleRect.left
        var draw = false
        while (sx < paddleRect.right) {
            if (draw) {
                canvas.drawRect(
                    sx, paddleRect.top,
                    (sx + stripeW).coerceAtMost(paddleRect.right), paddleRect.bottom,
                    paddleStripePaint,
                )
            }
            sx += stripeW
            draw = !draw
        }

        for (ball in balls) {
            canvas.drawCircle(ball.x, ball.y, ballRadius * 0.4f, ballCorePaint)
            val arm = ballRadius
            canvas.drawLine(ball.x - arm, ball.y, ball.x + arm, ball.y, ballPaint)
            canvas.drawLine(ball.x, ball.y - arm, ball.x, ball.y + arm, ballPaint)
            canvas.drawLine(ball.x - arm * 0.7f, ball.y - arm * 0.7f, ball.x + arm * 0.7f, ball.y + arm * 0.7f, ballPaint)
            canvas.drawLine(ball.x - arm * 0.7f, ball.y + arm * 0.7f, ball.x + arm * 0.7f, ball.y - arm * 0.7f, ballPaint)
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(frameRunnable)
        super.onDetachedFromWindow()
    }

    companion object {
        private const val GAME_DURATION_MS = 60_000L
        private const val FRAME_DELAY_MS = 16L
    }
}
