// core/scene/Props3D.kt
package com.hambaksanta.merrycri.core.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hambaksanta.merrycri.ui.theme.MerryColors
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size

/*
 * 공간마다 쓰는 임시 3D 소품. 담당: B (모양 다듬기·glb 교체는 A, 공간별 배치는 각 공간 담당)
 * 전부 기본 도형으로 만든 로우폴리 자리표시다. position 은 바닥 위 발 위치(월드 좌표, 미터).
 * 비율 좌표로 놓고 싶으면 World3D.toWorld(x, y) 를 쓴다.
 *
 * TODO(A): Kenney Holiday Kit 같은 CC0 glb 로 하나씩 바꾸기 → rememberModelInstance + ModelNode
 */

@Composable
fun SceneScope.PineTree3D(position: Position, height: Float = 3f, star: Boolean = true) {
    val m = LocalMerry3D.current.materials
    val trunk = height * 0.15f
    val layer = (height - trunk) / 3f
    Node(position = position) {
        CylinderNode(radius = height * 0.06f, height = trunk, materialInstance = m.of(MerryColors.Brown), position = Position(0f, trunk / 2f, 0f))
        for (i in 0..2) {
            val r = height * (0.38f - i * 0.09f)
            ConeNode(radius = r, height = layer * 1.4f, materialInstance = m.of(MerryColors.Pine), position = Position(0f, trunk + layer * i + layer * 0.7f, 0f))
        }
        if (star) SphereNode(radius = height * 0.05f, materialInstance = m.of(MerryColors.Gold), position = Position(0f, height + 0.1f, 0f))
    }
}

@Composable
fun SceneScope.House3D(position: Position, wall: Color = MerryColors.Wall, rotationY: Float = 0f) {
    val m = LocalMerry3D.current.materials
    Node(position = position, rotation = Rotation(y = rotationY)) {
        CubeNode(size = Size(2f, 1.6f, 1.8f), materialInstance = m.of(wall), position = Position(0f, 0.8f, 0f))
        // 지붕: 네모뿔 (옆면 4개짜리 원뿔을 45도 돌림) + 눈
        ConeNode(radius = 1.6f, height = 1.1f, sideCount = 4, materialInstance = m.of(MerryColors.Snow), position = Position(0f, 2.15f, 0f), rotation = Rotation(y = 45f))
        CubeNode(size = Size(0.45f, 0.8f, 0.05f), materialInstance = m.of(MerryColors.Brown), position = Position(0f, 0.4f, 0.92f))
        val window = m.of(MerryColors.Gold)
        CubeNode(size = Size(0.4f, 0.35f, 0.05f), materialInstance = window, position = Position(-0.6f, 1.05f, 0.92f))
        CubeNode(size = Size(0.4f, 0.35f, 0.05f), materialInstance = window, position = Position(0.6f, 1.05f, 0.92f))
    }
}

@Composable
fun SceneScope.Lamp3D(position: Position) {
    val m = LocalMerry3D.current.materials
    Node(position = position) {
        CylinderNode(radius = 0.05f, height = 1.8f, materialInstance = m.of(MerryColors.Night), position = Position(0f, 0.9f, 0f))
        SphereNode(radius = 0.16f, materialInstance = m.of(MerryColors.Gold), position = Position(0f, 1.9f, 0f))
    }
}

@Composable
fun SceneScope.Signpost3D(position: Position, color: Color = MerryColors.Red) {
    val m = LocalMerry3D.current.materials
    Node(position = position) {
        CylinderNode(radius = 0.05f, height = 1.3f, materialInstance = m.of(MerryColors.Brown), position = Position(0f, 0.65f, 0f))
        CubeNode(size = Size(0.8f, 0.3f, 0.06f), materialInstance = m.of(color), position = Position(0f, 1.15f, 0f))
    }
}

@Composable
fun SceneScope.Snowman3D(position: Position, scarf: Color = MerryColors.Red) {
    val m = LocalMerry3D.current.materials
    val snow = m.of(MerryColors.Snow)
    Node(position = position) {
        SphereNode(radius = 0.45f, materialInstance = snow, position = Position(0f, 0.42f, 0f))
        SphereNode(radius = 0.32f, materialInstance = snow, position = Position(0f, 1.05f, 0f))
        SphereNode(radius = 0.22f, materialInstance = snow, position = Position(0f, 1.5f, 0f))
        CylinderNode(radius = 0.3f, height = 0.08f, materialInstance = m.of(scarf), position = Position(0f, 1.3f, 0f))
        ConeNode(radius = 0.05f, height = 0.25f, materialInstance = m.of(MerryColors.Fire), position = Position(0f, 1.5f, 0.3f), rotation = Rotation(x = 90f))
    }
}

@Composable
fun SceneScope.GiftBox3D(position: Position, box: Color = MerryColors.Red, ribbon: Color = MerryColors.Gold, size: Float = 0.5f) {
    val m = LocalMerry3D.current.materials
    Node(position = position) {
        CubeNode(size = Size(size), materialInstance = m.of(box), position = Position(0f, size / 2f, 0f))
        CubeNode(size = Size(size * 1.02f, size * 1.02f, size * 0.15f), materialInstance = m.of(ribbon), position = Position(0f, size / 2f, 0f))
        CubeNode(size = Size(size * 0.15f, size * 1.02f, size * 1.02f), materialInstance = m.of(ribbon), position = Position(0f, size / 2f, 0f))
    }
}

@Composable
fun SceneScope.Fireplace3D(position: Position, fireLevel: Int = 2) {
    val m = LocalMerry3D.current.materials
    Node(position = position) {
        CubeNode(size = Size(2.4f, 2f, 0.6f), materialInstance = m.of(MerryColors.Stone), position = Position(0f, 1f, 0f))
        CubeNode(size = Size(1.4f, 0.9f, 0.1f), materialInstance = m.of(MerryColors.Night), position = Position(0f, 0.6f, 0.28f))
        // TODO(C): 불 일렁임. 지금은 단계만큼 크기가 다른 공
        if (fireLevel > 0) {
            SphereNode(radius = 0.15f + fireLevel * 0.1f, materialInstance = m.of(MerryColors.Fire), position = Position(0f, 0.45f, 0.3f))
        }
    }
}

@Composable
fun SceneScope.Sofa3D(position: Position, color: Color = MerryColors.Red, rotationY: Float = 0f) {
    val m = LocalMerry3D.current.materials
    val c = m.of(color)
    Node(position = position, rotation = Rotation(y = rotationY)) {
        CubeNode(size = Size(1.6f, 0.4f, 0.7f), materialInstance = c, position = Position(0f, 0.2f, 0f))
        CubeNode(size = Size(1.6f, 0.6f, 0.2f), materialInstance = c, position = Position(0f, 0.6f, -0.25f))
    }
}
