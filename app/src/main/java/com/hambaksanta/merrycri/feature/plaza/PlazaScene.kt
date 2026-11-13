// feature/plaza/PlazaScene.kt
package com.hambaksanta.merrycri.feature.plaza

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.scene.PlaceholderLabel
import com.hambaksanta.merrycri.core.scene.PresenceViewModel
import com.hambaksanta.merrycri.core.scene.SceneId
import com.hambaksanta.merrycri.core.scene.SceneScaffold
import io.github.sceneview.math.Position
import com.hambaksanta.merrycri.core.scene.House3D
import com.hambaksanta.merrycri.core.scene.Lamp3D
import com.hambaksanta.merrycri.core.scene.PineTree3D
import com.hambaksanta.merrycri.core.scene.Signpost3D
import com.hambaksanta.merrycri.ui.theme.MerryColors

/**
 * 마을 광장 (허브). 담당: A
 * 지금은 하단 칩이 이정표 역할. 3순위 공간은 '준비 중'으로 표시.
 * TODO(A, 단계 1): 집·전구·트리 배경, TwinkleLights, Aurora, 이정표를 배경 위 오브젝트로
 * 흔들기 폭설(events/snowstorm)은 SceneScaffold 가 모든 공간에서 처리한다. 디버그 빌드는 상단 버튼으로 대체.
 */
@Composable
fun PlazaScene(presence: PresenceViewModel, onGo: (SceneId) -> Unit, onWardrobe: () -> Unit) {
    SceneScaffold(
        sceneId = SceneId.PLAZA,
        presence = presence,
        onBackToPlaza = null,
        bottomBar = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { AssistChip(onClick = onWardrobe, label = { Text(stringResource(SceneId.WARDROBE.titleRes)) }) }
                items(SceneId.signposts) { s ->
                    val title = stringResource(s.titleRes)
                    AssistChip(
                        onClick = { onGo(s) },
                        label = { Text(if (s.priority == 3) stringResource(R.string.scene_preparing, title) else title) },
                    )
                }
            }
        },
        objects = { PlaceholderLabel(stringResource(R.string.placeholder_plaza)) },
        world = {
            // TODO(A): 임시 배치. 집·트리·가로등을 glb 로 바꾸고 전구·오로라 더하기
            PineTree3D(Position(0f, 0f, -2.5f), height = 4.5f)
            House3D(Position(-5f, 0f, -6f), rotationY = 15f)
            House3D(Position(0f, 0f, -7.5f), wall = MerryColors.Pine)
            House3D(Position(5f, 0f, -6f), wall = MerryColors.Navy, rotationY = -15f)
            House3D(Position(-7.5f, 0f, -1f), wall = MerryColors.Cream, rotationY = 70f)
            House3D(Position(7.5f, 0f, -1f), rotationY = -70f)
            Lamp3D(Position(-3f, 0f, 1f))
            Lamp3D(Position(3f, 0f, 1f))
            Signpost3D(Position(-1.5f, 0f, 3.5f))
            Signpost3D(Position(1.5f, 0f, 3.5f), color = MerryColors.Pine)
        },
    )
}
