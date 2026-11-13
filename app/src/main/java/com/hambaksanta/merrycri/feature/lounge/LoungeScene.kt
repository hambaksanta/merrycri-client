// feature/lounge/LoungeScene.kt
package com.hambaksanta.merrycri.feature.lounge

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hambaksanta.merrycri.R
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold

/**
 * 불멍 라운지. 담당: C
 * 들어오면 BGM 40% 로 낮추고, 나가면 되돌린다 (이미 연결돼 있음).
 * TODO(C, 단계 1): Fireplace (불 일렁임·불티·주황 오버레이), 장작 넣기, 자리 6개, SnackTable
 */
@Composable
fun LoungeScene(presence: PresenceViewModel, onBack: () -> Unit) {
    val audio = (LocalContext.current.applicationContext as MerryCriApp).audio
    DisposableEffect(Unit) {
        audio.duck(0.4f)
        onDispose { audio.unduck() }
    }
    SceneScaffold(
        sceneId = SceneId.LOUNGE,
        presence = presence,
        onBackToPlaza = onBack,
        objects = { PlaceholderLabel(stringResource(R.string.placeholder_lounge)) },
    )
}
