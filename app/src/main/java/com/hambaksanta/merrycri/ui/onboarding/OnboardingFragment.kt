// ui/onboarding/OnboardingFragment.kt
package com.hambaksanta.merrycri.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.databinding.FragmentOnboardingBinding
import kotlinx.coroutines.launch

/** 닉네임과 초대 코드를 준비하는 첫 화면. 담당: B */
class OnboardingFragment : Fragment() {
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val app get() = requireActivity().application as MerryCriApp
    private val viewModel by viewModels<OnboardingViewModel> {
        OnboardingViewModel.factory(app.repository, app.settings)
    }
    private var navigationHandled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.createButton.setOnClickListener {
            binding.nicknameInputLayout.error = null
            viewModel.createUser(binding.nicknameInput.text?.toString().orEmpty())
        }
        binding.retryButton.setOnClickListener { viewModel.retry() }
        binding.continueButton.setOnClickListener { navigateHome() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: OnboardingUiState) = with(binding) {
        progressIndicator.isVisible = state.step == OnboardingStep.CHECKING ||
            state.step == OnboardingStep.CREATING
        progressText.isVisible = progressIndicator.isVisible
        progressText.setText(
            if (state.step == OnboardingStep.CREATING) R.string.onboarding_creating
            else R.string.onboarding_checking,
        )
        formContent.isVisible = state.step == OnboardingStep.FORM
        successCard.isVisible = state.step == OnboardingStep.SUCCESS
        errorContent.isVisible = state.step == OnboardingStep.ERROR

        if (state.step == OnboardingStep.FORM) {
            nicknameInputLayout.error = when (state.error) {
                OnboardingError.INVALID_NICKNAME -> getString(R.string.onboarding_error_invalid_nickname)
                else -> null
            }
        }
        if (state.step == OnboardingStep.SUCCESS) {
            friendCodeText.text = state.profile?.friendCode.orEmpty()
        }
        if (state.step == OnboardingStep.ERROR) {
            errorText.setText(
                if (state.error == OnboardingError.OFFLINE) R.string.onboarding_error_offline
                else R.string.onboarding_error_connection,
            )
        }
        if (state.step == OnboardingStep.NAVIGATE_HOME) navigateHome()
    }

    private fun navigateHome() {
        if (navigationHandled) return
        navigationHandled = true
        findNavController().navigate(
            R.id.navigation_tree,
            null,
            navOptions {
                popUpTo(R.id.onboardingFragment) { inclusive = true }
                launchSingleTop = true
            },
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
