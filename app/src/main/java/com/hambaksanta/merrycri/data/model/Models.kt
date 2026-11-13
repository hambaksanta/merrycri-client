// data/model/Models.kt
package com.hambaksanta.merrycri.data.model

import com.google.firebase.database.Exclude
import com.hambaksanta.merrycri.core.character.Outfit

/*
 * Firebase Realtime Database 데이터 모델. 담당: B
 * merrycri-server 저장소의 docs/DATA_MODEL.md 와 같아야 한다.
 * 필드를 바꾸려면 세 명이 합의하고, 두 저장소를 같이 고친다.
 *
 * Firebase 가 읽어올 수 있도록 모든 필드는 var + 기본값.
 * DB 에 없는 값(키로 들어오는 uid, id 등)은 @get:Exclude 로 막고, 읽은 뒤 snapshot.key 로 채운다.
 * 시간은 전부 서버 시간(ms). 쓸 때는 ServerValue.TIMESTAMP.
 */

/** DB 경로를 한곳에 모아 둔다. 문자열로 경로를 직접 쓰지 말고 이걸 쓴다. */
object DbPath {
    const val USERS = "users"
    const val ROOMS = "rooms"
    const val PRESENCE = "presence"
    const val SNOWSTORM = "events/snowstorm"
    const val ORNAMENTS = "tree/ornaments"
    const val GIFTS = "gifts"
    const val DEMO_GIFTS = "demo/gifts"
    const val FIRE = "lounge/fire"
    const val SNOWMEN = "snowmen"
    const val SERVER_TIME_OFFSET = ".info/serverTimeOffset"

    fun presence(spaceId: String) = "$ROOMS/$spaceId/$PRESENCE"

    /** gifts/{giftId}.to 가 이 값이면 '아무나' 선물 */
    const val ANYONE = "anyone"
}

/** users/{uid} */
data class UserProfile(
    var nick: String = "",
    var outfit: Outfit = Outfit(),
)

/**
 * rooms/{spaceId}/presence/{uid}
 * 위치는 출발점(x, y)·목적지(tx, ty)·출발 시각(moveStartAt)만 보내고,
 * 지금 어디쯤인지는 각 폰이 계산한다 (core/scene/Movement.kt).
 * x, y, tx, ty 는 화면 비율 0~1.
 */
data class Presence(
    /** DB 에는 없고 키(snapshot.key)로 들어온다 */
    @get:Exclude var uid: String = "",
    var nick: String = "",
    var outfit: Outfit = Outfit(),
    var x: Double = 0.5,
    var y: Double = 0.8,
    var tx: Double = 0.5,
    var ty: Double = 0.8,
    var moveStartAt: Long = 0,
    /** null | cocoa | latte | americano | gingerbread | chocolate | marshmallow (core/character/Snack) */
    var holding: String? = null,
    /** 들고 있는 간식을 몇 입 먹었는지 */
    var bites: Int = 0,
    /** 마시멜로를 구웠는지 */
    var roasted: Boolean = false,
    var updatedAt: Long = 0,
)

/** events/snowstorm — 흔들기 폭설. at 이 오래된 이벤트(10초 넘음)는 무시한다. */
data class SnowstormEvent(
    var by: String = "",
    var at: Long = 0,
)

/** tree/ornaments/{id} */
data class Ornament(
    /** DB 에는 없고 키(snapshot.key)로 들어온다 */
    @get:Exclude var id: String = "",
    var type: String = "ball_red",
    var x: Double = 0.5,
    var y: Double = 0.5,
    var uid: String = "",
    var nick: String = "",
    var createdAt: Long = 0,
)

/** gifts/{giftId}, demo/gifts/{giftId} (데모는 잠금 없이 열람, 채점용) */
data class Gift(
    /** DB 에는 없고 키(snapshot.key)로 들어온다 */
    @get:Exclude var id: String = "",
    var fromUid: String = "",
    var fromNick: String = "",
    /** 받는 사람 uid 또는 DbPath.ANYONE */
    var to: String = DbPath.ANYONE,
    var letter: String = "",
    var paper: String = "snow",
    var box: String = "red",
    var ribbon: String = "gold",
    var createdAt: Long = 0,
    var openedBy: String? = null,
    var openedAt: Long? = null,
)

/**
 * lounge/fire
 * 장작을 넣을 때만 runTransaction 으로 level + 1 (최대 3), lastFedAt = 서버 시간.
 * 줄어드는 것은 DB 에 쓰지 않고 currentLevel() 로 계산만 한다.
 */
data class FireState(
    var level: Int = 0,
    var lastFedAt: Long = 0,
) {
    /** 현재 단계 = level - (지금 - lastFedAt) / 30초, 최소 0 */
    fun currentLevel(serverNow: Long): Int {
        val dropped = ((serverNow - lastFedAt).coerceAtLeast(0) / FIRE_STEP_MS).toInt()
        return (level - dropped).coerceIn(0, MAX_LEVEL)
    }

    companion object {
        const val MAX_LEVEL = 3
        const val FIRE_STEP_MS = 30_000L
    }
}

/**
 * snowmen/{id} — 1인 최대 3개 (만들기 전에 내 uid 로 개수를 세서 막는다)
 * TODO(A): parts 안의 키(몸·머리·모자·목도리·코 등)를 정해서 세 명 합의 후 DATA_MODEL.md 에 적기
 */
data class Snowman(
    /** DB 에는 없고 키(snapshot.key)로 들어온다 */
    @get:Exclude var id: String = "",
    var uid: String = "",
    var nick: String = "",
    var x: Double = 0.5,
    var y: Double = 0.8,
    var parts: Map<String, String> = emptyMap(),
    var createdAt: Long = 0,
) {
    companion object { const val MAX_PER_USER = 3 }
}
