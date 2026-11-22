// data/model/UserProfile.kt
package com.hambaksanta.merrycri.data.model

/** users/{uid}에 저장되는 v2 사용자 정보. */
data class UserProfile(
    val nickname: String = "",
    val friendCode: String = "",
    val createdAt: Long = 0L,
)
