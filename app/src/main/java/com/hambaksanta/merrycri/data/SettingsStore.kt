// data/SettingsStore.kt
package com.hambaksanta.merrycri.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** 기기별 설정: 데모 모드, BGM·효과음 켜기. 담당: B */
class SettingsStore(private val context: Context) {
    private val demoModeKey = booleanPreferencesKey("demo_mode")
    private val bgmKey = booleanPreferencesKey("bgm_on")
    private val sfxKey = booleanPreferencesKey("sfx_on")
    private val nicknameKey = stringPreferencesKey("nickname")
    private val friendCodeKey = stringPreferencesKey("friend_code")

    /** 켜면 12/25 전에도 선물이 열린다 (채점·시연용). */
    val demoMode: Flow<Boolean> = context.dataStore.data.map { it[demoModeKey] ?: false }
    val bgmOn: Flow<Boolean> = context.dataStore.data.map { it[bgmKey] ?: true }
    val sfxOn: Flow<Boolean> = context.dataStore.data.map { it[sfxKey] ?: true }
    val nickname: Flow<String?> = context.dataStore.data.map { it[nicknameKey] }
    val friendCode: Flow<String?> = context.dataStore.data.map { it[friendCodeKey] }

    suspend fun setDemoMode(on: Boolean) = context.dataStore.edit { it[demoModeKey] = on }
    suspend fun setBgmOn(on: Boolean) = context.dataStore.edit { it[bgmKey] = on }
    suspend fun setSfxOn(on: Boolean) = context.dataStore.edit { it[sfxKey] = on }

    suspend fun saveUserProfile(nickname: String, friendCode: String) {
        context.dataStore.edit {
            it[nicknameKey] = nickname
            it[friendCodeKey] = friendCode
        }
    }

    /** 첫 목록을 기준으로 저장하고, 이후에 도착한 편지가 있는지 반환한다. */
    suspend fun recordLetterArrivals(uid: String, arrivals: Map<String, Long>): Boolean {
        val key = stringSetPreferencesKey("letter_arrivals_$uid")
        var hasNewLetter = false
        context.dataStore.edit { preferences ->
            val previous = preferences[key]
            val latestByWriter = previous.orEmpty().mapNotNull { entry ->
                val timestamp = entry.substringAfterLast(':').toLongOrNull()
                    ?: return@mapNotNull null
                entry.substringBeforeLast(':') to timestamp
            }.toMap().toMutableMap()
            arrivals.forEach { (writerUid, timestamp) ->
                if (timestamp > (latestByWriter[writerUid] ?: 0L)) {
                    latestByWriter[writerUid] = timestamp
                    hasNewLetter = previous != null
                }
            }
            preferences[key] = latestByWriter.map { (writerUid, timestamp) ->
                "$writerUid:$timestamp"
            }.toSet()
        }
        return hasNewLetter
    }
}
