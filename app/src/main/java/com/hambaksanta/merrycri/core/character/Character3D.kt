// core/character/Character3D.kt
package com.hambaksanta.merrycri.core.character

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import com.hambaksanta.merrycri.core.scene.LocalMerry3D
import com.hambaksanta.merrycri.core.scene.Merry3D
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.model.Model
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.utils.readBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 메리 캐릭터 한 명을 그린다. 담당: A
 *
 * 기존 도형(Cube/Cylinder/Sphere) 캐릭터를 merry_character.glb 로 바꾼 버전.
 * 함수 이름과 파라미터는 그대로라서, 광장(SceneScaffold)과 옷장(WardrobeScene)은 고칠 게 없다.
 *
 * - 옷·색·간식: outfit / holding / roasted 가 바뀌면 바로 갈아입는다.
 * - 동작: sitting 이면 Sit, 위치가 움직이고 있으면 Walk, 아니면 Idle.
 *   걷는지는 위치 변화로 직접 알아내므로 isWalking 값을 따로 넘기지 않아도 된다.
 * - 모델이 불러와지는 첫 몇 프레임 동안은 아무것도 그리지 않는다.
 */
@Composable
fun SceneScope.Character3D(
    outfit: Outfit,
    holding: Snack?,
    position: Position,
    facingDeg: Float = 0f,
    scale: Float = 1f,
    sitting: Boolean = false,
    roasted: Boolean = false,
) {
    val merry = LocalMerry3D.current

    // 캐릭터마다 자기 모델을 하나씩 가진다. (옷·색이 서로 섞이지 않게)
    // 화면에서 빠지면 모델도 엔진에서 지운다 (rememberCharacterInstance 참고).
    val instance = rememberCharacterInstance(merry) ?: return
    val character = remember(instance) { MerryCharacter(merry.engine, instance) }

    // 옷 갈아입기
    LaunchedEffect(character, outfit, holding, roasted) {
        character.applyOutfit(outfit, holding, roasted)
    }

    // 동작 재생: 매 프레임 위치를 보고 걷는지 판단한 뒤 알맞은 애니메이션을 튼다.
    val currentPosition by rememberUpdatedState(position)
    val isSitting by rememberUpdatedState(sitting)
    LaunchedEffect(character) {
        var clip = MerryCharacter.IDLE
        var clipTime = 0f
        var lastFrame = 0L
        var lastPos = currentPosition
        var lastMoveAt = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastFrame == 0L) 0f else (now - lastFrame) / 1_000_000_000f
                lastFrame = now

                val p = currentPosition
                val dx = p.x - lastPos.x
                val dz = p.z - lastPos.z
                if (dx * dx + dz * dz > MOVE_EPSILON_SQ) lastMoveAt = now
                lastPos = p
                val walking = lastMoveAt != 0L && now - lastMoveAt < WALK_HOLD_NANOS

                val next = MerryCharacter.pick(walking = walking, sitting = isSitting)
                if (next != clip) { clip = next; clipTime = 0f } else clipTime += dt
                character.play(clip, clipTime)
            }
        }
    }

    ModelNode(
        modelInstance = instance,
        autoAnimate = false,          // 애니메이션은 위에서 직접 고른다
        scaleToUnits = null,          // glb 크기 그대로 (키 약 1.2, 발바닥이 원점)
        centerOrigin = null,
        position = position,
        rotation = Rotation(y = facingDeg),
        scale = Scale(scale * MODEL_SCALE),
    )
}

/**
 * 캐릭터 모델을 불러오고, 화면에서 빠지면 지운다.
 *
 * SceneView 의 rememberModelInstance 는 모델을 만들기만 하고 지우지 않는다.
 * (ModelNode 가 사라질 때 노드만 지우고, 메시·텍스처·재질이 든 모델 본체는 ModelLoader 가 끝날 때까지 남는다)
 * 그래서 공간을 옮기거나 사람이 들어왔다 나갈 때마다 1.3MB 짜리 모델이 계속 쌓였다.
 */
@Composable
private fun rememberCharacterInstance(merry: Merry3D): ModelInstance? {
    val context = LocalContext.current
    var instance by remember(merry) { mutableStateOf<ModelInstance?>(null) }
    LaunchedEffect(merry) {
        val buffer = withContext(Dispatchers.IO) {
            runCatching { context.assets.readBuffer(MODEL_PATH) }.getOrNull()
        } ?: return@LaunchedEffect
        // 만들기와 저장 사이에 멈추는 지점이 없어서, 만들어 놓고 놓치는 일은 없다.
        instance = runCatching { merry.modelLoader.createModelInstance(buffer) }.getOrNull()
    }
    DisposableEffect(instance) {
        val created = instance
        onDispose { if (created != null) releaseModel(merry, created.asset) }
    }
    return instance
}

/**
 * 모델 본체를 엔진에서 지운다. 바로 지우지 않고 메인 스레드 다음 차례에 지운다.
 * - 같은 순간에 ModelNode 가 씬에서 빠지고 노드를 정리하므로, 그게 다 끝난 뒤에 지워야 안전하다.
 *   (노드 정리 → 모델 지우기 순서는 SceneView 가 앱 종료 때 ModelLoader.clear() 로 하는 것과 같다)
 * - 텍스처를 아직 불러오는 중이면 지우면 안 된다 (Filament ResourceLoader 규칙). 끝날 때까지 기다린다.
 * - 앱이 끝나 엔진이 이미 정리됐으면 아무것도 안 한다 (ModelLoader 가 이미 지웠다).
 */
private fun releaseModel(merry: Merry3D, model: Model, tries: Int = 0) {
    val delayMs = if (tries == 0) 0L else RELEASE_RETRY_MS
    mainHandler.postDelayed({
        if (!merry.engine.isValid) return@postDelayed
        when {
            merry.modelLoader.progress >= 1f -> merry.modelLoader.destroyModel(model)
            tries < RELEASE_MAX_TRIES -> releaseModel(merry, model, tries + 1)
            // 텍스처 로딩이 계속 안 끝나면 지우지 않는다 (모델 1개가 남는 게 앱이 죽는 것보다 낫다)
            else -> Log.w(TAG, "캐릭터 모델 정리 건너뜀: 텍스처 로딩이 끝나지 않음")
        }
    }, delayMs)
}

private val mainHandler = Handler(Looper.getMainLooper())
private const val RELEASE_RETRY_MS = 100L
private const val RELEASE_MAX_TRIES = 50 // 약 5초
private const val TAG = "Character3D"

private const val MODEL_PATH = "models/merry_character.glb"

/** 기존 도형 캐릭터와 키를 맞추는 값. 광장에서 너무 크거나 작으면 이것만 바꾼다. */
private const val MODEL_SCALE = 1f

/** 이만큼(제곱 거리) 이상 움직이면 걷는 중으로 본다. */
private const val MOVE_EPSILON_SQ = 0.00001f

/** 마지막으로 움직인 뒤 이 시간 동안은 Walk 를 유지한다. (프레임 사이 위치가 안 바뀌는 순간에 멈칫하지 않게) */
private const val WALK_HOLD_NANOS = 150_000_000L
