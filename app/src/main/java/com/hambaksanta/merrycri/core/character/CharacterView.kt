// core/character/CharacterView.kt
package com.hambaksanta.merrycri.core.character

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambaksanta.merrycri.ui.theme.MerryColors

/**
 * 캐릭터 한 명. 담당: A
 * TODO(A): 지금은 Canvas 도형으로 그린 임시판. 부위별 함수 안을 PNG 레이어(흰색 + tint)로 바꾼다.
 * 레이어 순서: 몸 → 부츠 → 상의 → 장갑 → 목도리 → 모자 → 손에 든 것
 *
 * @param sitting 라운지 소파 등에 앉아 있을 때
 * @param roasted holding 이 마시멜로일 때 구웠는지
 */
@Composable
fun CharacterView(
    outfit: Outfit,
    holding: Snack?,
    nick: String,
    modifier: Modifier = Modifier,
    sitting: Boolean = false,
    roasted: Boolean = false,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = nick,
            color = MerryColors.Snow,
            fontSize = 11.sp,
            modifier = Modifier
                .background(MerryColors.Night.copy(alpha = 0.7f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Canvas(Modifier.size(CharacterSize.width, CharacterSize.height)) {
            drawBoots(outfit, sitting)
            drawBody(outfit)
            drawGloves(outfit)
            drawHead()
            drawScarf(outfit)
            drawHat(outfit)
            if (holding != null) drawHolding(holding, roasted)
        }
    }
}

/** SceneScaffold 가 캐릭터 발 위치를 맞출 때 쓴다. */
object CharacterSize {
    val width = 64.dp
    val height = 88.dp
    /** 닉네임 말풍선 높이까지 포함한 전체 높이 */
    val totalHeight = 104.dp
}

private fun DrawScope.drawBoots(o: Outfit, sitting: Boolean) {
    val w = size.width; val h = size.height
    val top = if (sitting) h * 0.80f else h * 0.84f
    val c = o.bootsColor()
    drawRoundRect(c, Offset(w * 0.28f, top), Size(w * 0.18f, h * 0.14f), CornerRadius(6f))
    drawRoundRect(c, Offset(w * 0.54f, top), Size(w * 0.18f, h * 0.14f), CornerRadius(6f))
}

private fun DrawScope.drawBody(o: Outfit) {
    val w = size.width; val h = size.height
    drawRoundRect(o.topColor(), Offset(w * 0.22f, h * 0.48f), Size(w * 0.56f, h * 0.38f), CornerRadius(w * 0.2f))
    if (o.topType() == Top.SANTA) {
        drawRect(MerryColors.Snow, Offset(w * 0.22f, h * 0.80f), Size(w * 0.56f, h * 0.05f))
    }
}

private fun DrawScope.drawGloves(o: Outfit) {
    if (o.glovesType() == Gloves.NONE) return
    val w = size.width; val h = size.height
    drawCircle(o.glovesColor(), w * 0.08f, Offset(w * 0.18f, h * 0.70f))
    drawCircle(o.glovesColor(), w * 0.08f, Offset(w * 0.82f, h * 0.70f))
}

private fun DrawScope.drawHead() {
    val w = size.width; val h = size.height
    drawCircle(MerryColors.Skin, w * 0.22f, Offset(w * 0.5f, h * 0.34f))
    drawCircle(MerryColors.Night, w * 0.025f, Offset(w * 0.42f, h * 0.34f))
    drawCircle(MerryColors.Night, w * 0.025f, Offset(w * 0.58f, h * 0.34f))
}

private fun DrawScope.drawScarf(o: Outfit) {
    if (o.scarfType() == Scarf.NONE) return
    val w = size.width; val h = size.height
    drawRoundRect(o.scarfColor(), Offset(w * 0.28f, h * 0.48f), Size(w * 0.44f, h * 0.07f), CornerRadius(8f))
    drawRoundRect(o.scarfColor(), Offset(w * 0.56f, h * 0.52f), Size(w * 0.09f, h * 0.14f), CornerRadius(4f))
}

private fun DrawScope.drawHat(o: Outfit) {
    val w = size.width; val h = size.height
    val c = o.hatColor()
    when (o.hatType()) {
        Hat.NONE -> Unit
        Hat.BEANIE -> {
            drawArc(c, 180f, 180f, true, Offset(w * 0.28f, h * 0.12f), Size(w * 0.44f, h * 0.24f))
            drawCircle(MerryColors.Snow, w * 0.05f, Offset(w * 0.5f, h * 0.12f))
        }
        Hat.SANTA -> {
            drawArc(c, 180f, 180f, true, Offset(w * 0.28f, h * 0.10f), Size(w * 0.44f, h * 0.28f))
            drawRect(MerryColors.Snow, Offset(w * 0.27f, h * 0.22f), Size(w * 0.46f, h * 0.04f))
            drawCircle(MerryColors.Snow, w * 0.05f, Offset(w * 0.74f, h * 0.14f))
        }
        Hat.EARMUFF -> {
            drawCircle(c, w * 0.08f, Offset(w * 0.28f, h * 0.36f))
            drawCircle(c, w * 0.08f, Offset(w * 0.72f, h * 0.36f))
        }
        Hat.ANTLER -> {
            drawRect(c, Offset(w * 0.34f, h * 0.06f), Size(w * 0.04f, h * 0.10f))
            drawRect(c, Offset(w * 0.62f, h * 0.06f), Size(w * 0.04f, h * 0.10f))
        }
    }
}

/** TODO(A): 간식 이미지로 바꾸기. 지금은 색 네모. */
private fun DrawScope.drawHolding(s: Snack, roasted: Boolean) {
    val w = size.width; val h = size.height
    val c = when (s) {
        Snack.COCOA -> MerryColors.Red
        Snack.LATTE, Snack.AMERICANO -> MerryColors.Snow
        Snack.GINGERBREAD -> MerryColors.Cookie
        Snack.CHOCOLATE -> MerryColors.Choco
        Snack.MARSHMALLOW -> if (roasted) MerryColors.Cookie else MerryColors.Snow
    }
    drawRoundRect(c, Offset(w * 0.80f, h * 0.58f), Size(w * 0.16f, h * 0.12f), CornerRadius(4f))
}
