// ui/theme/Theme.kt
package com.hambaksanta.merrycri.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 기획서 '색 팔레트'와 같은 값. 색은 전부 여기에만 둔다. 담당: A */
object MerryColors {
    val Night = Color(0xFF14213D)
    val NightSky = Color(0xFF1B2A4A)
    val Card = Color(0xFF24365C)
    val Snow = Color(0xFFF4F7FB)
    val Gold = Color(0xFFFFD27A)
    val Red = Color(0xFFD64545)
    val Pine = Color(0xFF2F6B4F)
    val Fire = Color(0xFFFF9A3C)
    val AuroraMint = Color(0xFF7FFFD4)
    val AuroraPurple = Color(0xFF9B7BFF)
    val Muted = Color(0xFFB8C4DA)

    // 옷 팔레트 (core/character/PaletteColor)
    val Cream = Color(0xFFF3E6C8)
    val Navy = Color(0xFF1E3A6E)
    val Pink = Color(0xFFF2A7B8)
    val Brown = Color(0xFF8A5A3C)

    // 캐릭터·간식 (임시 도형용)
    val Skin = Color(0xFFF2D3B3)
    val Cookie = Color(0xFFC98A4B)
    val Choco = Color(0xFF6B3A26)
}

private val scheme = darkColorScheme(
    primary = MerryColors.Gold,
    onPrimary = MerryColors.Night,
    secondary = MerryColors.Red,
    background = MerryColors.Night,
    onBackground = MerryColors.Snow,
    surface = MerryColors.Card,
    onSurface = MerryColors.Snow,
)

@Composable
fun MerryTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
