// ui/onboarding/OnboardingViewModel.kt
package com.hambaksanta.merrycri.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hambaksanta.merrycri.data.AuthState
import com.hambaksanta.merrycri.data.FirebaseRepository
import com.hambaksanta.merrycri.data.SettingsStore
import com.hambaksanta.merrycri.data.model.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: FirebaseRepository,
    private val settings: SettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    private var profileCheckJob: Job? = null

    init {
        viewModelScope.launch {
            repository.authState.collect { state ->
                when (state) {
                    AuthState.Loading -> _uiState.value = OnboardingUiState(OnboardingStep.CHECKING)
                    AuthState.SignedIn -> checkExistingProfile()
                    AuthState.Offline -> _uiState.value = OnboardingUiState(
                        step = OnboardingStep.ERROR,
                        error = OnboardingError.OFFLINE,
                    )
                    AuthState.Error -> _uiState.value = OnboardingUiState(
                        step = OnboardingStep.ERROR,
                        error = OnboardingError.CONNECTION,
                    )
                }
            }
        }
    }

    fun createUser(nickname: String) {
        if (nickname.isBlank()) {
            _uiState.value = OnboardingUiState(
                step = OnboardingStep.FORM,
                error = OnboardingError.INVALID_NICKNAME,
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = OnboardingUiState(OnboardingStep.CREATING)
            runCatching { repository.createUser(nickname) }
                .onSuccess { profile ->
                    settings.saveUserProfile(profile.nickname, profile.friendCode)
                    _uiState.value = OnboardingUiState(
                        step = OnboardingStep.SUCCESS,
                        profile = profile,
                    )
                }
                .onFailure {
                    _uiState.value = OnboardingUiState(
                        step = OnboardingStep.ERROR,
                        error = OnboardingError.CONNECTION,
                    )
                }
        }
    }

    fun retry() {
        if (repository.authState.value == AuthState.SignedIn) checkExistingProfile()
        else repository.signIn()
    }

    private fun checkExistingProfile() {
        if (profileCheckJob?.isActive == true) return
        profileCheckJob = viewModelScope.launch {
            _uiState.value = OnboardingUiState(OnboardingStep.CHECKING)
            runCatching { repository.getCurrentUser() }
                .onSuccess { profile ->
                    if (profile == null) {
                        _uiState.value = OnboardingUiState(OnboardingStep.FORM)
                    } else {
                        settings.saveUserProfile(profile.nickname, profile.friendCode)
                        _uiState.value = OnboardingUiState(
                            step = OnboardingStep.NAVIGATE_HOME,
                            profile = profile,
                        )
                    }
                }
                .onFailure {
                    _uiState.value = OnboardingUiState(
                        step = OnboardingStep.ERROR,
                        error = OnboardingError.CONNECTION,
                    )
                }
        }
    }

    companion object {
        fun factory(repository: FirebaseRepository, settings: SettingsStore): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    OnboardingViewModel(repository, settings) as T
            }
    }
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.CHECKING,
    val profile: UserProfile? = null,
    val error: OnboardingError? = null,
)

enum class OnboardingStep { CHECKING, FORM, CREATING, SUCCESS, ERROR, NAVIGATE_HOME }
enum class OnboardingError { OFFLINE, CONNECTION, INVALID_NICKNAME }
