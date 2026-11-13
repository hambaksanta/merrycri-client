// feature/post/PostOfficeScene.kt
package com.hambaksanta.merrycri.feature.post

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold

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
    )
}
