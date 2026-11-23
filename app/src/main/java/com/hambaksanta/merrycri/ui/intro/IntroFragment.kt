// ui/intro/IntroFragment.kt
package com.hambaksanta.merrycri.ui.intro

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.databinding.FragmentIntroBinding
import com.hambaksanta.merrycri.ui.tree.TreeArt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.hypot
import kotlin.math.max

/**
 * 앱 첫 화면. 시스템 스플래시의 편지 트리 아이콘을 그대로 이어받아,
 * 봉투 속 트리가 자라서 "내 트리"(받은 편지 수 단계)가 되는 장면을 보여준 뒤 홈으로 넘어간다.
 * 화면을 누르면 바로 넘어간다. 기기에서 애니메이션을 끈 경우 마지막 장면만 보여준다. 담당: A
 *
 * 시간표(ms): 300 흔들흔들 → 700 봉투가 내려가며 사라짐 → 1100 트리가 땅으로 내려오며 자람
 *            (1200 하늘이 둥글게 펼쳐짐) → 1800 내 트리 그림으로 바뀜 → 2200 제목·D-day → 잠시 뒤 홈
 */
class IntroFragment : Fragment() {
    private var _binding: FragmentIntroBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var animator: Animator? = null
    private var finished = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO(B): 받은 편지 수를 알면 MainActivity에서 navigate 할 때 bundleOf(IntroFragment.ARG_STAGE to 단계)로 넘겨주세요.
        //         없으면 Lv1(새 사용자)로 보여준다.
        val stage = (arguments?.getInt(ARG_STAGE, 1) ?: 1).coerceIn(1, 5)
        bindStage(stage)

        binding.layoutIntro.setOnClickListener { finish() }

        val skipAnimation = savedInstanceState != null || !ValueAnimator.areAnimatorsEnabled()
        binding.layoutIntro.doOnLayout {
            alignTreeToGround()
            if (skipAnimation) {
                showEndState()
                finishAfter(HOLD_MS)
            } else {
                playIntro()
            }
        }
    }

    private fun bindStage(stage: Int) = with(binding) {
        imageSky.setImageResource(TreeArt.skyRes(stage))
        treeScene.setStage(stage)
        treeScene.setBackgroundVisible(false)
        weather.setStage(stage)
        aurora.visibility = if (TreeArt.hasAurora(stage)) View.VISIBLE else View.GONE
        textStage.text = getString(R.string.intro_my_tree, getString(TreeArt.nameRes(stage)))
        textDday.text = ddayText()
        // Lv4·Lv5는 밤하늘이라 흰 글씨
        val color = ContextCompat.getColor(requireContext(), if (stage >= 4) R.color.white else R.color.ink)
        textTitle.setTextColor(color)
        textStage.setTextColor(color)
    }

    /**
     * 하늘 그림은 화면을 꽉 채우도록(centerCrop) 커지므로, 내 트리 밑동이 하늘 그림의 땅(y=334/380)에 닿게 내린다.
     * 키가 큰 화면일수록 트리가 아래로 내려가고 위쪽에 하늘(달·오로라)이 넓게 보인다.
     */
    private fun alignTreeToGround() = with(binding) {
        val scale = max(imageSky.width / ART_W, imageSky.height / ART_H)
        val groundY = (imageSky.height - ART_H * scale) / 2f + GROUND_Y * scale
        treeScene.translationY = groundY - (treeScene.top + treeScene.height * MY_TREE_BASE)
    }

    private fun playIntro() = with(binding) {
        val dp = resources.displayMetrics.density

        // 트리는 밑동을 기준으로 커진다. 밑동 위치: 아이콘 칸의 58.7%, 내 트리 그림의 334/380.
        imageIntroTree.pivotX = imageIntroTree.width / 2f
        imageIntroTree.pivotY = imageIntroTree.height * ICON_TREE_BASE
        treeScene.pivotX = treeScene.width / 2f
        treeScene.pivotY = treeScene.height.toFloat()
        val fromBase = imageIntroTree.top + imageIntroTree.pivotY
        val toBase = treeScene.top + treeScene.translationY + treeScene.height * MY_TREE_BASE
        val treeDrop = toBase - fromBase

        val wiggle = ObjectAnimator.ofFloat(imageIntroTree, View.ROTATION, 0f, -7f, 6f, -4f, 2f, 0f).apply {
            startDelay = 300; duration = 400
        }
        val sealPop = ObjectAnimator.ofPropertyValuesHolder(
            imageEnvelopeFront,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f, 1f),
        ).apply { startDelay = 300; duration = 220 }

        val envelopeOut = listOf(imageEnvelopeBack, imageEnvelopeFront).map { v ->
            ObjectAnimator.ofPropertyValuesHolder(
                v,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, 80 * dp),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0f),
            ).apply { startDelay = 700; duration = 500; interpolator = AccelerateDecelerateInterpolator() }
        }
        val popOut = ObjectAnimator.ofFloat(imageIntroTree, View.TRANSLATION_Y, 0f, -14 * dp, 0f).apply {
            startDelay = 700; duration = 400
        }
        val grow = ObjectAnimator.ofPropertyValuesHolder(
            imageIntroTree,
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, treeDrop),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, TREE_GROW_SCALE),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, TREE_GROW_SCALE),
        ).apply { startDelay = 1100; duration = 1000; interpolator = AccelerateDecelerateInterpolator() }

        // 하늘: 트리 자리에서 둥글게 펼쳐진다
        val cx = imageSky.width / 2
        val cy = (toBase - treeScene.height * 0.4f).toInt()
        val skyReveal = ViewAnimationUtils.createCircularReveal(
            imageSky, cx, cy, 0f, hypot(imageSky.width.toFloat(), imageSky.height.toFloat()),
        ).apply {
            startDelay = 1200; duration = 900; interpolator = AccelerateDecelerateInterpolator()
            doOnStart { imageSky.visibility = View.VISIBLE }
        }

        val treeOut = ObjectAnimator.ofFloat(imageIntroTree, View.ALPHA, 1f, 0f).apply {
            startDelay = 1800; duration = 500
        }
        val myTreeIn = ObjectAnimator.ofPropertyValuesHolder(
            treeScene,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.94f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.94f, 1f),
        ).apply { startDelay = 1800; duration = 500; interpolator = OvershootInterpolator(1.2f) }

        val snowIn = ObjectAnimator.ofFloat(weather, View.ALPHA, 0f, 1f).apply {
            startDelay = 1600; duration = 800
        }
        val auroraIn = ObjectAnimator.ofFloat(aurora, View.ALPHA, 0f, 1f).apply {
            startDelay = 1700; duration = 1000
        }

        val headerIn = ObjectAnimator.ofPropertyValuesHolder(
            layoutHeader,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 16 * dp, 0f),
        ).apply { startDelay = 2200; duration = 600; interpolator = DecelerateInterpolator() }

        animator = AnimatorSet().apply {
            playTogether(listOf(wiggle, sealPop, popOut, grow, skyReveal, treeOut, myTreeIn, snowIn, auroraIn, headerIn) + envelopeOut)
            doOnEnd { finishAfter(HOLD_MS) }
            start()
        }
    }

    /** 애니메이션 없이 마지막 장면 */
    private fun showEndState() = with(binding) {
        imageEnvelopeBack.alpha = 0f
        imageEnvelopeFront.alpha = 0f
        imageIntroTree.alpha = 0f
        imageSky.visibility = View.VISIBLE
        treeScene.alpha = 1f
        weather.alpha = 1f
        aurora.alpha = 1f
        layoutHeader.alpha = 1f
    }

    private fun finishAfter(millis: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(millis)
            finish()
        }
    }

    private fun finish() {
        if (finished || _binding == null) return
        finished = true
        animator?.removeAllListeners()
        animator?.cancel()
        val nav = findNavController()
        if (nav.currentDestination?.id == R.id.introFragment) {
            nav.popBackStack() // 아래의 온보딩이 나타나서 기존 사용자는 홈, 새 사용자는 닉네임 입력으로 보낸다
        }
    }

    private fun ddayText(): String {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val days = ChronoUnit.DAYS.between(today, LocalDate.of(today.year, 12, 25))
        return if (days > 0) getString(R.string.intro_dday, days.toInt()) else getString(R.string.intro_christmas)
    }

    override fun onDestroyView() {
        animator?.removeAllListeners()
        animator?.cancel()
        animator = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_STAGE = "stage"
        private const val HOLD_MS = 700L
        private const val TREE_GROW_SCALE = 2.6f
        private const val ICON_TREE_BASE = 0.587f
        private const val ART_W = 320f
        private const val ART_H = 380f
        private const val GROUND_Y = 334f
        private const val MY_TREE_BASE = GROUND_Y / ART_H
    }
}
