// ui/tree/TreeSceneView.kt
package com.hambaksanta.merrycri.ui.tree

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.hambaksanta.merrycri.R
import kotlin.random.Random

/**
 * 내 트리 / 친구 트리 화면의 트리 장면. 그림 비율 320:380을 유지한다. 담당: A
 *
 * 겹치는 순서: 하늘 → 오로라 → 날씨(눈·별) → [흔들리는 묶음: 트리 → 전구 A/B → 별 → 편지 장식] → 땅 위 소품 → 우체통
 * 움직임: 트리 흔들림, 전구 번갈아 깜빡임, 별 반짝임, 장식 살랑임, 오로라 물결, 눈 내림, 별 반짝임.
 * 기기에서 애니메이션을 끄면 모두 멈춘 장면으로 보여준다.
 *
 * 사용 예)
 *   binding.treeScene.setStage(TreeArt.stageOf(letterCount))
 *   binding.treeScene.setOrnaments(ornaments.map { it.type }) { index -> 편지 열기(index) }
 *   binding.treeScene.setMailboxFull(안 읽은 편지가 있는지)
 */
class TreeSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val sky = layer()
    private val aurora = AuroraView(context)
    private val weather = WeatherView(context)
    private val sway = FrameLayout(context).apply { clipChildren = false }
    private val tree = layer()
    private val lightsA = layer()
    private val lightsB = layer()
    private val star = layer()
    private val ornamentLayer = FrameLayout(context).apply { clipChildren = false }
    private val props = layer()
    private val mailbox = layer()

    private var stage = 1
    private var backgroundVisible = true
    private var mailboxFull = false
    private var ornamentTypes: List<String> = emptyList()
    private var onOrnamentClick: ((Int) -> Unit)? = null
    private val animators = mutableListOf<Animator>()

    init {
        clipChildren = false
        listOf<View>(sky, aurora, weather).forEach { addView(it, matchParent()) }
        listOf<View>(tree, lightsA, lightsB, star, ornamentLayer).forEach { sway.addView(it, matchParent()) }
        addView(sway, matchParent())
        addView(props, matchParent())
        addView(mailbox, matchParent())
        setStage(1)
    }

    /** 트리 단계(1~5)에 맞춰 그림을 바꾼다. 장식 자리도 단계에 따라 바뀐다. */
    fun setStage(stage: Int) {
        this.stage = stage.coerceIn(1, 5)
        sky.setImageResource(TreeArt.skyRes(this.stage))
        tree.setImageResource(TreeArt.treeRes(this.stage))
        props.setOptional(TreeArt.propsRes(this.stage))
        mailbox.setImageResource(TreeArt.mailboxRes(this.stage, mailboxFull))
        aurora.visibility = if (backgroundVisible && TreeArt.hasAurora(this.stage)) View.VISIBLE else View.GONE
        star.setOptional(TreeArt.starRes(this.stage))
        val lights = TreeArt.lightsRes(this.stage)
        lightsA.setOptional(lights?.first)
        lightsB.setOptional(lights?.second)
        weather.setStage(this.stage)
        placeOrnaments()
        restartAnimations()
    }

    /** false면 하늘·오로라·날씨를 숨기고 트리·소품만 보여준다 (첫 화면처럼 하늘을 따로 깔 때). */
    fun setBackgroundVisible(visible: Boolean) {
        val v = if (visible) View.VISIBLE else View.GONE
        sky.visibility = v
        weather.visibility = v
        aurora.visibility = if (visible && TreeArt.hasAurora(stage)) View.VISIBLE else View.GONE
        backgroundVisible = visible
        restartAnimations()
    }

    /** 우체통 상태: false = 비어 있음(문 닫힘), true = 편지 가득(문 열림·깃발 올림). 예) 아직 안 읽은 새 편지가 있으면 true */
    fun setMailboxFull(full: Boolean) {
        mailboxFull = full
        mailbox.setImageResource(TreeArt.mailboxRes(stage, full))
    }

    /** 편지 장식 type 목록(받은 순서). 누르면 [onClick]에 몇 번째 장식인지 알려준다. */
    fun setOrnaments(types: List<String>, onClick: ((Int) -> Unit)? = null) {
        ornamentTypes = types
        onOrnamentClick = onClick
        placeOrnaments()
        restartAnimations()
    }

    /**
     * 달려 있는 장식을 하나씩 톡톡 나타나게 한다 (첫 화면 마지막 장면용).
     * 장식은 setOrnaments() 로 먼저 놓여 있어야 한다.
     */
    fun revealOrnaments(staggerMs: Long) {
        for (i in 0 until ornamentLayer.childCount) {
            val v = ornamentLayer.getChildAt(i)
            v.scaleX = 0f
            v.scaleY = 0f
            v.animate().scaleX(1f).scaleY(1f)
                .setStartDelay(i * staggerMs).setDuration(260L)
                .setInterpolator(OvershootInterpolator(2.2f)).start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 너비 기준으로 320:380 비율 유지 (높이를 정해 두면 그 높이를 따른다)
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            MeasureSpec.getSize(heightMeasureSpec)
        } else {
            (w * ART_H / ART_W).toInt()
        }
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        sway.pivotX = w / 2f
        sway.pivotY = h * TreeArt.TREE_BASE_Y
        star.pivotX = w * TreeArt.STAR_CENTER.x
        star.pivotY = h * TreeArt.STAR_CENTER.y
        // 레이아웃 도중에 자식 뷰를 추가하면 크기가 0으로 잡힐 수 있어서 다음 프레임에 배치한다
        post {
            placeOrnaments()
            restartAnimations()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        restartAnimations()
    }

    override fun onDetachedFromWindow() {
        stopAnimations()
        super.onDetachedFromWindow()
    }

    private fun placeOrnaments() {
        ornamentLayer.removeAllViews()
        val slots = TreeArt.SLOTS[stage].orEmpty()
        if (width == 0 || slots.isEmpty()) return
        // 주의: 아래 apply { } 안에서 width/height를 쓰면 장식 ImageView 자신의 크기(0)가 잡힌다.
        // 그래서 장면 크기를 먼저 꺼내 둔다. (예전엔 이것 때문에 장식이 전부 왼쪽 위 구석에 몰렸음)
        val sceneW = width.toFloat()
        val sceneH = height.toFloat()
        val size = (sceneW * ORNAMENT_SIZE / ART_W).toInt()
        ornamentTypes.forEachIndexed { index, type ->
            val slot = slots[index % slots.size]
            val view = ImageView(context).apply {
                setImageResource(TreeArt.ornamentRes(type))
                contentDescription = context.getString(R.string.tree_cd_ornament, index + 1)
                translationX = slot.x * sceneW - size / 2f
                translationY = slot.y * sceneH - size / 2f
                pivotX = size / 2f
                pivotY = size * 0.08f // 고리에 매달린 듯 위쪽 기준으로 살랑
                onOrnamentClick?.let { click -> setOnClickListener { click(index) } }
            }
            ornamentLayer.addView(view, LayoutParams(size, size))
        }
    }

    private fun restartAnimations() {
        stopAnimations()
        if (!isAttachedToWindow || !ValueAnimator.areAnimatorsEnabled()) return

        // 트리 흔들림: 어린 나무일수록 더 크게
        val angle = if (stage <= 2) 1.6f else 0.8f
        animators += loop(ObjectAnimator.ofFloat(sway, View.ROTATION, -angle, angle), 2000L)

        if (lightsA.visibility == View.VISIBLE) {
            animators += loop(ObjectAnimator.ofFloat(lightsA, View.ALPHA, 1f, 0.35f), 700L)
            animators += loop(ObjectAnimator.ofFloat(lightsB, View.ALPHA, 0.35f, 1f), 700L)
        }
        if (star.visibility == View.VISIBLE) {
            animators += loop(
                ObjectAnimator.ofPropertyValuesHolder(
                    star,
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0.94f, 1.08f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.94f, 1.08f),
                ),
                1000L,
            )
        }
        // 장식 살랑임: 장식마다 박자를 다르게
        val rnd = Random(stage)
        for (i in 0 until ornamentLayer.childCount) {
            val v = ornamentLayer.getChildAt(i)
            animators += loop(ObjectAnimator.ofFloat(v, View.ROTATION, -5f, 5f), 1300L).apply {
                startDelay = rnd.nextLong(0, 1300)
            }
        }
        animators.forEach { it.start() }
    }

    private fun stopAnimations() {
        animators.forEach { it.cancel() }
        animators.clear()
        sway.rotation = 0f
        lightsA.alpha = 1f
        lightsB.alpha = 1f
        star.scaleX = 1f
        star.scaleY = 1f
    }

    private fun loop(anim: ValueAnimator, halfPeriod: Long) = anim.apply {
        duration = halfPeriod
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
    }

    private fun layer() = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_XY
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun ImageView.setOptional(res: Int?) {
        if (res == null) {
            setImageDrawable(null)
            visibility = View.GONE
        } else {
            setImageResource(res)
            visibility = View.VISIBLE
        }
    }

    private fun matchParent() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

    private companion object {
        const val ART_W = 320f
        const val ART_H = 380f
        const val ORNAMENT_SIZE = 26f
    }
}
