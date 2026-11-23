// data/model/Letter.kt
package com.hambaksanta.merrycri.data.model

/** letters/{recipientUid}/{letterId}에서 읽어 오는 편지. */
data class Letter(
    val id: String = "",
    val senderNickname: String = "",
    val content: String = "",
    val ornamentType: String = "",
    val createdAt: Long = 0L,
    val opened: Boolean = false,
)
