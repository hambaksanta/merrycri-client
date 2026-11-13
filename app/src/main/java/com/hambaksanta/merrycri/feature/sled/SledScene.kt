// feature/sled/SledScene.kt
package com.hambaksanta.merrycri.feature.sled

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import com.hambaksanta.merrycri.ui.theme.MerryColors
import com.hambaksanta.merrycri.core.scene.LocalMerry3D
import io.github.sceneview.math.Size
import io.github.sceneview.SceneScope
import com.hambaksanta.merrycri.core.scene.PineTree3D

/**
 * 썰매 언덕. 담당: A
 * 구현 명세: 기획서 '[구현] 공간별 구현 명세' 의 썰매 언덕 항목
 */
@Composable
fun SledScene(presence: PresenceViewModel, onBack: () -> Unit) {
    SceneScaffold(
        sceneId = SceneId.SLED,
        presence = presence,
        onBackToPlaza = onBack,
        objects = { PlaceholderLabel(stringResource(R.string.placeholder_sled)) },
        world = {
            // TODO(A): 3순위. 언덕 경사와 썰매 타기 (11/24 시작 여부 판단)
            SledSlope3D()
            PineTree3D(Position(-5f, 0f, -3f), height = 3f, star = false)
            PineTree3D(Position(5f, 0f, -1f), height = 3.5f, star = false)
        },
    )
}

/** 썰매 언덕 경사면 (기울인 판). TODO(A): 언덕 glb 또는 지형 메시 */
@Composable
private fun SceneScope.SledSlope3D() {
    val m = LocalMerry3D.current.materials
    CubeNode(size = Size(5f, 0.3f, 9f), materialInstance = m.of(MerryColors.Ice), position = Position(0f, 1.6f, -6f), rotation = Rotation(x = 22f))
}
