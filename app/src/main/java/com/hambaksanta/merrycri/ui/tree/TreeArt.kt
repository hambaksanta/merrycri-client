// ui/tree/TreeArt.kt
package com.hambaksanta.merrycri.ui.tree

import android.graphics.PointF
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.hambaksanta.merrycri.R

/**
 * 트리 그림 자료: 단계 계산, 단계별 그림, 장식 그림, 장식 자리. 담당: A
 *
 * 화면에 그리는 순서 (TreeView): bg_sky_lvN → (WeatherView 파티클) → tree_lvN → 편지 장식(orn_*)을 SLOTS 자리에
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
            PointF(0.456f, 0.689f),
            PointF(0.550f, 0.742f),
        ),
        3 to listOf(
            PointF(0.412f, 0.600f),
            PointF(0.581f, 0.563f),
            PointF(0.369f, 0.768f),
            PointF(0.625f, 0.753f),
            PointF(0.500f, 0.689f),
        ),
        4 to listOf(
            PointF(0.438f, 0.389f),
            PointF(0.556f, 0.368f),
            PointF(0.375f, 0.558f),
            PointF(0.500f, 0.526f),
            PointF(0.625f, 0.563f),
            PointF(0.300f, 0.763f),
            PointF(0.425f, 0.737f),
            PointF(0.581f, 0.768f),
            PointF(0.706f, 0.758f),
        ),
        5 to listOf(
            PointF(0.500f, 0.211f),
            PointF(0.391f, 0.276f),
            PointF(0.609f, 0.276f),
            PointF(0.312f, 0.368f),
            PointF(0.500f, 0.342f),
            PointF(0.688f, 0.368f),
            PointF(0.234f, 0.474f),
            PointF(0.406f, 0.447f),
            PointF(0.594f, 0.447f),
            PointF(0.766f, 0.474f),
            PointF(0.312f, 0.566f),
            PointF(0.500f, 0.553f),
            PointF(0.688f, 0.566f),
            PointF(0.406f, 0.645f),
            PointF(0.594f, 0.645f),
        ),
    )
}
