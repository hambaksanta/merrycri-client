// ui/tree/TreeFragment.kt
package com.hambaksanta.merrycri.ui.tree

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hambaksanta.merrycri.databinding.FragmentTreeBinding

/**
 * 내 트리 홈. 담당: A
 * 지금은 트리 장면(TreeSceneView)만 보여준다. 받은 편지 수·D-day·버튼은 "홈 화면 조립" 작업에서 연결한다.
 * TODO(A): 임시 미리보기(길게 눌러 단계 바꾸기, 눌러서 우체통 바꾸기)는 실제 데이터 연결 후 지운다.
 */
class TreeFragment : Fragment() {
    private var _binding: FragmentTreeBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var previewStage = 1
    private var previewMailboxFull = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewStage = savedInstanceState?.getInt(KEY_STAGE, 1) ?: 1
        showStage(previewStage)
        binding.treeScene.setOnClickListener {
            previewMailboxFull = !previewMailboxFull
            binding.treeScene.setMailboxFull(previewMailboxFull)
        }
        binding.treeScene.setOnLongClickListener {
            previewStage = previewStage % 5 + 1
            showStage(previewStage)
            true
        }
    }

    private fun showStage(stage: Int) = with(binding) {
        treeScene.setStage(stage)
        // 미리보기: 그 단계 장식 자리를 다 채워서 보여준다
        val count = TreeArt.SLOTS[stage].orEmpty().size
        treeScene.setOrnaments(List(count) { TreeArt.ORNAMENT_TYPES[it % TreeArt.ORNAMENT_TYPES.size] })
        textStage.setText(TreeArt.nameRes(stage))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_STAGE, previewStage)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val KEY_STAGE = "preview_stage"
    }
}
