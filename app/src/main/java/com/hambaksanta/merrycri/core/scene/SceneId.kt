// core/scene/SceneId.kt
package com.hambaksanta.merrycri.core.scene

import androidx.annotation.StringRes
import com.hambaksanta.merrycri.R

/**
 * 마을의 공간 목록. 담당: B
 * id = 화면 이동 주소이자 DB 경로 rooms/{id}/presence 의 {id}. 바꾸면 DB 규칙(merrycri-server)도 같이 바꾼다.
 * owner = 담당자, priority = 기획서 우선순위 (3 = 시간 남으면)
 */
enum class SceneId(val id: String, @StringRes val titleRes: Int, val priority: Int, val owner: String) {
    WARDROBE("wardrobe", R.string.space_wardrobe, 1, "A"),
    PLAZA("plaza", R.string.space_plaza, 1, "A"),
    TREE("tree", R.string.space_tree, 1, "C"),
    POST("post", R.string.space_post, 1, "C"),
    LOUNGE("lounge", R.string.space_lounge, 1, "C"),
    SNOWMAN("snowman", R.string.space_snowman, 2, "A"),
    SNOWBALL("snowball", R.string.space_snowball, 2, "B"),
    SLED("sled", R.string.space_sled, 3, "A");

    val route: String get() = id

    companion object {
        /** 광장 이정표에 나오는 공간들 (옷장·광장 제외. 옷장은 따로 버튼) */
        val signposts: List<SceneId> get() = entries.filter { it != PLAZA && it != WARDROBE }
    }
}
