// ui/letter/FriendTreeFragment.kt
package com.hambaksanta.merrycri.ui.letter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.data.model.Ornament
import com.hambaksanta.merrycri.databinding.FragmentFriendTreeBinding
import com.hambaksanta.merrycri.ui.tree.TreeArt
import kotlinx.coroutines.launch

/**
 * 친구 트리를 구경하는 화면. 친구 목록에서 카드를 탭하면 들어오고,
 * '← 뒤로가기'를 누르면 친구 목록으로 돌아간다(popBackStack). 담당: C
 */
class FriendTreeFragment : Fragment() {
    private var _binding: FragmentFriendTreeBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val app get() = requireActivity().application as MerryCriApp
    private val ownerUid get() = arguments?.getString(ARG_OWNER_UID).orEmpty()
    private val ownerNickname get() = arguments?.getString(ARG_OWNER_NICKNAME).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFriendTreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.friendTreeTitle.text = getString(R.string.friend_tree_title_format, ownerNickname)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.repository.observeOrnaments(ownerUid).collect(::render)
            }
        }
    }

    private fun render(ornaments: List<Ornament>) {
        val letterCount = ornaments.size
        val stage = TreeArt.stageOf(letterCount)
        binding.friendTreeScene.setStage(stage)
        binding.friendTreeScene.setOrnaments(ornaments.sortedBy(Ornament::slot).map(Ornament::type))
        binding.friendTreeStageText.setText(TreeArt.nameRes(stage))
        binding.friendTreeLetterCountText.text = getString(R.string.friends_letter_count, letterCount)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_OWNER_UID = "owner_uid"
        const val ARG_OWNER_NICKNAME = "owner_nickname"
    }
}
