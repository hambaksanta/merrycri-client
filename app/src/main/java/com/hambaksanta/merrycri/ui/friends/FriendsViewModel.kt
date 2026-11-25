// ui/friends/FriendsViewModel.kt
package com.hambaksanta.merrycri.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hambaksanta.merrycri.data.FirebaseRepository
import com.hambaksanta.merrycri.data.model.FriendProfile
import com.hambaksanta.merrycri.data.model.Score
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 친구 목록(친구 코드로 추가) + 각 친구 트리의 미니게임 1등 점수. 담당: B(목록) · C(1등 점수 연동)
 */
class FriendsViewModel(
    private val repository: FirebaseRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<FriendsEvent>()
    val events: SharedFlow<FriendsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observeFriendItems()
                .catch {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = FriendsError.CONNECTION,
                    )
                }
                .collect { friends ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        friends = friends,
                        error = null,
                    )
                }
        }
    }

    fun addFriend(friendCode: String) {
        val code = friendCode.trim().uppercase()
        if (code.length != FRIEND_CODE_LENGTH) {
            viewModelScope.launch { _events.emit(FriendsEvent.Error(FriendsError.INVALID_CODE)) }
            return
        }
        if (_uiState.value.friends.any { it.friendCode == code }) {
            viewModelScope.launch { _events.emit(FriendsEvent.Error(FriendsError.ALREADY_ADDED)) }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true)
            runCatching { repository.addFriendByCode(code) }
                .onSuccess { _events.emit(FriendsEvent.Added(it.nickname)) }
                .onFailure { error -> _events.emit(FriendsEvent.Error(error.toFriendsError())) }
            _uiState.value = _uiState.value.copy(isAdding = false)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeFriendItems(): Flow<List<FriendListItem>> =
        repository.observeFriends().flatMapLatest { friends ->
            if (friends.isEmpty()) return@flatMapLatest flowOf(emptyList())
            combine(friends.map(::observeFriendItem)) { items -> items.toList() }
        }

    /** 친구 한 명의 트리 정보 + 그 트리의 미니게임 1등 점수(담당: C)를 합쳐서 내보낸다. */
    private fun observeFriendItem(friend: FriendProfile): Flow<FriendListItem> =
        repository.observeOrnaments(friend.uid).map { ornaments ->
            val letterCount = ornaments.size
            val topScore = runCatching { repository.observeScores(friend.uid).first() }
                .getOrNull()
                ?.maxByOrNull(Score::best)
            // 편지 장식은 친구당 한 자리(같은 writerUid면 덮어씀)라, 그 createdAt이 곧 '내가 이 친구에게
            // 마지막으로 편지를 쓴 시각'이다. 하루 한 통 제한 여부(오늘 이미 썼는지)를 여기서 판단한다. 담당: C
            val myOrnament = ornaments.find { it.writerUid == repository.uid }
            FriendListItem(
                uid = friend.uid,
                nickname = friend.nickname,
                friendCode = friend.friendCode,
                letterCount = letterCount,
                treeStage = treeStageOf(letterCount),
                wroteToday = myOrnament?.let { isSameDaySeoul(it.createdAt) } ?: false,
                topScore = topScore,
            )
        }

    private fun isSameDaySeoul(epochMillis: Long): Boolean {
        val zone = ZoneId.of("Asia/Seoul")
        val day = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return day == LocalDate.now(zone)
    }

    private fun Throwable.toFriendsError(): FriendsError = when {
        this is NoSuchElementException -> FriendsError.NOT_FOUND
        message?.contains("cannot add self") == true -> FriendsError.SELF
        this is IllegalArgumentException -> FriendsError.INVALID_CODE
        else -> FriendsError.CONNECTION
    }

    private fun treeStageOf(letterCount: Int): Int = when (letterCount) {
        0 -> 1
        in 1..2 -> 2
        in 3..5 -> 3
        in 6..9 -> 4
        else -> 5
    }

    companion object {
        private const val FRIEND_CODE_LENGTH = 6

        fun factory(repository: FirebaseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FriendsViewModel(repository) as T
            }
    }
}

data class FriendsUiState(
    val isLoading: Boolean = true,
    val isAdding: Boolean = false,
    val friends: List<FriendListItem> = emptyList(),
    val error: FriendsError? = null,
)

/** 친구 한 명의 트리 정보 + 그 트리에서 지금까지 나온 미니게임 최고점(1등). topScore·wroteToday 담당: C */
data class FriendListItem(
    val uid: String,
    val nickname: String,
    val friendCode: String,
    val letterCount: Int,
    val treeStage: Int,
    /** 오늘(한국시간 기준) 이 친구에게 이미 편지를 썼는지. 편지는 친구당 하루 한 통으로 제한한다. */
    val wroteToday: Boolean,
    val topScore: Score?,
)

sealed interface FriendsEvent {
    data class Added(val nickname: String) : FriendsEvent
    data class Error(val reason: FriendsError) : FriendsEvent
}

enum class FriendsError { INVALID_CODE, NOT_FOUND, SELF, ALREADY_ADDED, CONNECTION }
