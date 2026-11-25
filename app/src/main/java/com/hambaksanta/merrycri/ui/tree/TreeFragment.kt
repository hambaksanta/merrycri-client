// ui/tree/TreeFragment.kt
package com.hambaksanta.merrycri.ui.tree

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.data.AuthState
import com.hambaksanta.merrycri.data.model.Letter
import com.hambaksanta.merrycri.data.model.Ornament
import com.hambaksanta.merrycri.databinding.FragmentTreeBinding
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * 내 트리 홈. 담당: A
 *
 * 첫 화면처럼 단계별 하늘이 화면을 꽉 채우고, 트리 밑동이 하늘 그림의 땅에 닿게 놓는다.
 * - 내가 받은 장식(ornaments/{내 uid})을 실시간으로 받아 트리 단계·장식·진행바를 바꾼다
 * - [받은 편지] 버튼, 우체통, 트리의 장식을 누르면 편지 시트(TreeLettersSheet)가 열린다
 *   12/25(config/openAt) 전에는 서버가 읽기를 막으므로 잠금 안내를, 그 뒤에는 편지 목록을 보여준다
 * - 상태바 색도 하늘 맨 위 색에 맞춘다 (다른 탭으로 가면 원래대로)
 */
class TreeFragment : Fragment() {
    private var _binding: FragmentTreeBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val app get() = requireActivity().application as MerryCriApp

    /** slot 순서로 정렬한 내 장식 = 받은 편지 */
    private var ornaments: List<Ornament> = emptyList()
    private var stage = 0

    /** 한 번 열어 본 편지 목록. 12/25 전이면 null (읽을 수 없음) */
    private var letters: List<Letter>? = null
    private var sheet: TreeLettersSheet? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 다른 탭에 갔다 오면 화면(View)만 새로 만들어지므로 단계·편지 기억도 처음부터
        stage = 0
        letters = null
        binding.treeScene.setBackgroundVisible(false)
        binding.textDday.text = ddayText()
        binding.buttonLetters.setOnClickListener { openLetters(null) }
        binding.treeScene.setOnMailboxClick { openLetters(null) }

        // 하늘을 상태바 뒤까지 그리는 기기(안드로이드 15 이상)에서는 머리글을 상태바 아래로 내린다
        val headerTop = (HEADER_TOP_DP * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutTree) { _, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            binding.layoutHeader.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = headerTop + top }
            insets
        }
        binding.layoutTree.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> alignToGround() }

        render(emptyList())
        viewLifecycleOwner.lifecycleScope.launch {
            val repository = app.repository
            // 익명 로그인이 끝날 때까지 기다린다. 오프라인이면 빈 트리(Lv1)로 둔다
            val auth = repository.authState.first { it != AuthState.Loading }
            val uid = repository.uid ?: FirebaseAuth.getInstance().currentUser?.uid
            if (auth != AuthState.SignedIn || uid == null) return@launch
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeOrnaments(uid)
                    .catch { emit(emptyList()) }
                    .collect { render(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applySystemBars()
    }

    override fun onPause() {
        // 다른 탭(크림색 바탕)으로 갈 때 상태바를 앱 기본값으로 되돌린다
        val window = requireActivity().window
        @Suppress("DEPRECATION")
        window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.cream)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        super.onPause()
    }

    // ---------------------------------------------------------------- 화면 그리기

    private fun render(list: List<Ornament>) = with(binding) {
        ornaments = list.sortedBy { it.slot }
        val count = ornaments.size
        val newStage = TreeArt.stageOf(count)
        if (newStage != stage) {
            stage = newStage
            applyStage(newStage)
        }
        treeScene.setOrnaments(ornaments.map { it.type }) { index -> openLetters(ornaments.getOrNull(index)) }
        treeScene.setMailboxFull(count > 0 && letters?.any { !it.opened } != false)

        textStage.setText(TreeArt.nameRes(newStage))
        textMessage.setText(TreeArt.messageRes(newStage))
        val toNext = TreeArt.lettersToNext(count)
        progressStage.isVisible = toNext > 0
        progressStage.setProgressCompat((TreeArt.progressInStage(count) * 100).toInt(), true)
        textProgress.text = if (toNext > 0) getString(R.string.tree_letters_to_next, toNext) else getString(R.string.tree_max_stage)
        buttonLetters.text = if (count > 0) getString(R.string.tree_letters_button, count) else getString(R.string.tree_letters_button_empty)
    }

    /** 단계에 따라 하늘·날씨·글자색·상태바를 바꾼다 */
    private fun applyStage(s: Int) = with(binding) {
        imageSky.setImageResource(TreeArt.skyRes(s))
        layoutTree.setBackgroundColor(SKY_TOP[s])
        aurora.isVisible = TreeArt.hasAurora(s)
        weather.setStage(s)
        treeScene.setStage(s)

        val onDark = s >= 4
        val text = ContextCompat.getColor(requireContext(), if (onDark) R.color.white else R.color.ink)
        val sub = ContextCompat.getColor(requireContext(), if (onDark) R.color.night_text_muted else R.color.ink_muted)
        imageTitle.imageTintList = ColorStateList.valueOf(text)
        textStage.setTextColor(text)
        textMessage.setTextColor(sub)
        textProgress.setTextColor(text)
        if (isResumed) applySystemBars()
    }

    private fun applySystemBars() {
        if (stage == 0) return
        val window = requireActivity().window
        @Suppress("DEPRECATION")
        window.statusBarColor = SKY_TOP[stage]
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = stage <= 3
    }

    /** 트리 밑동이 하늘 그림의 땅(y=334/380)에 닿게 (하늘은 화면을 꽉 채우도록 centerCrop) */
    private fun alignToGround() {
        val b = _binding ?: return
        val sky = b.imageSky
        val scene = b.treeScene
        if (sky.width == 0 || scene.height == 0) return
        val scale = max(sky.width / ART_W, sky.height / ART_H)
        val groundY = (sky.height - ART_H * scale) / 2f + GROUND_Y * scale
        scene.translationY = groundY - (scene.top + scene.height * TreeArt.TREE_BASE_Y)
    }

    // ---------------------------------------------------------------- 편지

    /** 받은 편지 시트를 연다. [target]이 있으면(장식을 누름) 그 장식의 편지를 펼쳐 보여준다 */
    private fun openLetters(target: Ornament?) {
        if (sheet?.isShowing == true) return
        val count = ornaments.size
        val s = TreeLettersSheet(
            context = requireContext(),
            onOpenLetter = ::markOpened,
        ).also { sheet = it }
        s.setOnDismissListener { if (sheet === s) sheet = null }
        s.show()

        if (count == 0) {
            s.showEmpty()
            return
        }
        letters?.let { cached ->
            s.showLetters(cached, findLetter(cached, target))
            return
        }
        s.showLoading(count)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { app.repository.getReceivedLetters() }
                .onSuccess { list ->
                    letters = list
                    if (sheet === s) s.showLetters(list, findLetter(list, target))
                    render(ornaments)
                }
                .onFailure { error ->
                    if (sheet !== s) return@onFailure
                    if (error.isPermissionDenied()) s.showLocked(count, daysToChristmas(), target?.type) else s.showError()
                }
        }
    }

    /** 장식 → 편지: 편지와 장식은 한 번에 저장돼 만든 시각(서버 시간)이 같다 */
    private fun findLetter(list: List<Letter>, target: Ornament?): Letter? {
        target ?: return null
        return list.firstOrNull { it.createdAt == target.createdAt }
            ?: list.filter { it.ornamentType == target.type }.minByOrNull { kotlin.math.abs(it.createdAt - target.createdAt) }
    }

    private fun markOpened(letter: Letter) {
        if (letter.opened) return
        letters = letters?.map { if (it.id == letter.id) it.copy(opened = true) else it }
        render(ornaments)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { app.repository.markLetterOpened(letter.id) }
        }
    }

    /** 12/25 전에는 보안 규칙이 letters 읽기를 막는다 → 'Permission denied' */
    private fun Throwable.isPermissionDenied(): Boolean =
        generateSequence(this) { it.cause }.any { it.message?.contains("ermission denied") == true }

    private fun daysToChristmas(): Long {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        return ChronoUnit.DAYS.between(today, LocalDate.of(today.year, 12, 25))
    }

    private fun ddayText(): String {
        val days = daysToChristmas()
        return if (days > 0) getString(R.string.intro_dday, days.toInt()) else getString(R.string.intro_christmas)
    }

    override fun onDestroyView() {
        sheet?.dismiss()
        sheet = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val ART_W = 320f
        const val ART_H = 380f
        const val GROUND_Y = 334f
        const val HEADER_TOP_DP = 28 // fragment_tree.xml의 layout_header marginTop과 같게
        /** 단계별 하늘 그림(bg_sky_lvN) 맨 위 색 — 상태바·바탕색 */
        val SKY_TOP = intArrayOf(
            0xFF86929F.toInt(),
            0xFF86929F.toInt(),
            0xFF9DB8D0.toInt(),
            0xFF8C9FCC.toInt(),
            0xFF3C3A72.toInt(),
            0xFF0E1536.toInt(),
        )
    }
}
