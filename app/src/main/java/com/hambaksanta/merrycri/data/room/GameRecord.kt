// data/room/GameRecord.kt
package com.hambaksanta.merrycri.data.room

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_records",
    indices = [Index(value = ["playerUid", "playedAt", "id"])],
)
data class GameRecord(
    val playerUid: String?,
    val ownerUid: String?,
    val score: Int,
    val playedAt: Long,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)
