// ui/leaderboard/LeaderboardViewModel.kt
package com.hambaksanta.merrycri.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hambaksanta.merrycri.data.FirebaseRepository
import com.hambaksanta.merrycri.data.model.Score
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 특정 친구 트리의 미니게임 리더보드. 담당: C */
class LeaderboardViewModel(
    private val repository: FirebaseRepository,
    private val ownerUid: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    val myUid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    init {
        viewModelScope.launch {
            repository.observeScores(ownerUid).collect { scores ->
                _uiState.value = LeaderboardUiState(loading = false, scores = scores)
            }
        }
    }

    companion object {
        fun factory(repository: FirebaseRepository, ownerUid: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LeaderboardViewModel(repository, ownerUid) as T
            }
    }
}

data class LeaderboardUiState(
    val loading: Boolean = true,
    val scores: List<Score> = emptyList(),
)
