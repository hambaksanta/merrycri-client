// data/room/GameRecordDao.kt
package com.hambaksanta.merrycri.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameRecordDao {
    @Insert
    suspend fun insert(record: GameRecord)

    @Query(
        "SELECT * FROM game_records WHERE playerUid IS :playerUid " +
            "ORDER BY playedAt DESC, id DESC LIMIT :limit",
    )
    fun observeRecent(playerUid: String?, limit: Int): Flow<List<GameRecord>>
}
