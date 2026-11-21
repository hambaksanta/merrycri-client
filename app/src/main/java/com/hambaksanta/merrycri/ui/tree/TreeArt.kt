// ui/tree/TreeArt.kt
package com.hambaksanta.merrycri.ui.tree

import android.graphics.PointF
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.hambaksanta.merrycri.R

/**
 * 트리 그림 자료: 단계 계산, 단계별 그림, 장식 그림, 장식 자리. 담당: A
 *
 * 겹쳐 그리는 순서 (TreeSceneView):
 *   bg_sky_lvN → AuroraView(Lv5, 물결치는 오로라) → WeatherView(내리는 눈·반짝이는 별)
 *   → [흔들림] tree_lvN → tree_lights_a/b(번갈아 깜빡임) → tree_star_lv5(반짝임) → 편지 장식(orn_*, SLOTS 자리)
 *   → props_lvN (랜턴·선물·발자국 등 땅 위 소품) → mailbox_lvN_empty/full (우체통, 맨 앞)
 * 모든 그림은 320 x 380 기준이고, SLOTS는 그 안의 비율(0~1)이다.
 */
object TreeArt {
    /** 받은 편지 수 → 단계 시작 기준. 0 / 1~2 / 3~5 / 6~9 / 10 이상. 발표 때 바꾸려면 여기만 고친다. */
    private val STAGE_STARTS = intArrayOf(0, 1, 3, 6, 10)

    /** 받은 편지 수로 트리 단계(1~5)를 정한다. */
    fun stageOf(letterCount: Int): Int = STAGE_STARTS.indexOfLast { letterCount >= it } + 1

    /** 다음 단계까지 남은 편지 수. 최고 단계면 0. */
    fun lettersToNext(letterCount: Int): Int {
        val stage = stageOf(letterCount)
        return if (stage >= STAGE_STARTS.size) 0 else STAGE_STARTS[stage] - letterCount
    }

    /** 현재 단계 안에서의 진행률 0f~1f (진행바용). */
    fun progressInStage(letterCount: Int): Float {
        val stage = stageOf(letterCount)
        if (stage >= STAGE_STARTS.size) return 1f
        val start = STAGE_STARTS[stage - 1]
        val end = STAGE_STARTS[stage]
        return (letterCount - start).toFloat() / (end - start)
    }

    @DrawableRes
    fun treeRes(stage: Int): Int = when (stage) {
        1 -> R.drawable.tree_lv1
        2 -> R.drawable.tree_lv2
        3 -> R.drawable.tree_lv3
        4 -> R.drawable.tree_lv4
        else -> R.drawable.tree_lv5
    }

    @DrawableRes
    fun skyRes(stage: Int): Int = when (stage) {
        1 -> R.drawable.bg_sky_lv1
        2 -> R.drawable.bg_sky_lv2
        3 -> R.drawable.bg_sky_lv3
        4 -> R.drawable.bg_sky_lv4
        else -> R.drawable.bg_sky_lv5
    }

    /** 땅 위 소품(랜턴·선물·발자국 등). 트리 앞에 그린다. Lv1은 없음. */
    @DrawableRes
    fun propsRes(stage: Int): Int? = when (stage) {
        1 -> null
        2 -> R.drawable.props_lv2
        3 -> R.drawable.props_lv3
        4 -> R.drawable.props_lv4
        else -> R.drawable.props_lv5
    }

    /** 번갈아 깜빡이는 전구 두 장 (Lv4·Lv5만). 없으면 null. */
    fun lightsRes(stage: Int): Pair<Int, Int>? = when (stage) {
        4 -> R.drawable.tree_lights_a_lv4 to R.drawable.tree_lights_b_lv4
        5 -> R.drawable.tree_lights_a_lv5 to R.drawable.tree_lights_b_lv5
        else -> null
    }

    /** 꼭대기 별 (Lv5만). 별 중심은 그림 안 (160, 52). */
    @DrawableRes
    fun starRes(stage: Int): Int? = if (stage >= 5) R.drawable.tree_star_lv5 else null
    val STAR_CENTER = PointF(160f / 320f, 52f / 380f)

    /** 오로라는 그림 파일이 아니라 AuroraView가 직접 그린다 (Lv5만). */
    fun hasAurora(stage: Int): Boolean = stage >= 5

    /** 우체통: 비어 있음 / 편지 가득. 단계마다 색이 다르다 (회색 → 나무 → 빨강 → 나무 → 빨강). */
    @DrawableRes
    fun mailboxRes(stage: Int, full: Boolean): Int = when (stage) {
        1 -> if (full) R.drawable.mailbox_lv1_full else R.drawable.mailbox_lv1_empty
        2 -> if (full) R.drawable.mailbox_lv2_full else R.drawable.mailbox_lv2_empty
        3 -> if (full) R.drawable.mailbox_lv3_full else R.drawable.mailbox_lv3_empty
        4 -> if (full) R.drawable.mailbox_lv4_full else R.drawable.mailbox_lv4_empty
        else -> if (full) R.drawable.mailbox_lv5_full else R.drawable.mailbox_lv5_empty
    }

    /** 트리 밑동(흔들림 기준점) 높이 비율 */
    const val TREE_BASE_Y = 334f / 380f

    @StringRes
    fun nameRes(stage: Int): Int = when (stage) {
        1 -> R.string.tree_stage_1
        2 -> R.string.tree_stage_2
        3 -> R.string.tree_stage_3
        4 -> R.string.tree_stage_4
        else -> R.string.tree_stage_5
    }

    @StringRes
    fun messageRes(stage: Int): Int = when (stage) {
        1 -> R.string.tree_stage_message_1
        2 -> R.string.tree_stage_message_2
        3 -> R.string.tree_stage_message_3
        4 -> R.string.tree_stage_message_4
        else -> R.string.tree_stage_message_5
    }

    /** DB의 장식 type 값 순서. 편지 쓰기 화면의 장식 고르기도 이 순서로 보여준다. */
    val ORNAMENT_TYPES = listOf("bauble", "star", "cane", "snow", "gift", "bell")

    /** 장식 type → 그림. 모르는 값이면 방울. */
    @DrawableRes
    fun ornamentRes(type: String): Int = when (type) {
        "star" -> R.drawable.orn_star
        "cane" -> R.drawable.orn_cane
        "snow" -> R.drawable.orn_snow
        "gift" -> R.drawable.orn_gift
        "bell" -> R.drawable.orn_bell
        else -> R.drawable.orn_bauble
    }

    /**
     * 단계별 장식 자리 (그림 안 비율). 장식은 slot 번호 순서대로 여기에 놓는다.
     * 자리 수 = 그 단계의 최대 편지 수(Lv2 2, Lv3 5, Lv4 9). Lv5는 15자리이고, 넘으면 slot % 15 자리에 겹쳐 그린다.
     */
    val SLOTS: Map<Int, List<PointF>> = mapOf(
        1 to emptyList(),
        2 to listOf(
            PointF(0.347f, 0.703f),
            PointF(0.619f, 0.545f),
        ),
        3 to listOf(
            PointF(0.469f, 0.384f),
            PointF(0.406f, 0.521f),
            PointF(0.512f, 0.647f),
            PointF(0.319f, 0.758f),
            PointF(0.675f, 0.763f),
        ),
        4 to listOf(
            PointF(0.500f, 0.305f),
            PointF(0.403f, 0.432f),
            PointF(0.597f, 0.432f),
            PointF(0.312f, 0.647f),
            PointF(0.456f, 0.663f),
            PointF(0.713f, 0.663f),
            PointF(0.278f, 0.766f),
            PointF(0.500f, 0.776f),
            PointF(0.722f, 0.766f),
        ),
        5 to listOf(
            PointF(0.500f, 0.276f),
            PointF(0.391f, 0.413f),
            PointF(0.609f, 0.413f),
            PointF(0.347f, 0.526f),
            PointF(0.500f, 0.537f),
            PointF(0.653f, 0.526f),
            PointF(0.303f, 0.650f),
            PointF(0.434f, 0.661f),
            PointF(0.566f, 0.650f),
            PointF(0.697f, 0.661f),
            PointF(0.250f, 0.768f),
            PointF(0.375f, 0.779f),
            PointF(0.500f, 0.768f),
            PointF(0.625f, 0.779f),
            PointF(0.750f, 0.768f),
        ),
    )
}
