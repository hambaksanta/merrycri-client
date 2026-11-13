// feature/snowman/SnowmanScene.kt
package com.hambaksanta.merrycri.feature.snowman

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold
import io.github.sceneview.math.Position
import com.hambaksanta.merrycri.core.scene.PineTree3D
import com.hambaksanta.merrycri.core.scene.Snowman3D
import com.hambaksanta.merrycri.ui.theme.MerryColors

/**
 * 눈사람 언덕. 담당: A
 * 구현 명세: 기획서 '[구현] 공간별 구현 명세' 의 눈사람 언덕 항목
 */
@Composable
fun SnowmanScene(presence: PresenceViewModel, onBack: () -> Unit) {
    SceneScaffold(
        sceneId = SceneId.SNOWMAN,
        presence = presence,
        onBackToPlaza = onBack,
        objects = { PlaceholderLabel(stringResource(R.string.placeholder_snowman)) },
        world = {
            // TODO(A): 굴리기 → 쌓기 → 꾸미기 → 세우기 (snowmen, 1인 3개)
            Snowman3D(Position(-3f, 0f, -3f))
            Snowman3D(Position(3.5f, 0f, -2f), scarf = MerryColors.Pine)
            PineTree3D(Position(-6f, 0f, -6f), height = 3.5f, star = false)
            PineTree3D(Position(0f, 0f, -7f), height = 4f, star = false)
            PineTree3D(Position(6f, 0f, -6f), height = 3f, star = false)
        },
    )
}
