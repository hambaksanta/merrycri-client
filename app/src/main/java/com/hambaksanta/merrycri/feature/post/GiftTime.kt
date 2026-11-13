// feature/post/GiftTime.kt
package com.hambaksanta.merrycri.feature.post

import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 선물 잠금 규칙. 담당: C
 * serverNow = System.currentTimeMillis() + 서버 시간 오프셋 (FirebaseRepository.observeServerTimeOffset)
 */
object GiftTime {
    private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    val OPEN_AT: Long = ZonedDateTime.of(2026, 12, 25, 0, 0, 0, 0, KST).toInstant().toEpochMilli()
    val CLOSE_AT: Long = ZonedDateTime.of(2027, 1, 1, 0, 0, 0, 0, KST).toInstant().toEpochMilli()

    enum class State { LOCKED, OPEN, CLOSED }

    fun state(serverNow: Long, demoMode: Boolean): State = when {
        demoMode -> State.OPEN
        serverNow < OPEN_AT -> State.LOCKED
        serverNow < CLOSE_AT -> State.OPEN
        else -> State.CLOSED
    }

    /** 잠김 상태에서 보여줄 D-day (오늘이 12/24 면 1) */
    fun daysUntilOpen(serverNow: Long): Long =
        ((OPEN_AT - serverNow + 86_399_999) / 86_400_000).coerceAtLeast(0)
}
