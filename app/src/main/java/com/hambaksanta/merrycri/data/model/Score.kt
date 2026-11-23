// data/model/Score.kt
package com.hambaksanta.merrycri.data.model

/** scores/{ownerUid}/{writerUid}에 저장되는 미니게임 최고점. */
data class Score(
    val writerUid: String,
    val nickname: String,
    val best: Int,
    val updatedAt: Long,
)
