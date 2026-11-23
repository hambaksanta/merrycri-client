// ui/friends/FriendsViewModel.kt
package com.hambaksanta.merrycri.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hambaksanta.merrycri.data.FirebaseRepository
import com.hambaksanta.merrycri.data.model.FriendProfile
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

    private fun observeFriendItem(friend: FriendProfile): Flow<FriendListItem> =
        repository.observeOrnaments(friend.uid).map { ornaments ->
            val letterCount = ornaments.size
            FriendListItem(
                uid = friend.uid,
                nickname = friend.nickname,
                friendCode = friend.friendCode,
                letterCount = letterCount,
                treeStage = treeStageOf(letterCount),
                hasWritten = ornaments.any { it.writerUid == repository.uid },
            )
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

data class FriendListItem(
    val uid: String,
    val nickname: String,
    val friendCode: String,
    val letterCount: Int,
    val treeStage: Int,
    val hasWritten: Boolean,
)

sealed interface FriendsEvent {
    data class Added(val nickname: String) : FriendsEvent
    data class Error(val reason: FriendsError) : FriendsEvent
}

enum class FriendsError { INVALID_CODE, NOT_FOUND, SELF, ALREADY_ADDED, CONNECTION }
