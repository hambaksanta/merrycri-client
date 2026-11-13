// core/scene/SceneScaffold.kt
package com.hambaksanta.merrycri.core.scene

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hambaksanta.merrycri.BuildConfig
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.character.CharacterSize
import com.hambaksanta.merrycri.core.character.CharacterView
import com.hambaksanta.merrycri.core.character.Snack
import com.hambaksanta.merrycri.core.effects.Snowfall
import com.hambaksanta.merrycri.core.sensor.ShakeDetector
import com.hambaksanta.merrycri.data.model.Presence
import com.hambaksanta.merrycri.ui.theme.MerryColors

/**
 * 모든 공간이 공유하는 틀. 담당: B (캐릭터·이펙트 그리기는 A 의 컴포넌트를 쓴다)
 *
 * 레이어 (아래 → 위)
 * ① 배경  ② objects(공간 오브젝트)  ③ 캐릭터(y순)  ④ 이펙트(눈)  ⑤ UI 오버레이
 * ⑤ = 공간 이름, 접속자 수, '광장으로', 하단 버튼(bottomBar)
 *
 * 공간 담당자는 objects(②) 와 bottomBar(⑤의 하단 버튼) 만 채운다.
 *
 * 흔들기 → events/snowstorm → 모두에게 폭설. 디버그 빌드에서는 상단에 '흔들기 (디버그)' 버튼이 대신 보인다.
 */
@Composable
fun SceneScaffold(
    sceneId: SceneId,
    presence: PresenceViewModel,
    onBackToPlaza: (() -> Unit)?,
    @DrawableRes background: Int? = null,
    showSnow: Boolean = sceneId != SceneId.LOUNGE && sceneId != SceneId.POST && sceneId != SceneId.WARDROBE,
    showCharacters: Boolean = true,
    onFloorTap: ((x: Float, y: Float) -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    objects: @Composable BoxScope.() -> Unit = {},
) {
    LaunchedEffect(sceneId) { presence.enterSpace(sceneId) }
    val me by presence.me.collectAsStateWithLifecycle()
    val others by presence.others.collectAsStateWithLifecycle()
    val snowstorm by presence.snowstorm.collectAsStateWithLifecycle()

    // 서버 시간 기준 '지금'. 매 프레임 갱신해서 걷기 위치를 계산한다.
    var now by remember { mutableLongStateOf(presence.serverNow()) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { now = presence.serverNow() }
    }

    // 흔들기 감지. 화면을 떠나면 센서 해제.
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val detector = ShakeDetector(context) { presence.sendSnowstorm() }
        detector.start()
        onDispose { detector.stop() }
    }

    Box(Modifier.fillMaxSize()) {
        // ① 배경 (+ 바닥 탭 = 걷기)
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(MerryColors.Night, MerryColors.NightSky)))
                .pointerInput(sceneId) {
                    detectTapGestures { pos ->
                        val x = pos.x / size.width
                        val y = pos.y / size.height
                        if (onFloorTap != null) onFloorTap(x, y)
                        else if (y > FLOOR_TOP) presence.moveTo(x.toDouble(), y.toDouble())
                    }
                }
        ) {
            if (background != null) {
                Image(
                    painter = painterResource(background),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // ② 공간 오브젝트
        Box(Modifier.fillMaxSize(), content = objects)

        // ③ 캐릭터
        if (showCharacters) CharacterLayer(me = me, others = others, now = now)

        // ④ 이펙트
        if (showSnow) Snowfall(Modifier.fillMaxSize(), heavy = presence.isSnowstormActive(snowstorm, now))

        // ⑤ UI 오버레이
        Column(
            Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(sceneId.titleRes),
                    color = MerryColors.Snow,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (presence.isOnline) stringResource(R.string.scene_people_count, others.size + 1)
                    else stringResource(R.string.scene_offline),
                    color = MerryColors.Gold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(MerryColors.Card, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
                if (onBackToPlaza != null) {
                    Button(onClick = onBackToPlaza) { Text(stringResource(R.string.scene_back_to_plaza)) }
                }
            }
            if (BuildConfig.DEBUG) {
                // AVD 에서 흔들기 대신 쓰는 버튼
                AssistChip(
                    onClick = { presence.sendSnowstorm() },
                    label = { Text(stringResource(R.string.debug_shake), fontSize = 12.sp) },
                )
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(16.dp)
        ) { bottomBar() }
    }
}

/** 바닥으로 치는 화면 높이 비율. 이보다 위(하늘)를 탭하면 걷지 않는다. */
private const val FLOOR_TOP = 0.45f

@Composable
private fun CharacterLayer(me: Presence, others: List<Presence>, now: Long) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        val placed = (others + me).map { p -> p to Movement.positionAt(p, now) }
        // y 가 큰(화면 아래) 사람이 앞에 오도록 정렬
        placed.sortedBy { it.second.second }.forEach { (p, pos) ->
            key(p.uid) {
                val (x, y) = pos
                CharacterView(
                    outfit = p.outfit,
                    holding = Snack.of(p.holding),
                    roasted = p.roasted,
                    nick = p.nick.ifBlank { stringResource(R.string.me_default) },
                    modifier = Modifier.offset(
                        x = w * x.toFloat() - CharacterSize.width / 2,
                        y = h * y.toFloat() - CharacterSize.totalHeight,
                    ),
                )
            }
        }
    }
}

/** 아직 안 만든 공간에 띄우는 안내. 공간을 만들기 시작하면 지운다. */
@Composable
fun BoxScope.PlaceholderLabel(text: String) {
    Text(
        text = text,
        color = MerryColors.Muted,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        modifier = Modifier
            .align(Alignment.Center)
            .background(MerryColors.Card.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            .padding(20.dp),
    )
}
