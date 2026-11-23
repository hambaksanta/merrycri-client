// ui/leaderboard/LeaderboardFragment.kt
package com.hambaksanta.merrycri.ui.leaderboard

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.databinding.FragmentLeaderboardBinding
import com.hambaksanta.merrycri.ui.game.GameActivity
import kotlinx.coroutines.launch

/**
 * 친구 트리 하나의 미니게임 점수 순위 화면. 담당: C
 * [ARG_OWNER_UID]로 전달된 트리 주인의 리더보드를 보여주고, '미니게임으로 도전하기'를 누르면
 * [GameActivity.forOwner]로 그 트리 경쟁 모드를 바로 시작한다 — 끝나면 이 화면 점수도 갱신된다.
 */
class LeaderboardFragment : Fragment() {
    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val app get() = requireActivity().application as MerryCriApp
    private val ownerUid get() = arguments?.getString(ARG_OWNER_UID).orEmpty()
    private val ownerNickname get() = arguments?.getString(ARG_OWNER_NICKNAME).orEmpty()
    private val viewModel by viewModels<LeaderboardViewModel> {
        LeaderboardViewModel.factory(app.repository, ownerUid)
    }
    private val adapter by lazy { ScoreAdapter(viewModel.myUid) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.leaderboardTitle.text = getString(R.string.leaderboard_title_format, ownerNickname)
        binding.scoreRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.scoreRecycler.adapter = adapter
        binding.playButton.setOnClickListener {
            startActivity(GameActivity.forOwner(requireContext(), ownerUid))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: LeaderboardUiState) {
        adapter.submitList(state.scores)
        binding.scoreRecycler.isVisible = state.scores.isNotEmpty()
        binding.leaderboardEmptyText.isVisible = !state.loading && state.scores.isEmpty()
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
