// ui/friends/FriendsFragment.kt
package com.hambaksanta.merrycri.ui.friends

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
import com.google.android.material.snackbar.Snackbar
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.databinding.FragmentFriendsBinding
import kotlinx.coroutines.launch

/** 친구 목록. 담당: B */
class FriendsFragment : Fragment() {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val app get() = requireActivity().application as MerryCriApp
    private val viewModel by viewModels<FriendsViewModel> {
        FriendsViewModel.factory(app.repository)
    }
    private val friendAdapter = FriendAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFriendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.friendList.adapter = friendAdapter
        binding.addFriendButton.setOnClickListener {
            binding.friendCodeInputLayout.error = null
            viewModel.addFriend(binding.friendCodeInput.text?.toString().orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    private fun render(state: FriendsUiState) = with(binding) {
        loadingIndicator.isVisible = state.isLoading
        addFriendButton.isEnabled = !state.isAdding
        friendCodeInput.isEnabled = !state.isAdding
        friendList.isVisible = !state.isLoading && state.friends.isNotEmpty()
        emptyText.isVisible = !state.isLoading && state.friends.isEmpty() && state.error == null
        connectionErrorText.isVisible = state.error == FriendsError.CONNECTION
        friendAdapter.submitList(state.friends)
    }

    private fun handleEvent(event: FriendsEvent) {
        when (event) {
            is FriendsEvent.Added -> {
                binding.friendCodeInput.text?.clear()
                Snackbar.make(
                    binding.root,
                    getString(R.string.friends_add_success, event.nickname),
                    Snackbar.LENGTH_SHORT,
                ).show()
            }
            is FriendsEvent.Error -> {
                val message = when (event.reason) {
                    FriendsError.INVALID_CODE -> R.string.friends_error_invalid_code
                    FriendsError.NOT_FOUND -> R.string.friends_error_not_found
                    FriendsError.SELF -> R.string.friends_error_self
                    FriendsError.ALREADY_ADDED -> R.string.friends_error_already_added
                    FriendsError.CONNECTION -> R.string.friends_error_connection
                }
                binding.friendCodeInputLayout.error = getString(message)
            }
        }
    }

    override fun onDestroyView() {
        binding.friendList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
