// data/model/Letter.kt
package com.hambaksanta.merrycri.data.model

/** letters/{ownerUid}/{writerUid}에 저장되는 공개일 이후 편지. */
data class Letter(
    val writerUid: String,
    val nickname: String,
    val text: String,
    val createdAt: Long,
)
