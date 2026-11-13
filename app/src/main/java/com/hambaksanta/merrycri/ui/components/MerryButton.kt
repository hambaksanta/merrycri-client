// ui/components/MerryButton.kt
package com.hambaksanta.merrycri.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.core.audio.Sfx
import com.hambaksanta.merrycri.ui.theme.MerryColors
import com.hambaksanta.merrycri.ui.theme.MerryTheme

/** 버튼 종류. Primary = 금색 주요 버튼, Secondary = 카드색 보조 버튼 */
enum class MerryButtonStyle { Primary, Secondary }

/**
 * 앱 공통 버튼. 담당: C
 * - 높이 52dp, 둥근 모서리, 누르면 살짝 작아짐
 * - 누를 때 버튼 효과음(sfx_button)이 있으면 재생
 */
@Composable
fun MerryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: MerryButtonStyle = MerryButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "merryButtonPress")
    // Preview 에서는 MerryCriApp 이 없으므로 null 이 된다
    val app = LocalContext.current.applicationContext as? MerryCriApp

    val container = if (style == MerryButtonStyle.Primary) MerryColors.Gold else MerryColors.Card
    val content = if (style == MerryButtonStyle.Primary) MerryColors.Night else MerryColors.Snow

    Button(
        onClick = {
            app?.audio?.playSfx(Sfx.BUTTON)
            onClick()
        },
        modifier = modifier
            .height(52.dp)
            .scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.4f),
            disabledContentColor = content.copy(alpha = 0.6f),
        ),
        interactionSource = interaction,
    ) {
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14213D)
@Composable
private fun MerryButtonPreview() {
    MerryTheme {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MerryButton("포장하기", onClick = {}, modifier = Modifier.fillMaxWidth())
            MerryButton("광장으로", onClick = {}, style = MerryButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            MerryButton("12/25에 열 수 있어요", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        }
    }
}
