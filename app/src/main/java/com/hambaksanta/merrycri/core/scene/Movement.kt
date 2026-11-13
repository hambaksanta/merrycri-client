// core/scene/Movement.kt
package com.hambaksanta.merrycri.core.scene

import com.hambaksanta.merrycri.data.model.Presence
import kotlin.math.hypot

/**
 * 걷기 계산. 담당: B
 * DB 에는 출발점(x, y)·목적지(tx, ty)·출발 시각(moveStartAt)만 있고,
 * 지금 위치는 각 폰이 서버 시간 기준으로 똑같이 계산한다. 그래서 걷는 동안 DB 에 아무것도 안 보낸다.
 */
object Movement {
    /** 1초에 화면 비율로 얼마나 가는지 (0.3 = 화면 폭의 30%) */
    const val SPEED_PER_SEC = 0.3

    fun durationMs(p: Presence): Long {
        val dist = hypot(p.tx - p.x, p.ty - p.y)
        return (dist / SPEED_PER_SEC * 1000).toLong()
    }

    /** serverNow 시점의 위치 (0~1 비율) */
    fun positionAt(p: Presence, serverNow: Long): Pair<Double, Double> {
        val duration = durationMs(p)
        if (duration <= 0 || p.moveStartAt <= 0) return p.tx to p.ty
        val t = ((serverNow - p.moveStartAt).toDouble() / duration).coerceIn(0.0, 1.0)
        return (p.x + (p.tx - p.x) * t) to (p.y + (p.ty - p.y) * t)
    }

    fun isWalking(p: Presence, serverNow: Long): Boolean =
        p.moveStartAt > 0 && serverNow < p.moveStartAt + durationMs(p)
}
