// data/room/GameRecordRepository.kt
package com.hambaksanta.merrycri.data.room

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GameRecordRepository(private val dao: GameRecordDao) {
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 게임 한 판당 한 번 호출한다. Firebase 점수 저장 결과와 별개로 기기에 남긴다.
     * 화면을 닫더라도 저장을 마칠 수 있도록 앱 범위에서 실행한다.
     */
    fun recordFinishedGame(playerUid: String?, ownerUid: String?, score: Int) {
        require(score >= 0) { "score must not be negative" }
        val record = GameRecord(playerUid, ownerUid, score, System.currentTimeMillis())
        writeScope.launch {
            try {
                dao.insert(record)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("GameRecordRepository", "Could not save game record", error)
            }
        }
    }

    /** 로그인 전 연습 기록은 null로 조회한다. */
    fun observeRecent(playerUid: String?, limit: Int = 10): Flow<List<GameRecord>> {
        require(limit > 0) { "limit must be positive" }
        return dao.observeRecent(playerUid, limit)
    }
}
