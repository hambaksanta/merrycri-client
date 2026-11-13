// feature/snowball/SnowballScene.kt
package com.hambaksanta.merrycri.feature.snowball

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold
import io.github.sceneview.math.Position
import com.hambaksanta.merrycri.ui.theme.MerryColors
import com.hambaksanta.merrycri.core.scene.LocalMerry3D
import io.github.sceneview.math.Size
import io.github.sceneview.SceneScope
import com.hambaksanta.merrycri.core.scene.PineTree3D

/**
 * 눈싸움 마당. 담당: B
 * 구현 명세: 기획서 '[구현] 공간별 구현 명세' 의 눈싸움 마당 항목
 */
@Composable
fun SnowballScene(presence: PresenceViewModel, onBack: () -> Unit) {
    SceneScaffold(
        sceneId = SceneId.SNOWBALL,
        presence = presence,
        onBackToPlaza = onBack,
        objects = { PlaceholderLabel(stringResource(R.string.placeholder_snowball)) },
        world = {
            // TODO(B): 상대 캐릭터 탭 → 눈덩이 던지기. 지금은 눈 벽 자리표시
            SnowWall3D(Position(-3f, 0f, -1.5f))
            SnowWall3D(Position(3f, 0f, 1f))
            PineTree3D(Position(-6f, 0f, -6f), height = 3f, star = false)
            PineTree3D(Position(6f, 0f, -6f), height = 3.5f, star = false)
        },
    )
}

/** 눈 벽 (몸 숨기는 곳). TODO(B): glb 로 바꾸거나 모양 다듬기 */
@Composable
private fun SceneScope.SnowWall3D(position: Position) {
    val m = LocalMerry3D.current.materials
    CubeNode(size = Size(2.2f, 0.8f, 0.5f), materialInstance = m.of(MerryColors.Ice), position = Position(position.x, 0.4f, position.z))
}
