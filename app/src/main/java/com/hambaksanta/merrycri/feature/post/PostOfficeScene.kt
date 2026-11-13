// feature/post/PostOfficeScene.kt
package com.hambaksanta.merrycri.feature.post

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold
import io.github.sceneview.math.Position
import com.hambaksanta.merrycri.core.scene.GiftBox3D
import com.hambaksanta.merrycri.core.scene.House3D
import com.hambaksanta.merrycri.ui.theme.MerryColors

/**
 * 선물 우체국. 담당: C
 * TODO(C, 단계 1): LetterWriteSheet (편지 쓰기·포장), GiftBoxSheet (내 선물함, GiftTime 으로 상태 표시)
 */
@Composable
fun PostOfficeScene(presence: PresenceViewModel, onBack: () -> Unit) {
    SceneScaffold(
        sceneId = SceneId.POST,
        presence = presence,
        onBackToPlaza = onBack,
        objects = { PlaceholderLabel(stringResource(R.string.placeholder_post)) },
        world = {
            // TODO(C): 편지 쓰기 → 포장 → 트리 아래. 지금은 우체국 건물과 선물 더미 자리표시
            House3D(Position(0f, 0f, -5f), wall = MerryColors.Red)
            GiftBox3D(Position(-2f, 0f, -1f), box = MerryColors.Pine)
            GiftBox3D(Position(-1.4f, 0f, -1.4f), size = 0.35f)
            GiftBox3D(Position(2f, 0f, -1.2f), box = MerryColors.Gold, ribbon = MerryColors.Red, size = 0.45f)
        },
    )
}
