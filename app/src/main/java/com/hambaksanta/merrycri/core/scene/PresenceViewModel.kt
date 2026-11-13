// core/scene/PresenceViewModel.kt
package com.hambaksanta.merrycri.core.scene

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.core.character.Outfit
import com.hambaksanta.merrycri.core.character.Snack
import com.hambaksanta.merrycri.data.model.Presence
import com.hambaksanta.merrycri.data.model.SnowstormEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * 앱 전체에서 하나만 쓴다 (Activity 범위). 담당: B
 * 내 캐릭터(me), 같은 공간의 다른 사람들(others), 서버 시간, 폭설 이벤트를 들고 있다.
 * Firebase 리스너는 Flow 구독이 끝나면(화면을 떠난 뒤 5초) 자동으로 해제된다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PresenceViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as MerryCriApp).repository

    private val _me = MutableStateFlow(Presence())
    val me: StateFlow<Presence> = _me.asStateFlow()

    private val currentSpace = MutableStateFlow(SceneId.WARDROBE)

    val others: StateFlow<List<Presence>> = currentSpace
        .flatMapLatest { repo.observeRoom(it.id) }
        .map { list -> list.filter { it.uid != repo.uid } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 서버 시간 - 폰 시간 (ms) */
    val serverOffset: StateFlow<Long> = repo.observeServerTimeOffset()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val snowstorm: StateFlow<SnowstormEvent?> = repo.observeSnowstorm()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isOnline: Boolean get() = repo.isAvailable

    fun serverNow(): Long = System.currentTimeMillis() + serverOffset.value

    fun setProfile(nick: String, outfit: Outfit) {
        _me.update { it.copy(nick = nick, outfit = outfit).withUid() }
        repo.saveUser(nick, outfit)
        repo.updatePresence(currentSpace.value.id, mapOf("nick" to nick, "outfit" to outfit))
    }

    /** SceneScaffold 가 공간에 들어갈 때 부른다. 이전 공간의 presence 는 지운다. */
    fun enterSpace(space: SceneId) {
        val prev = currentSpace.value
        if (prev != space) repo.leaveRoom(prev.id)
        currentSpace.value = space
        _me.update {
            it.copy(x = START_X, y = START_Y, tx = START_X, ty = START_Y, moveStartAt = 0).withUid()
        }
        repo.enterRoom(space.id, _me.value)
    }

    /** 바닥을 탭했을 때. 지금 위치에서 (x, y) 로 걷기 시작. x, y 는 0~1 비율. */
    fun moveTo(x: Double, y: Double) {
        val (cx, cy) = Movement.positionAt(_me.value, serverNow())
        _me.update { it.copy(x = cx, y = cy, tx = x, ty = y, moveStartAt = serverNow()) }
        repo.startMove(currentSpace.value.id, cx, cy, x, y)
    }

    fun setHolding(snack: Snack?) {
        _me.update { it.copy(holding = snack?.id, bites = 0, roasted = false) }
        repo.updatePresence(currentSpace.value.id, mapOf("holding" to snack?.id, "bites" to 0, "roasted" to false))
    }

    fun bite() {
        val bites = _me.value.bites + 1
        _me.update { it.copy(bites = bites) }
        repo.updatePresence(currentSpace.value.id, mapOf("bites" to bites))
    }

    fun roast() {
        _me.update { it.copy(roasted = true) }
        repo.updatePresence(currentSpace.value.id, mapOf("roasted" to true))
    }

    /** 흔들기 감지 또는 디버그 버튼 */
    fun sendSnowstorm() = repo.sendSnowstorm()

    /** 폭설 중인지: 이벤트가 10초 이내면 true */
    fun isSnowstormActive(event: SnowstormEvent?, serverNow: Long): Boolean =
        event != null && serverNow - event.at in 0..SNOWSTORM_MS

    private fun Presence.withUid(): Presence = copy(uid = repo.uid ?: LOCAL_UID)

    private companion object {
        const val START_X = 0.5
        const val START_Y = 0.8
        const val SNOWSTORM_MS = 10_000L
        const val LOCAL_UID = "local"
    }
}
