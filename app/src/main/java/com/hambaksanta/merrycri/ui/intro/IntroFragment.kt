// ui/intro/IntroFragment.kt
package com.hambaksanta.merrycri.ui.intro

import android.animation.ValueAnimator
import android.graphics.Outline
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.databinding.FragmentIntroBinding
import com.hambaksanta.merrycri.ui.tree.TreeArt
import com.hambaksanta.merrycri.ui.tree.TreeSceneView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * 앱 첫 화면. 담당: A
 *
 * 1. 시스템 스플래시의 편지 트리 아이콘에서 이어서, 봉투 속 트리만 흔들 → 봉투는 제자리에서 사라지고 트리가 점프해 땅에 뿌리내림 (Lv1)
 * 2. 우체통에서 편지가 한 통씩 날아와 트리에 톡 맞을 때마다: 트리가 움찔 → 쭉 늘어나며 한 단계 자람 (단계마다 편지 한 통)
 * 3. 다 자라면 장식이 톡톡 달리고, 타이틀·D-day·안내가 올라온다
 *
 * - 신규 사용자(기기에 닉네임 없음): Lv5까지 시연 → "내 트리 만들기" → 닫히면 온보딩
 * - 기존 사용자: 내가 받은 편지 수의 단계까지 → 잠시 뒤 자동으로 홈
 * 움직임은 웹 시안과 같은 시간표를 render(t) 한 곳에서 그린다. 화면을 누르면 바로 넘어간다.
 * 기기에서 애니메이션을 끈 경우 마지막 장면만 보여준다.
 */
class IntroFragment : Fragment() {
    private var _binding: FragmentIntroBinding? = null
    private val binding get() = requireNotNull(_binding)

    /** 이번 인트로에서 보여줄 것 */
    private data class Plan(val newUser: Boolean, val stage: Int, val letterCount: Int, val ornamentTypes: List<String>)

    private lateinit var plan: Plan
    private var timeline: ValueAnimator? = null
    private var finished = false
    private var ornamentsRevealed = false

    // 레이아웃이 잡힌 뒤 계산하는 값 (px)
    private var dp = 1f
    private var groundY = 0f
    private var iconBaseY = 0f
    private var skyRevealRadius = 0f

    // 시간표 (ms)
    private var dep = LongArray(0)
    private var hit = LongArray(0)
    private var tEnd = 0L
    private var tHeader = 0L
    private var total = 0L

    // 단계별로 어느 장면·하늘 뷰가 맡고 있는지 (홀짝으로 번갈아 씀)
    private val sceneStage = IntArray(2)
    private val skyStage = IntArray(2)
    private var mailboxFull: Boolean? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dp = resources.displayMetrics.density
        binding.layoutIntro.setOnClickListener { finish() }
        binding.buttonStart.setOnClickListener { finish() }
        binding.textDday.text = ddayText()
        listOf(binding.sceneA, binding.sceneB).forEach { it.setBackgroundVisible(false) }

        val skipAnimation = savedInstanceState != null || !ValueAnimator.areAnimatorsEnabled()
        viewLifecycleOwner.lifecycleScope.launch {
            plan = loadPlan() // 봉투(스플래시와 같은 장면)가 보이는 동안 불러옴, 최대 약 1.2초
            val b = _binding ?: return@launch
            b.layoutIntro.doOnLayout {
                if (_binding == null) return@doOnLayout
                prepare()
                if (skipAnimation) showEndState() else play()
            }
        }
    }

    // ---------------------------------------------------------------- 데이터

    private suspend fun loadPlan(): Plan {
        val app = requireActivity().application as MerryCriApp
        val nickname = withTimeoutOrNull(1500) { app.settings.nickname.first() }
        if (nickname.isNullOrBlank()) {
            val demo = List(TreeArt.SLOTS[5].orEmpty().size) { TreeArt.ORNAMENT_TYPES[it % TreeArt.ORNAMENT_TYPES.size] }
            return Plan(newUser = true, stage = 5, letterCount = demo.size, ornamentTypes = demo)
        }
        val ornaments = withTimeoutOrNull(1200) {
            runCatching {
                val uid = app.repository.uid ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@runCatching null
                app.repository.observeOrnaments(uid).first()
            }.getOrNull()
        }.orEmpty()
        val count = ornaments.size
        return Plan(
            newUser = false,
            stage = TreeArt.stageOf(count),
            letterCount = count,
            ornamentTypes = ornaments.sortedBy { it.slot }.map { it.type },
        )
    }

    // ---------------------------------------------------------------- 준비

    private fun prepare() = with(binding) {
        val s = plan.stage
        // 트리 밑동이 하늘 그림의 땅(y=334/380)에 닿게 (하늘은 화면을 꽉 채우도록 centerCrop)
        val scale = max(layoutSkies.width / ART_W, layoutSkies.height / ART_H)
        groundY = (layoutSkies.height - ART_H * scale) / 2f + GROUND_Y * scale
        listOf(sceneA, sceneB).forEach {
            it.translationY = groundY - (it.top + it.height * TreeArt.TREE_BASE_Y)
            it.pivotX = it.width / 2f
            it.pivotY = it.height * TreeArt.TREE_BASE_Y
        }
        imageIntroTree.pivotX = imageIntroTree.width / 2f
        imageIntroTree.pivotY = imageIntroTree.height * ICON_TREE_BASE
        iconBaseY = imageIntroTree.top + imageIntroTree.pivotY
        skyRevealRadius = hypot(layoutSkies.width.toFloat(), layoutSkies.height.toFloat())

        // 편지 시간표: 올라갈 단계 수만큼 한 통씩
        val n = s - 1
        val gap = if (plan.newUser) 720L else 820L
        dep = LongArray(n) { LETTER_START + it * gap }
        hit = LongArray(n) { dep[it] + FLY }
        tEnd = if (n > 0) stageStart(s) + 420 else LETTER_START + 50
        tHeader = tEnd + if (plan.newUser) 300 else 250
        total = tHeader + 900

        weather.setStage(s)
        aurora.isVisible = TreeArt.hasAurora(s)
        val onDark = s >= 4
        val color = ContextCompat.getColor(requireContext(), if (onDark) R.color.white else R.color.ink)
        imageTitle.imageTintList = android.content.res.ColorStateList.valueOf(color)
        textStage.setTextColor(color)
        textStage.text = if (plan.newUser) {
            getString(R.string.intro_new_user)
        } else {
            getString(R.string.intro_existing_user, getString(TreeArt.nameRes(s)), plan.letterCount)
        }
        buttonStart.isVisible = plan.newUser

        // Lv1 하늘은 착지한 자리에서 둥글게 펼쳐짐
        imageSkyB.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val r = revealR.toInt()
                val cx = view.width / 2
                val cy = (groundY - 45 * dp).toInt()
                outline.setOval(cx - r, cy - r, cx + r, cy + r)
            }
        }
        imageSkyB.clipToOutline = true
        assignSky(1)
        assignScene(1)
        setMailbox(true)
    }

    private var revealR = 0f

    private fun stageStart(stage: Int): Long = hit[stage - 2] + 130 // stage ≥ 2

    // ---------------------------------------------------------------- 재생

    private fun play() {
        timeline = ValueAnimator.ofFloat(0f, total.toFloat()).apply {
            duration = total
            interpolator = LinearInterpolator()
            addUpdateListener { render((it.animatedValue as Float).toLong()) }
            doOnEnd { if (!finished && !plan.newUser) finishAfter(HOLD_MS) }
            start()
        }
    }

    /** 시간 t(ms)의 장면을 그린다. 웹 시안(scene.html 의 C='G')과 같은 식. */
    private fun render(t: Long) = with(binding) {
        val s = plan.stage

        // 1) 봉투 속 트리만 흔들 → 트리가 살짝 튀어오르는 동안 봉투는 제자리에서 사라짐 (봉투는 움직이지 않는다)
        val w = cl(t, 300, 650)
        val iconRot = sin(w * PI * 3).toFloat() * 7f * (1 - w)
        val e = ad(cl(t, 650, 950))
        listOf(imageEnvelopeBack, imageEnvelopeFront).forEach { it.alpha = 1 - e }
        val pop = -30 * dp * eo(cl(t, 650, 950))

        // 2) 트리가 포물선으로 점프해 땅에 착지 → 꾹 눌렸다 펴짐
        val j = cl(t, 950, 1500)
        val startBase = iconBaseY - 30 * dp
        val base = if (t < 950) iconBaseY + pop else startBase + (groundY - startBase) * j * j - 120 * dp * sin(PI * j).toFloat()
        var sx = 1f; var sy = 1f
        val sq = cl(t, 1500, 1720)
        if (sq > 0f && sq < 1f) { val b = sin(PI * sq).toFloat(); sx = 1 + 0.14f * b; sy = 1 - 0.16f * b }
        imageIntroTree.translationY = base - iconBaseY
        imageIntroTree.rotation = iconRot
        imageIntroTree.scaleX = sx; imageIntroTree.scaleY = sy

        // 3) 하늘 펼침 + 봉투 트리 → Lv1 묘목
        revealR = skyRevealRadius * ad(cl(t, 1450, 2100))
        imageSkyB.invalidateOutline()
        if (skyStage[1] == 1) {
            imageSkyB.visibility = if (t >= 1450) View.VISIBLE else View.INVISIBLE
            imageSkyB.alpha = 1f // Lv1 하늘은 투명도 대신 원이 커지며 드러남
        }
        val x = ad(cl(t, 1600, 1900))
        imageIntroTree.alpha = 1 - x
        weather.alpha = cl(t, 1600, 2400)

        // 4) 단계별 장면 알파 (단계마다 짧게 겹치며 바뀜)
        val alpha = FloatArray(6)
        alpha[1] = if (t < 1900) x else if (s > 1) 1 - cl(t, stageStart(2), stageStart(2) + 180) else 1f
        for (st in 2..s) {
            var a = cl(t, stageStart(st), stageStart(st) + 180)
            if (st < s) a *= 1 - cl(t, stageStart(st + 1), stageStart(st + 1) + 180)
            alpha[st] = a
        }
        // 다음 단계가 시작되기 직전에 그 단계를 맡을 뷰를 준비 (가장 최근 단계만 — 앞 단계로 되돌리지 않게)
        val upcoming = (2..s).lastOrNull { t >= stageStart(it) - 40 }
        if (upcoming != null) { assignScene(upcoming); assignSky(upcoming) }
        for (st in 2..s) if (skyStage[st % 2] == st) skyView(st).apply {
            visibility = View.VISIBLE; this.alpha = cl(t, stageStart(st), stageStart(st) + 300)
        }
        listOf(sceneA, sceneB).forEachIndexed { i, v -> v.alpha = alpha.getOrElse(sceneStage[i]) { 0f } }
        if (TreeArt.hasAurora(s)) aurora.alpha = cl(t, stageStart(5), stageStart(5) + 700)

        // 5) 트리 몸짓: 편지가 맞으면 움찔 → 쭉 늘어났다 탄력 있게 자리 잡음 (마지막은 크게 흔들)
        var tsx = 1f; var tsy = 1f; var trot = 0f
        if (t < 1900) { val k = 0.9f + 0.1f * x; tsx = k; tsy = k }
        val lh = hit.lastOrNull { t >= it }
        if (lh != null) {
            val d = (t - lh).toFloat()
            val isFinal = lh == hit.last()
            if (d < 130) {
                val q = sin(PI * (d / 130f) * 0.5).toFloat(); tsy = 1 - 0.12f * q; tsx = 1 + 0.07f * q; trot = -3 * q
            } else {
                val dd = d - 130
                val amp = if (isFinal) 0.15f else 0.11f
                val sp = exp(-dd / (if (isFinal) 240f else 170f)) * cos(dd / (if (isFinal) 70f else 58f))
                tsy = 1 + amp * sp; tsx = 1 - amp * 0.55f * sp
                trot = (if (isFinal) 4.5f else 2.5f) * exp(-dd / (if (isFinal) 420f else 220f)) * sin(dd / (if (isFinal) 95f else 72f))
            }
        }
        listOf(sceneA, sceneB).forEach { it.scaleX = tsx; it.scaleY = tsy; it.rotation = trot }

        // 6) 편지: 지금 단계의 우체통 → 트리 몸통으로 날아가 톡, 맞으면 쏙 사라짐
        var letterShown = false
        for (i in dep.indices) {
            val q = cl(t, dep[i], hit[i])
            val st = i + 1
            if (q > 0f && q < 1f) {
                val (mx, my) = scenePoint(MAILBOX_X[st] + 20f, MAILBOX_GY[st] - 74f)
                val (ex, ey) = scenePoint(ART_W / 2f, TRUNK_Y[st])
                placeLetter(mx + (ex - mx) * ad(q), my + (ey - my) * q - 50 * dp * sin(PI * q).toFloat(), -15 + 30 * q, 1f, 1f)
                letterShown = true
            } else if (t >= hit[i] && t < hit[i] + 90) {
                val (ex, ey) = scenePoint(ART_W / 2f, TRUNK_Y[st])
                val k = 1 - (t - hit[i]) / 90f
                placeLetter(ex, ey, 15f, k, k)
                letterShown = true
            }
        }
        imageLetter.visibility = if (letterShown) View.VISIBLE else View.INVISIBLE
        setMailbox(dep.isNotEmpty() && t < dep.last())

        // 7) 다 자란 뒤 장식 톡톡 + 타이틀
        if (t >= tEnd && !ornamentsRevealed) revealOrnaments()
        val h = eo(cl(t, tHeader, tHeader + 600))
        layoutHeader.alpha = h; layoutHeader.translationY = 14 * dp * (1 - h)
        if (plan.newUser) {
            val bq = eo(cl(t, tHeader + 300, tHeader + 900))
            buttonStart.alpha = bq; buttonStart.translationY = 16 * dp * (1 - bq)
        }
    }

    /** 애니메이션 없이 마지막 장면 */
    private fun showEndState() = with(binding) {
        val s = plan.stage
        listOf(imageEnvelopeBack, imageEnvelopeFront, imageIntroTree).forEach { it.alpha = 0f }
        revealR = skyRevealRadius; imageSkyB.invalidateOutline()
        assignSky(s); assignScene(s)
        skyView(s).apply { visibility = View.VISIBLE; alpha = 1f }
        listOf(sceneA, sceneB).forEachIndexed { i, v -> v.alpha = if (sceneStage[i] == s) 1f else 0f }
        if (TreeArt.hasAurora(s)) aurora.alpha = 1f
        weather.alpha = 1f
        setMailbox(false)
        sceneFor(s).setOrnaments(plan.ornamentTypes.take(TreeArt.SLOTS[s].orEmpty().size))
        ornamentsRevealed = true
        layoutHeader.alpha = 1f
        if (plan.newUser) {
            buttonStart.alpha = 1f
        } else {
            finishAfter(HOLD_MS)
        }
    }

    // ---------------------------------------------------------------- 도우미

    private fun skyView(stage: Int): ImageView = if (stage % 2 == 1) binding.imageSkyB else binding.imageSkyA
    private fun sceneFor(stage: Int): TreeSceneView = if (stage % 2 == 1) binding.sceneB else binding.sceneA

    private fun assignSky(stage: Int) {
        val i = stage % 2
        if (skyStage[i] == stage) return
        skyStage[i] = stage
        skyView(stage).apply {
            setImageResource(TreeArt.skyRes(stage))
            alpha = 0f
            if (stage > 1) clipToOutline = false
            bringToFront() // 새 하늘이 앞 단계 하늘 위로 덮이게
        }
    }

    private fun assignScene(stage: Int) {
        val i = stage % 2
        if (sceneStage[i] == stage) return
        sceneStage[i] = stage
        sceneFor(stage).apply {
            setStage(stage)
            setOrnaments(emptyList())
            mailboxFull?.let { setMailboxFull(it) }
        }
    }

    private fun setMailbox(full: Boolean) {
        if (mailboxFull == full) return
        mailboxFull = full
        binding.sceneA.setMailboxFull(full)
        binding.sceneB.setMailboxFull(full)
    }

    private fun revealOrnaments() {
        ornamentsRevealed = true
        val s = plan.stage
        val scene = sceneFor(s)
        scene.setOrnaments(plan.ornamentTypes.take(TreeArt.SLOTS[s].orEmpty().size))
        scene.revealOrnaments(if (plan.newUser) 45L else 90L)
    }

    /** 트리 그림(320x380) 좌표 → 화면 좌표 */
    private fun scenePoint(ax: Float, ay: Float): Pair<Float, Float> {
        val v = binding.sceneA
        return (v.left + ax / ART_W * v.width) to (v.top + v.translationY + ay / ART_H * v.height)
    }

    private fun placeLetter(cx: Float, cy: Float, rot: Float, sx: Float, sy: Float) = with(binding.imageLetter) {
        translationX = cx - width / 2f - left
        translationY = cy - height / 2f - top
        rotation = rot; scaleX = sx; scaleY = sy
        alpha = if (sx < 1f) sx else 1f
    }

    private fun finishAfter(millis: Long) {
        viewLifecycleOwner.lifecycleScope.launch { delay(millis); finish() }
    }

    private fun finish() {
        if (finished || _binding == null) return
        finished = true
        timeline?.removeAllListeners(); timeline?.cancel()
        val nav = findNavController()
        // 인트로를 닫으면 아래의 온보딩이 기존 사용자는 홈, 새 사용자는 닉네임 입력으로 보낸다
        if (nav.currentDestination?.id == R.id.introFragment) nav.popBackStack()
    }

    private fun ddayText(): String {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        val days = ChronoUnit.DAYS.between(today, LocalDate.of(today.year, 12, 25))
        return if (days > 0) getString(R.string.intro_dday, days.toInt()) else getString(R.string.intro_christmas)
    }

    override fun onDestroyView() {
        timeline?.removeAllListeners(); timeline?.cancel(); timeline = null
        _binding = null
        super.onDestroyView()
    }

    private fun cl(t: Long, a: Long, b: Long): Float = min(1f, max(0f, (t - a).toFloat() / (b - a)))
    private fun ad(p: Float): Float = ((1 - cos(PI * p)) / 2).toFloat()
    private fun eo(p: Float): Float = 1 - (1 - p).pow(3)

    private companion object {
        const val ART_W = 320f
        const val ART_H = 380f
        const val GROUND_Y = 334f
        const val ICON_TREE_BASE = 0.587f
        const val LETTER_START = 2150L
        const val FLY = 340L
        const val HOLD_MS = 900L
        // 그림 좌표: 단계별 우체통 위치(기둥 x, 땅 y)와 편지가 맞는 몸통 높이
        val MAILBOX_X = floatArrayOf(0f, 52f, 50f, 38f, 36f, 34f)
        val MAILBOX_GY = floatArrayOf(0f, 332f, 332f, 334f, 334f, 334f)
        val TRUNK_Y = floatArrayOf(0f, 250f, 232f, 215f, 205f, 195f)
    }
}
