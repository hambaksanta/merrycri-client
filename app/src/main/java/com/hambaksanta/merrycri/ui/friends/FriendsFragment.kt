// ui/friends/FriendsFragment.kt
package com.hambaksanta.merrycri.ui.friends

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
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

/** 친구 목록. 담당: B (위쪽 '내 친구 코드' 카드만 A, #45) */
class FriendsFragment : Fragment() {
    private var _binding: FragmentFriendsBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val app get() = requireActivity().application as MerryCriApp
    private val viewModel by viewModels<FriendsViewModel> {
        FriendsViewModel.factory(app.repository)
    }
    private val friendAdapter = FriendAdapter()

    /** 가입할 때 저장된 내 친구 코드. 가입 전이면 null (카드 숨김) */
    private var myCode: String? = null

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

        binding.myCodeArea.setOnClickListener { copyMyCode() }
        binding.buttonCopyCode.setOnClickListener { copyMyCode() }
        binding.buttonShareCode.setOnClickListener { shareMyCode() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handleEvent) }
                launch {
                    app.settings.friendCode.collect { code ->
                        myCode = code?.takeIf { it.isNotBlank() }
                        binding.cardMyCode.isVisible = myCode != null
                        binding.textMyCode.text = myCode.orEmpty()
                    }
                }
            }
        }
    }

    /** 내 친구 코드를 클립보드에 복사 (담당: A, #45) */
    private fun copyMyCode() {
        val code = myCode ?: return
        val clipboard = requireContext().getSystemService<ClipboardManager>() ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.my_code_clip_label), code))
        // Android 13부터는 시스템이 "복사됨" 알림을 직접 띄우므로 겹치지 않게 그 아래 버전에서만 알린다
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Snackbar.make(binding.root, R.string.my_code_copied, Snackbar.LENGTH_SHORT).show()
        }
    }

    /** 공유 시트로 친구 코드 안내 문구를 보낸다 (카카오톡·문자 등 사용자가 고름) */
    private fun shareMyCode() {
        val code = myCode ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.my_code_share_text, code))
        }
        startActivity(Intent.createChooser(send, getString(R.string.my_code_share_title)))
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
