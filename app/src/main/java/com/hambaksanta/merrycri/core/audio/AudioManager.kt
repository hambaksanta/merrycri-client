// core/audio/AudioManager.kt
package com.hambaksanta.merrycri.core.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** 효과음 목록. res/raw 에 같은 이름의 파일(예: sfx_bell.ogg)을 넣으면 자동으로 쓰인다. */
enum class Sfx(val rawName: String) {
    SNOW_STEP("sfx_snow_step"), BELL("sfx_bell"), PAPER("sfx_paper"), RIBBON("sfx_ribbon"),
    SIP("sfx_sip"), CRUNCH("sfx_crunch"), THUD("sfx_thud"), WHOOSH("sfx_whoosh"),
    CRACKLE("sfx_crackle"), WIND("sfx_wind"), BUTTON("sfx_button"),
    CHIME("sfx_chime"), HANGER("sfx_hanger"), SNAP("sfx_snap"), CHEW("sfx_chew"),
}

/** 팀 약속 인터페이스. 담당: B */
interface AudioManager {
    fun playSfx(sfx: Sfx)
    /** 라운지 입장 시 0.4 */
    fun duck(volume: Float)
    fun unduck()
    fun pause()
    fun resume()
    fun setBgmEnabled(enabled: Boolean)
}

/**
 * BGM: res/raw/bgm_01 ~ bgm_08 을 셔플 + 전체 반복. 파일이 없으면 조용히 넘어간다.
 * 효과음: SoundPool.
 */
class MerryAudioManager(context: Context) : AudioManager {
    private val appContext = context.applicationContext
    private val player = ExoPlayer.Builder(appContext).build()
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val sfxIds = mutableMapOf<Sfx, Int>()
    private var bgmEnabled = true

    init {
        val items = (1..8)
            .mapNotNull { rawId("bgm_%02d".format(it)) }
            .map { MediaItem.fromUri(Uri.parse("android.resource://${appContext.packageName}/$it")) }
        if (items.isNotEmpty()) {
            player.setMediaItems(items)
            player.shuffleModeEnabled = true
            player.repeatMode = Player.REPEAT_MODE_ALL
            player.prepare()
        }
        Sfx.entries.forEach { s -> rawId(s.rawName)?.let { sfxIds[s] = soundPool.load(appContext, it, 1) } }
    }

    @SuppressLint("DiscouragedApi")
    private fun rawId(name: String): Int? =
        appContext.resources.getIdentifier(name, "raw", appContext.packageName).takeIf { it != 0 }

    override fun playSfx(sfx: Sfx) {
        sfxIds[sfx]?.let { soundPool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    override fun duck(volume: Float) { player.volume = volume }
    override fun unduck() { player.volume = 1f }
    override fun pause() { player.pause() }
    override fun resume() { if (bgmEnabled && player.mediaItemCount > 0) player.play() }

    override fun setBgmEnabled(enabled: Boolean) {
        bgmEnabled = enabled
        if (enabled) resume() else pause()
    }
}
