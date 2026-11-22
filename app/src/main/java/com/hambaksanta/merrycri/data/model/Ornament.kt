// data/model/Ornament.kt
package com.hambaksanta.merrycri.data.model

/** ornaments/{ownerUid}/{writerUid}에 저장되는 장식. */
data class Ornament(
    val writerUid: String,
    val type: OrnamentType,
    val slot: Int,
    val createdAt: Long,
)

enum class OrnamentType(val wireValue: String) {
    BAUBLE("bauble"),
    STAR("star"),
    CANE("cane"),
    SNOW("snow"),
    GIFT("gift"),
    BELL("bell");

    companion object {
        fun fromWireValue(value: String): OrnamentType? = entries.firstOrNull { it.wireValue == value }
    }
}
