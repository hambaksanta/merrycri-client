// feature/tree/TreeScene.kt
package com.hambaksanta.merrycri.feature.tree

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold

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
    )
}
