// feature/snowball/SnowballScene.kt
package com.hambaksanta.merrycri.feature.snowball

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold

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
    )
}
