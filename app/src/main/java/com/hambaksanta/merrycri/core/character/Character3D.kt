// core/character/Character3D.kt
package com.hambaksanta.merrycri.core.character

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import com.hambaksanta.merrycri.core.scene.LocalMerry3D
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.rememberModelInstance

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
    val instance = rememberModelInstance(merry.modelLoader, MODEL_PATH) ?: return
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

private const val MODEL_PATH = "models/merry_character.glb"

/** 기존 도형 캐릭터와 키를 맞추는 값. 광장에서 너무 크거나 작으면 이것만 바꾼다. */
private const val MODEL_SCALE = 1f

/** 이만큼(제곱 거리) 이상 움직이면 걷는 중으로 본다. */
private const val MOVE_EPSILON_SQ = 0.00001f

/** 마지막으로 움직인 뒤 이 시간 동안은 Walk 를 유지한다. (프레임 사이 위치가 안 바뀌는 순간에 멈칫하지 않게) */
private const val WALK_HOLD_NANOS = 150_000_000L
