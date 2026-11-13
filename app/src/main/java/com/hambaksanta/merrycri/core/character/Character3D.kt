// core/character/Character3D.kt
package com.hambaksanta.merrycri.core.character

import androidx.compose.runtime.Composable
import com.hambaksanta.merrycri.core.scene.LocalMerry3D
import com.hambaksanta.merrycri.ui.theme.MerryColors
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size

/**
 * 3D 캐릭터 한 명. 담당: A
 * 발바닥이 position(바닥) 에 닿게 서 있고, 키는 약 1.3m (scale 1 기준).
 *
 * 지금은 기본 도형(원기둥·구·상자·원뿔)을 쌓은 임시판이다.
 * TODO(A): 로우폴리 glb 로 바꾸기. 부위별로 따로 만든 glb 를 같은 자리에 겹쳐 놓고 색만 바꾸는 방식을 추천.
 * 레이어 순서(2D 때와 같음): 몸 → 부츠 → 상의 → 장갑 → 목도리 → 모자 → 손에 든 것
 *
 * @param facingDeg 바라보는 방향 (Y축 회전, 도). 0 = 카메라 쪽
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
    val m = LocalMerry3D.current.materials
    val lift = if (sitting) -0.12f else 0f

    Node(position = position, rotation = Rotation(y = facingDeg), scale = Scale(scale)) {
        // 부츠
        val boots = m.of(outfit.bootsColor())
        CubeNode(size = Size(0.14f, 0.12f, 0.22f), materialInstance = boots, position = Position(-0.1f, 0.06f, 0.02f))
        CubeNode(size = Size(0.14f, 0.12f, 0.22f), materialInstance = boots, position = Position(0.1f, 0.06f, 0.02f))

        // 상의 (몸통)
        CylinderNode(radius = 0.25f, height = 0.62f, materialInstance = m.of(outfit.topColor()), position = Position(0f, 0.43f + lift, 0f))
        if (outfit.topType() == Top.SANTA) {
            CylinderNode(radius = 0.26f, height = 0.06f, materialInstance = m.of(MerryColors.Snow), position = Position(0f, 0.16f + lift, 0f))
        }

        // 장갑
        if (outfit.glovesType() != Gloves.NONE) {
            val gloves = m.of(outfit.glovesColor())
            SphereNode(radius = 0.08f, materialInstance = gloves, position = Position(-0.31f, 0.42f + lift, 0.04f))
            SphereNode(radius = 0.08f, materialInstance = gloves, position = Position(0.31f, 0.42f + lift, 0.04f))
        }

        // 머리 + 눈
        SphereNode(radius = 0.22f, materialInstance = m.of(MerryColors.Skin), position = Position(0f, 0.96f + lift, 0f))
        val eye = m.of(MerryColors.Night)
        SphereNode(radius = 0.025f, materialInstance = eye, position = Position(-0.07f, 0.99f + lift, 0.2f))
        SphereNode(radius = 0.025f, materialInstance = eye, position = Position(0.07f, 0.99f + lift, 0.2f))

        // 목도리
        if (outfit.scarfType() != Scarf.NONE) {
            val scarf = m.of(outfit.scarfColor())
            CylinderNode(radius = 0.24f, height = 0.08f, materialInstance = scarf, position = Position(0f, 0.76f + lift, 0f))
            CubeNode(size = Size(0.07f, 0.2f, 0.04f), materialInstance = scarf, position = Position(0.1f, 0.64f + lift, 0.24f))
        }

        // 모자
        val hat = m.of(outfit.hatColor())
        val snow = m.of(MerryColors.Snow)
        when (outfit.hatType()) {
            Hat.NONE -> Unit
            Hat.BEANIE -> {
                CylinderNode(radius = 0.21f, height = 0.16f, materialInstance = hat, position = Position(0f, 1.12f + lift, 0f))
                SphereNode(radius = 0.06f, materialInstance = snow, position = Position(0f, 1.24f + lift, 0f))
            }
            Hat.SANTA -> {
                CylinderNode(radius = 0.23f, height = 0.07f, materialInstance = snow, position = Position(0f, 1.1f + lift, 0f))
                ConeNode(radius = 0.2f, height = 0.38f, materialInstance = hat, position = Position(0f, 1.32f + lift, 0f))
                SphereNode(radius = 0.06f, materialInstance = snow, position = Position(0f, 1.52f + lift, 0f))
            }
            Hat.EARMUFF -> {
                SphereNode(radius = 0.08f, materialInstance = hat, position = Position(-0.22f, 0.96f + lift, 0f))
                SphereNode(radius = 0.08f, materialInstance = hat, position = Position(0.22f, 0.96f + lift, 0f))
            }
            Hat.ANTLER -> {
                CylinderNode(radius = 0.025f, height = 0.25f, materialInstance = hat, position = Position(-0.12f, 1.25f + lift, 0f), rotation = Rotation(z = 20f))
                CylinderNode(radius = 0.025f, height = 0.25f, materialInstance = hat, position = Position(0.12f, 1.25f + lift, 0f), rotation = Rotation(z = -20f))
            }
        }

        // 손에 든 간식. TODO(A): 간식 glb 로 바꾸기
        if (holding != null) {
            val c = when (holding) {
                Snack.COCOA -> MerryColors.Red
                Snack.LATTE, Snack.AMERICANO -> MerryColors.Snow
                Snack.GINGERBREAD -> MerryColors.Cookie
                Snack.CHOCOLATE -> MerryColors.Choco
                Snack.MARSHMALLOW -> if (roasted) MerryColors.Cookie else MerryColors.Snow
            }
            CubeNode(size = Size(0.12f), materialInstance = m.of(c), position = Position(0.34f, 0.52f + lift, 0.12f))
        }
    }
}
