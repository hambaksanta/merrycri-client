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
import com.hambaksanta.merrycri.ui.game.GameActivity
import kotlinx.coroutines.launch

/**
 * 친구 목록. 담당: B
 * 각 친구 트리에서 지금까지 나온 미니게임 1등 점수를 같이 보여주고(담당: C),
 * 탭하면 그 친구 트리 경쟁 모드로 GameActivity를 바로 띄운다(상단 큰 시작 버튼 + 하단 전체 순위) — 약간의 경쟁심리 자극용.
 */
class FriendsFragment : Fragment() {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val app get() = requireActivity().application as MerryCriApp
    private val viewModel by viewModels<FriendsViewModel> {
        FriendsViewModel.factory(app.repository)
    }
    private val friendAdapter = FriendAdapter(onClick = ::onFriendClicked)

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

    private fun onFriendClicked(item: FriendListItem) {
        // '게임쪽'에 바로 띄움: GameActivity 시작 화면 상단 큰 버튼 + 하단 전체 순위로 바로 들어간다.
        startActivity(GameActivity.forOwner(requireContext(), item.uid))
    }

    override fun onDestroyView() {
        binding.friendList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
