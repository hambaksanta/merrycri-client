// feature/snowman/SnowmanScene.kt
package com.hambaksanta.merrycri.feature.snowman

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold

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
    )
}
