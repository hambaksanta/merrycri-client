// core/character/MerryCharacter.kt
package com.hambaksanta.merrycri.core.character

import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import com.google.android.filament.gltfio.FilamentInstance
import kotlin.math.pow

/**
 * merry_character.glb 캐릭터 한 명을 조작한다. 담당: A
 *
 * glb 구조
 * - 움직이는 마디: hips, torso, head, leg_L/R, arm_L/R, hand_L/R, socket_hand_R(간식 자리)
 * - 파츠 그룹: "{부위}_{id}" 이름의 노드. 보이면 크기 1, 숨기면 크기 0.
 *     hat_beanie / hat_santa / hat_earmuff / hat_antler
 *     scarf_stripe / scarf_knit / scarf_solid
 *     gloves_mitten / gloves_knit          (왼손·오른손 각각: gloves_mitten.L, gloves_mitten.R)
 *     top_padding / top_sweater / top_coat / top_santa   (top_santa 는 소매 끝 top_santa.cuffL/R 도 있음)
 *     boots_shoes / boots_fur / boots_rain (다리마다 .L, .R)
 *     snack_cocoa / snack_latte / snack_americano / snack_gingerbread / snack_chocolate / snack_marshmallow
 *   id 는 Outfit.kt 의 id 와 같다. "none" 이면 그 부위는 전부 숨긴다.
 * - 색을 바꾸는 재질: mat_hat, mat_scarf, mat_gloves, mat_top, mat_boots, mat_marshmallow
 * - 애니메이션: Idle(2.4초), Walk(0.64초), Sit(고정 자세), Wave(1초), Bite(0.8초)
 *   모든 애니메이션이 7개 마디를 다 갖고 있어서, 동작을 바꿔도 이전 자세가 남지 않는다.
 *
 * 쓰는 법: Character3D.kt 를 본다. 화면 코드는 Character3D(...) 만 부르면 되고,
 * 이 클래스는 Character3D 안에서 옷 입히기(applyOutfit)와 동작 재생(play)에만 쓴다.
 */
class MerryCharacter(
    private val engine: Engine,
    private val instance: FilamentInstance,
) {
    /** 이 캐릭터(instance)의 노드 이름 → 엔티티 */
    private val entities: List<Pair<String, Int>> =
        instance.entities.map { (instance.asset.getName(it) ?: "") to it }

    private val animator = instance.animator
    private val animIndex: Map<String, Int> =
        (0 until animator.animationCount).associateBy { animator.getAnimationName(it) }

    /** 옷·색·손에 든 간식을 한 번에 반영한다. 옷장에서 바꿀 때마다, presence 가 바뀔 때마다 부른다. */
    fun applyOutfit(outfit: Outfit, holding: Snack?, roasted: Boolean = false) {
        show("hat", outfit.hat)
        show("scarf", outfit.scarf)
        show("gloves", outfit.gloves)
        show("top", outfit.top)
        show("boots", outfit.boots)
        show("snack", holding?.id ?: "none")

        setColor("mat_hat", outfit.hatColor())
        setColor("mat_scarf", outfit.scarfColor())
        setColor("mat_gloves", outfit.glovesColor())
        setColor("mat_top", outfit.topColor())
        setColor("mat_boots", outfit.bootsColor())
        setColor("mat_marshmallow", if (roasted) ROASTED else MARSHMALLOW)
    }

    /** 부위(category)에서 id 하나만 보이고 나머지는 숨긴다. id 가 "none" 이면 전부 숨김. */
    fun show(category: String, id: String) {
        val tm = engine.transformManager
        val prefix = category + "_"
        for ((name, entity) in entities) {
            if (!name.startsWith(prefix)) continue
            val variantId = name.removePrefix(prefix).substringBefore('.')
            val ti = tm.getInstance(entity)
            if (ti != 0) tm.setTransform(ti, if (variantId == id) IDENTITY else HIDDEN)
        }
    }

    /** 재질 이름이 materialName 인 모든 부분의 색을 바꾼다. Compose Color(sRGB) 를 선형 색으로 바꿔 넣는다. */
    fun setColor(materialName: String, color: Color) {
        val rm = engine.renderableManager
        val r = srgbToLinear(color.red)
        val g = srgbToLinear(color.green)
        val b = srgbToLinear(color.blue)
        for ((_, entity) in entities) {
            if (!rm.hasComponent(entity)) continue
            val ri = rm.getInstance(entity)
            for (p in 0 until rm.getPrimitiveCount(ri)) {
                val mi = rm.getMaterialInstanceAt(ri, p)
                if (mi.name == materialName) mi.setParameter("baseColorFactor", r, g, b, 1f)
            }
        }
    }

    /** 애니메이션 name 을 timeSeconds 시점 자세로 만든다. 매 프레임 부른다. */
    fun play(name: String, timeSeconds: Float) {
        val i = animIndex[name] ?: return
        val duration = animator.getAnimationDuration(i)
        animator.applyAnimation(i, if (duration > 0f) timeSeconds % duration else 0f)
        animator.updateBoneMatrices()
    }

    companion object {
        const val IDLE = "Idle"
        const val WALK = "Walk"
        const val SIT = "Sit"
        const val WAVE = "Wave"
        const val BITE = "Bite"

        /** 상태에 맞는 기본 동작. 손 흔들기·한 입은 그 순간에만 따로 play 한다. */
        fun pick(walking: Boolean, sitting: Boolean): String = when {
            sitting -> SIT
            walking -> WALK
            else -> IDLE
        }

        private val MARSHMALLOW = Color(0xFFFBF6EE)
        private val ROASTED = Color(0xFFC8834A)

        private val IDENTITY = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        )
        private val HIDDEN = floatArrayOf(
            0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f,
        )

        private fun srgbToLinear(c: Float): Float =
            if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
}
