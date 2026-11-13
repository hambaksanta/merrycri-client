// core/effects/Snowfall.kt
package com.hambaksanta.merrycri.core.effects

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

private class Flake(val x: Float, val y: Float, val layer: Int, val phase: Float)

/**
 * 원근 눈: 뒤(작고 느림) · 중간 · 앞(크고 빠름) 3레이어. 담당: A
 * heavy = 흔들기 폭설 (속도 2.5배)
 */
@Composable
fun Snowfall(modifier: Modifier = Modifier, count: Int = 120, heavy: Boolean = false) {
    val flakes = remember(count) {
        List(count) { Flake(Random.nextFloat(), Random.nextFloat(), it % 3, Random.nextFloat() * 6.28f) }
    }
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { frameNanos = it }
    }
    Canvas(modifier) {
        val t = frameNanos / 1_000_000_000f
        val boost = if (heavy) 2.5f else 1f
        flakes.forEach { f ->
            val speed = when (f.layer) { 0 -> 0.03f; 1 -> 0.06f; else -> 0.10f } * boost
            val y = ((f.y + t * speed) % 1f) * size.height
            val x = (f.x + sin(t * 0.6f + f.phase) * 0.012f) * size.width
            drawCircle(
                color = Color.White.copy(alpha = 0.45f + 0.2f * f.layer),
                radius = (1.5f + f.layer * 1.5f) * density,
                center = Offset(x, y),
            )
        }
    }
}
