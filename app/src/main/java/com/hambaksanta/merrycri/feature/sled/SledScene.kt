// feature/sled/SledScene.kt
package com.hambaksanta.merrycri.feature.sled

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold

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
    )
}
