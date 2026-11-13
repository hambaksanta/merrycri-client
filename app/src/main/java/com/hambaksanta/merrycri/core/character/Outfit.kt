// core/character/Outfit.kt
package com.hambaksanta.merrycri.core.character

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.ui.theme.MerryColors

/*
 * 캐릭터 옷과 손에 든 간식. 담당: A
 *
 * Firebase 에는 소문자 id 문자열로 저장한다 (예: "beanie", "red", "cocoa").
 * id 를 바꾸면 이미 저장된 데이터가 깨지니, 바꿀 때는 세 명이 합의한다.
 * 화면에 보이는 이름은 strings.xml (labelRes) 에 있다.
 */

enum class PaletteColor(val id: String, val color: Color) {
    RED("red", MerryColors.Red),
    GREEN("green", MerryColors.Pine),
    WHITE("white", MerryColors.Snow),
    CREAM("cream", MerryColors.Cream),
    NAVY("navy", MerryColors.Navy),
    GOLD("gold", MerryColors.Gold),
    PINK("pink", MerryColors.Pink),
    BROWN("brown", MerryColors.Brown);

    companion object {
        fun of(id: String?): PaletteColor = entries.firstOrNull { it.id == id } ?: RED
    }
}

enum class Hat(val id: String, @StringRes val labelRes: Int) {
    NONE("none", R.string.hat_none), BEANIE("beanie", R.string.hat_beanie), SANTA("santa", R.string.hat_santa),
    EARMUFF("earmuff", R.string.hat_earmuff), ANTLER("antler", R.string.hat_antler);

    companion object { fun of(id: String?): Hat = entries.firstOrNull { it.id == id } ?: BEANIE }
}

enum class Scarf(val id: String, @StringRes val labelRes: Int) {
    NONE("none", R.string.scarf_none), STRIPE("stripe", R.string.scarf_stripe),
    KNIT("knit", R.string.scarf_knit), SOLID("solid", R.string.scarf_solid);

    companion object { fun of(id: String?): Scarf = entries.firstOrNull { it.id == id } ?: STRIPE }
}

enum class Gloves(val id: String, @StringRes val labelRes: Int) {
    NONE("none", R.string.gloves_none), MITTEN("mitten", R.string.gloves_mitten), KNIT("knit", R.string.gloves_knit);

    companion object { fun of(id: String?): Gloves = entries.firstOrNull { it.id == id } ?: MITTEN }
}

enum class Top(val id: String, @StringRes val labelRes: Int) {
    PADDING("padding", R.string.top_padding), SWEATER("sweater", R.string.top_sweater),
    COAT("coat", R.string.top_coat), SANTA("santa", R.string.top_santa);

    companion object { fun of(id: String?): Top = entries.firstOrNull { it.id == id } ?: SWEATER }
}

enum class Boots(val id: String, @StringRes val labelRes: Int) {
    SHOES("shoes", R.string.boots_shoes), FUR("fur", R.string.boots_fur), RAIN("rain", R.string.boots_rain);

    companion object { fun of(id: String?): Boots = entries.firstOrNull { it.id == id } ?: FUR }
}

/** presence.holding 값. 마시멜로를 구웠는지는 presence.roasted, 몇 입 먹었는지는 presence.bites. */
enum class Snack(val id: String, @StringRes val labelRes: Int) {
    COCOA("cocoa", R.string.snack_cocoa),
    LATTE("latte", R.string.snack_latte),
    AMERICANO("americano", R.string.snack_americano),
    GINGERBREAD("gingerbread", R.string.snack_gingerbread),
    CHOCOLATE("chocolate", R.string.snack_chocolate),
    MARSHMALLOW("marshmallow", R.string.snack_marshmallow);

    companion object { fun of(id: String?): Snack? = entries.firstOrNull { it.id == id } }
}

/** DB: users/{uid}/outfit/colors, presence/.../outfit/colors */
data class OutfitColors(
    var hat: String = PaletteColor.RED.id,
    var scarf: String = PaletteColor.GREEN.id,
    var gloves: String = PaletteColor.WHITE.id,
    var top: String = PaletteColor.RED.id,
    var boots: String = PaletteColor.BROWN.id,
)

/**
 * DB: { hat, scarf, gloves, top, boots, colors: { hat, scarf, gloves, top, boots } }
 * Firebase 가 읽을 수 있게 전부 var + 기본값. 코드에서는 아래 확장 함수(hatType() 등)로 enum 을 꺼내 쓴다.
 * (클래스 안에 getter 를 만들면 Firebase 가 그 값까지 DB 에 써 버리므로 확장 함수로 둔다)
 */
data class Outfit(
    var hat: String = Hat.BEANIE.id,
    var scarf: String = Scarf.STRIPE.id,
    var gloves: String = Gloves.MITTEN.id,
    var top: String = Top.SWEATER.id,
    var boots: String = Boots.FUR.id,
    var colors: OutfitColors = OutfitColors(),
)

fun Outfit.hatType(): Hat = Hat.of(hat)
fun Outfit.scarfType(): Scarf = Scarf.of(scarf)
fun Outfit.glovesType(): Gloves = Gloves.of(gloves)
fun Outfit.topType(): Top = Top.of(top)
fun Outfit.bootsType(): Boots = Boots.of(boots)

fun Outfit.hatColor(): Color = PaletteColor.of(colors.hat).color
fun Outfit.scarfColor(): Color = PaletteColor.of(colors.scarf).color
fun Outfit.glovesColor(): Color = PaletteColor.of(colors.gloves).color
fun Outfit.topColor(): Color = PaletteColor.of(colors.top).color
fun Outfit.bootsColor(): Color = PaletteColor.of(colors.boots).color
