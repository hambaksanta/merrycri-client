// data/model/Ornament.kt
package com.hambaksanta.merrycri.data.model

/** ornaments/{ownerUid}/{writerUid}에 저장되는 트리 장식. */
data class Ornament(
    val writerUid: String,
    val type: String,
    val slot: Int,
    val createdAt: Long,
)
