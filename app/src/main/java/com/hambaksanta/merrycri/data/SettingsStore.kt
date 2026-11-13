// data/SettingsStore.kt
package com.hambaksanta.merrycri.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** 기기별 설정: 데모 모드, BGM·효과음 켜기. 담당: B */
class SettingsStore(private val context: Context) {
    private val demoModeKey = booleanPreferencesKey("demo_mode")
    private val bgmKey = booleanPreferencesKey("bgm_on")
    private val sfxKey = booleanPreferencesKey("sfx_on")

    /** 켜면 12/25 전에도 선물이 열린다 (채점·시연용). */
    val demoMode: Flow<Boolean> = context.dataStore.data.map { it[demoModeKey] ?: false }
    val bgmOn: Flow<Boolean> = context.dataStore.data.map { it[bgmKey] ?: true }
    val sfxOn: Flow<Boolean> = context.dataStore.data.map { it[sfxKey] ?: true }

    suspend fun setDemoMode(on: Boolean) = context.dataStore.edit { it[demoModeKey] = on }
    suspend fun setBgmOn(on: Boolean) = context.dataStore.edit { it[bgmKey] = on }
    suspend fun setSfxOn(on: Boolean) = context.dataStore.edit { it[sfxKey] = on }
}
