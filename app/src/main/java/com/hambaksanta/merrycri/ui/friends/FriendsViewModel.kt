// ui/friends/FriendsViewModel.kt
package com.hambaksanta.merrycri.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hambaksanta.merrycri.data.FirebaseRepository
import com.hambaksanta.merrycri.data.model.FriendProfile
import com.hambaksanta.merrycri.data.model.Score
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 친구 목록 + 각 친구 트리의 미니게임 1등 점수. 담당: C (리더보드 연동)
 *
 * 친구가 많아져도 매번 전체 리스트를 다시 그리지 않도록, 친구 목록이 바뀔 때만
 * 각 친구 트리의 [FirebaseRepository.observeScores] 최신 값을 한 번씩 읽어 온다.
 */
class FriendsViewModel(private val repository: FirebaseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private var scoreJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeFriends().collect { friends -> loadTopScores(friends) }
        }
    }

    private fun loadTopScores(friends: List<FriendProfile>) {
        scoreJob?.cancel()
        if (friends.isEmpty()) {
            _uiState.value = FriendsUiState(loading = false, items = emptyList())
            return
        }
        scoreJob = viewModelScope.launch {
            val items = friends.map { friend ->
                val topScore = runCatching { repository.observeScores(friend.uid).first() }
                    .getOrNull()
                    ?.maxByOrNull(Score::best)
                FriendListItem(friend = friend, topScore = topScore)
            }
            _uiState.value = FriendsUiState(loading = false, items = items)
        }
    }

    companion object {
        fun factory(repository: FirebaseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FriendsViewModel(repository) as T
            }
    }
}

data class FriendsUiState(
    val loading: Boolean = true,
    val items: List<FriendListItem> = emptyList(),
)

/** 친구 한 명 + 그 친구 트리에서 지금까지 나온 미니게임 최고점(1등). */
data class FriendListItem(
    val friend: FriendProfile,
    val topScore: Score?,
)
