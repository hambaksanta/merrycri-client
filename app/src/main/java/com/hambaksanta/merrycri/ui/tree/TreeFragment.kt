// ui/tree/TreeFragment.kt
package com.hambaksanta.merrycri.ui.tree

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.databinding.FragmentTreeBinding
import kotlinx.coroutines.launch

/**
 * 내 트리 홈. 담당: A
 * 트리 장면(TreeSceneView)과 내 친구 코드 카드(보여주기·복사·공유, #45)를 보여준다.
 * 받은 편지 수·D-day·버튼은 "홈 화면 조립" 작업에서 연결한다.
 * TODO(A): 임시 미리보기(길게 눌러 단계 바꾸기, 눌러서 우체통 바꾸기)는 실제 데이터 연결 후 지운다.
 */
class TreeFragment : Fragment() {
    private var _binding: FragmentTreeBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var previewStage = 1
    private var previewMailboxFull = false

    /** 가입할 때 저장된 내 친구 코드. 가입 전이면 null (카드 숨김) */
    private var myCode: String? = null

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

        binding.textMyCode.setOnClickListener { copyMyCode() }
        binding.buttonCopyCode.setOnClickListener { copyMyCode() }
        binding.buttonShareCode.setOnClickListener { shareMyCode() }
        val settings = (requireActivity().application as MerryCriApp).settings
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.friendCode.collect { code ->
                    myCode = code?.takeIf { it.isNotBlank() }
                    binding.cardMyCode.isVisible = myCode != null
                    binding.textMyCode.text = myCode.orEmpty()
                }
            }
        }
    }

    /** 내 친구 코드를 클립보드에 복사 */
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
