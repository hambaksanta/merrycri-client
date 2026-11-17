// core/scene/SceneScaffold.kt
package com.hambaksanta.merrycri.core.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.filament.Skybox
import com.hambaksanta.merrycri.BuildConfig
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.character.Character3D
import com.hambaksanta.merrycri.core.character.Snack
import com.hambaksanta.merrycri.core.effects.LightMood
import com.hambaksanta.merrycri.core.effects.MerryLighting
import com.hambaksanta.merrycri.core.effects.Snowfall
import com.hambaksanta.merrycri.core.sensor.ShakeDetector
import com.hambaksanta.merrycri.data.model.Presence
import com.hambaksanta.merrycri.ui.theme.MerryColors
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneScope
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.CameraNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * 모든 공간이 공유하는 3D 틀. 담당: B (캐릭터·소품 모양은 A 의 컴포넌트를 쓴다)
 *
 * 레이어 (아래 → 위)
 * ① 배경: 하늘색(skybox) + 눈 덮인 바닥          ← 3D
 * ② 공간 오브젝트: world { } 에 3D 노드를 넣는다   ← 3D (공간 담당자)
 * ③ 캐릭터: presence 로 받은 사람들 (깊이 순서는 3D 가 알아서)  ← 3D
 * ④ 이펙트: 눈 내리기, 폭설                        ← 2D 오버레이
 * ⑤ UI: 닉네임, 공간 이름, 접속자 수, '광장으로', 하단 버튼(bottomBar)  ← 2D 오버레이
 *
 * 공간 담당자는 world(②) 와 bottomBar(⑤의 하단 버튼) 만 채운다.
 * 2D 로 띄울 안내·패널이 있으면 objects 에 넣는다 (3D 위에 겹쳐 보임).
 *
 * 바닥을 탭하면 그 자리로 걷는다. onFloorTap 을 주면 걷는 대신 그걸 부른다 (x, y 는 0~1 비율).
 * 흔들기 → events/snowstorm → 모두에게 폭설. 디버그 빌드에서는 상단에 '흔들기 (디버그)' 버튼이 대신 보인다.
 */
@Composable
fun SceneScaffold(
    sceneId: SceneId,
    presence: PresenceViewModel,
    onBackToPlaza: (() -> Unit)?,
    skyColor: Color = MerryColors.NightSky,
    groundColor: Color = MerryColors.Snow,
    showSnow: Boolean = sceneId != SceneId.LOUNGE && sceneId != SceneId.POST && sceneId != SceneId.WARDROBE,
    showCharacters: Boolean = true,
    lightMood: LightMood = LightMood.EVENING,
    onFloorTap: ((x: Double, y: Double) -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    objects: @Composable BoxScope.() -> Unit = {},
    world: @Composable SceneScope.() -> Unit = {},
) {
    LaunchedEffect(sceneId) { presence.enterSpace(sceneId) }
    val me by presence.me.collectAsStateWithLifecycle()
    val others by presence.others.collectAsStateWithLifecycle()
    val snowstorm = presence.snowstorm.collectAsStateWithLifecycle()

    // 서버 시간 기준 '지금'. 매 프레임 갱신해서 걷기 위치를 계산한다 (Movement.kt).
    // 성능: 이 함수 본문에서는 now 를 직접 읽지 않는다. 읽으면 화면 전체가 매 프레임 다시 그려진다.
    // 캐릭터(MovingCharacter)·닉네임(NickLabel)처럼 필요한 곳에서만 읽는다.
    val now = remember { mutableLongStateOf(presence.serverNow()) }
    LaunchedEffect(Unit) {
        while (true) withFrameMillis { now.longValue = presence.serverNow() }
    }
    // 폭설 여부는 바뀔 때(켜짐/꺼짐)만 다시 그린다.
    val heavySnow by remember { derivedStateOf { presence.isSnowstormActive(snowstorm.value, now.longValue) } }

    // 흔들기 감지. 화면을 떠나면 센서 해제.
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val detector = ShakeDetector(context) { presence.sendSnowstorm() }
        detector.start()
        onDispose { detector.stop() }
    }

    val merry = LocalMerry3D.current
    val cameraNode = rememberCameraNode(merry.engine) {
        position = World3D.CameraEye
        lookAt(World3D.CameraTarget)
    }
    // 성능: SceneView 기본 후처리(SSAO·bloom)는 단색 로우폴리 장면에서 거의 안 보이고 비싸다.
    // Performance 프리셋 = SSAO·bloom 끔, 동적 해상도 켬. 대신 꺼지는 그림자는 아래 LaunchedEffect 에서 다시 켠다.
    val view = rememberView(merry.engine)
    val environmentLoader = rememberEnvironmentLoader(merry.engine)
    val environment = rememberEnvironment(environmentLoader, key = skyColor) {
        environmentLoader.createEnvironment(
            skybox = Skybox.Builder().color(skyColor.red, skyColor.green, skyColor.blue, 1f).build(merry.engine),
        )
    }
    val gestures = rememberOnGestureListener(
        onSingleTapConfirmed = { e, _ ->
            val ratio = World3D.tapToRatio(cameraNode, e.x, e.y) ?: return@rememberOnGestureListener
            if (onFloorTap != null) onFloorTap(ratio.first, ratio.second)
            else presence.moveTo(ratio.first, ratio.second)
        },
    )

    val people = if (showCharacters) others + me else emptyList()
    val viewSize = remember { mutableStateOf(IntSize.Zero) }

    Box(Modifier.fillMaxSize().onSizeChanged { viewSize.value = it }) {
        // ①②③ 3D
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = merry.engine,
            modelLoader = merry.modelLoader,
            materialLoader = merry.materialLoader,
            environmentLoader = environmentLoader,
            view = view,
            renderQuality = RenderQuality.Performance,
            environment = environment,
            mainLightNode = null, // 조명은 MerryLighting 이 담당
            fillLightNode = null,
            cameraNode = cameraNode,
            cameraManipulator = null, // 카메라 고정
            autoCenterContent = false, // 좌표 그대로 배치
            onGestureListener = gestures,
        ) {
            MerryLighting(mood = lightMood)
            Ground(groundColor)
            world()
            people.forEach { p ->
                key(p.uid) { MovingCharacter(p, now) }
            }
        }
        // Performance 프리셋이 끈 그림자를 다시 켠다. (SceneView 안의 프리셋 적용 뒤에 실행되도록 SceneView 다음에 둔다)
        // 그림자를 실제로 그릴지는 MerryLighting(shadows = …) 이 정한다.
        LaunchedEffect(view) { view.setShadowingEnabled(true) }

        // ② 2D 오버레이 (안내, 패널)
        Box(Modifier.fillMaxSize(), content = objects)

        // ⑤-1 닉네임: 머리 위 3D 좌표를 화면 좌표로 바꿔서 띄운다
        people.forEach { p ->
            key(p.uid) { NickLabel(p, now, cameraNode, viewSize) }
        }

        // ④ 이펙트
        if (showSnow) Snowfall(Modifier.fillMaxSize(), heavy = heavySnow)

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
                    modifier = Modifier
                        .background(MerryColors.Card.copy(alpha = 0.8f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
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

/** 눈 덮인 바닥. 걸을 수 있는 영역(World3D.WIDTH × DEPTH)보다 넓게 깔아서 끝이 안 보이게 한다. */
@Composable
private fun SceneScope.Ground(color: Color) {
    val m = LocalMerry3D.current.materials
    CubeNode(size = Size(60f, 0.2f, 60f), materialInstance = m.of(color), position = Position(0f, -0.1f, 0f))
}

/**
 * 캐릭터 한 명. now 는 여기서만 읽어서, 매 프레임 이 캐릭터만 다시 그린다.
 * 위치가 그대로면(서 있으면) derivedStateOf 덕분에 다시 그리지도 않는다.
 */
@Composable
private fun SceneScope.MovingCharacter(p: Presence, now: LongState) {
    val pos by remember(p) { derivedStateOf { Movement.positionAt(p, now.longValue) } }
    Character3D(
        outfit = p.outfit,
        holding = Snack.of(p.holding),
        roasted = p.roasted,
        position = World3D.toWorld(pos.first, pos.second),
        facingDeg = facing(p),
    )
}

/**
 * 머리 위 닉네임. 위치 계산을 offset { } 안(배치 단계)에서 해서, 매 프레임 글자를 다시 만들지 않고 자리만 옮긴다.
 * 카메라가 아직 화면 크기를 모르면 화면 밖에 둔다.
 */
@Composable
private fun NickLabel(p: Presence, now: LongState, cameraNode: CameraNode, viewSize: State<IntSize>) {
    val labelWidth = 96.dp
    Text(
        text = p.nick.ifBlank { stringResource(R.string.me_default) },
        color = MerryColors.Snow,
        fontSize = 11.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .offset {
                val pos = Movement.positionAt(p, now.longValue)
                val screen = World3D.worldToScreen(
                    cameraNode, World3D.toWorld(pos.first, pos.second, height = 1.75f), viewSize.value.height.toFloat(),
                ) ?: return@offset OFFSCREEN
                IntOffset(screen.first.roundToInt() - labelWidth.roundToPx() / 2, screen.second.roundToInt())
            }
            .width(labelWidth)
            .background(MerryColors.Night.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private val OFFSCREEN = IntOffset(-100_000, -100_000)

/** 걷는 방향을 바라본다. 멈춰 있으면 카메라 쪽(0도). */
private fun facing(p: Presence): Float {
    val dx = (p.tx - p.x) * World3D.WIDTH
    val dz = (p.ty - p.y) * World3D.DEPTH
    if (dx * dx + dz * dz < 1e-4) return 0f
    return Math.toDegrees(atan2(dx, dz)).toFloat()
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
            .align(Alignment.TopCenter)
            .padding(top = 120.dp)
            .background(MerryColors.Card.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
            .padding(20.dp),
    )
}
