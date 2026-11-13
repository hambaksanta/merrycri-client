// core/effects/SnowstormTransition.kt
package com.hambaksanta.merrycri.core.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.hambaksanta.merrycri.ui.theme.MerryColors

/**
 * 공간 이동할 때 덮는 '눈보라 화이트아웃'. 담당: A
 * progress 1 = 화면이 하얗게 다 덮임, 0 = 안 보임. 애니메이션은 MerryNavHost(B)가 돌린다.
 * TODO(A): 바람 소리(Sfx.WIND)와 눈발이 옆으로 날리는 느낌 더하기
 */
@Composable
fun SnowstormTransition(progress: Float, modifier: Modifier = Modifier) {
    if (progress <= 0f) return
    Box(modifier.fillMaxSize().alpha(progress)) {
        Box(Modifier.fillMaxSize().background(MerryColors.Snow))
        Snowfall(Modifier.fillMaxSize(), count = 200, heavy = true)
    }
}
