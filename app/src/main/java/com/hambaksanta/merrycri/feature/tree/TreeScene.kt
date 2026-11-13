// feature/tree/TreeScene.kt
package com.hambaksanta.merrycri.feature.tree

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold
import io.github.sceneview.math.Position
import com.hambaksanta.merrycri.core.scene.GiftBox3D
import com.hambaksanta.merrycri.core.scene.PineTree3D
import com.hambaksanta.merrycri.ui.theme.MerryColors

/**
 * 트리 광장. 담당: C
 * 구현 명세: 기획서 '[구현] 공간별 구현 명세' 의 트리 광장 항목
 */
@Composable
fun TreeScene(presence: PresenceViewModel, onBack: () -> Unit) {
    SceneScaffold(
        sceneId = SceneId.TREE,
        presence = presence,
        onBackToPlaza = onBack,
        objects = { PlaceholderLabel(stringResource(R.string.placeholder_tree)) },
        world = {
            // TODO(C): 공동 트리 꾸미기. 탭하면 tree/ornaments 에 오너먼트 추가
            PineTree3D(Position(0f, 0f, -2f), height = 6f)
            GiftBox3D(Position(-1.2f, 0f, -0.6f))
            GiftBox3D(Position(1.1f, 0f, -0.4f), box = MerryColors.Pine, ribbon = MerryColors.Red, size = 0.4f)
            GiftBox3D(Position(0.2f, 0f, 0f), box = MerryColors.Navy, ribbon = MerryColors.Snow, size = 0.35f)
            PineTree3D(Position(-6f, 0f, -5f), height = 3f, star = false)
            PineTree3D(Position(6f, 0f, -5f), height = 3.4f, star = false)
        },
    )
}
