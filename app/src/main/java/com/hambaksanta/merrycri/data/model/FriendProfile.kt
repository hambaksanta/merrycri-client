// data/model/FriendProfile.kt
package com.hambaksanta.merrycri.data.model

/** friends/{uid}/{friendUid}와 users/{friendUid}를 합친 친구 목록 항목. */
data class FriendProfile(
    val uid: String,
    val nickname: String,
    val friendCode: String,
    val friendedAt: Long,
)
