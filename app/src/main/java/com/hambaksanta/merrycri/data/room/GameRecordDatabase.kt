// data/room/GameRecordDatabase.kt
package com.hambaksanta.merrycri.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GameRecord::class], version = 1, exportSchema = false)
abstract class GameRecordDatabase : RoomDatabase() {
    abstract fun gameRecordDao(): GameRecordDao
}
