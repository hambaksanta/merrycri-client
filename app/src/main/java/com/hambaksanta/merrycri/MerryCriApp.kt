// MerryCriApp.kt
package com.hambaksanta.merrycri

import android.app.Application
import com.hambaksanta.merrycri.core.audio.AudioManager
import com.hambaksanta.merrycri.core.audio.MerryAudioManager
import com.hambaksanta.merrycri.data.FirebaseRepository
import com.hambaksanta.merrycri.data.SettingsStore

/** 앱 전체에서 하나씩만 쓰는 공통 서비스. 담당: B */
class MerryCriApp : Application() {
    lateinit var repository: FirebaseRepository
        private set
    lateinit var audio: AudioManager
        private set
    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        repository = FirebaseRepository(this)
        audio = MerryAudioManager(this)
        settings = SettingsStore(this)
        repository.signIn { }
    }
}
