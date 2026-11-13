// core/scene/World3D.kt
package com.hambaksanta.merrycri.core.scene

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import io.github.sceneview.collision.Vector3
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.node.CameraNode

/**
 * 3D 마을의 좌표 규칙. 담당: B
 *
 * DB(presence)의 x, y 는 그대로 0~1 비율이다. 3D 에서는 바닥(y = 0 평면) 위의 좌표로 바꿔 쓴다.
 *   비율 x 0 → 1  =  월드 X  -WIDTH/2 → +WIDTH/2  (왼쪽 → 오른쪽)
 *   비율 y 0 → 1  =  월드 Z  -DEPTH/2 → +DEPTH/2  (화면 안쪽 → 앞쪽)
 * 높이는 월드 Y (위가 +). 단위는 미터.
 */
object World3D {
    const val WIDTH = 12f
    const val DEPTH = 10f

    /** 카메라: 앞쪽 위에서 마을 가운데를 비스듬히 내려다본다 (고정, 손가락으로 돌리지 않음) */
    val CameraEye = Position(x = 0f, y = 8.5f, z = 12f)
    val CameraTarget = Position(x = 0f, y = 0f, z = 0.5f)

    fun toWorld(ratioX: Double, ratioY: Double, height: Float = 0f): Position =
        Position(
            x = ((ratioX - 0.5) * WIDTH).toFloat(),
            y = height,
            z = ((ratioY - 0.5) * DEPTH).toFloat(),
        )

    /** 월드 X, Z → 비율 (바닥 밖이면 null) */
    fun toRatio(worldX: Float, worldZ: Float): Pair<Double, Double>? {
        val rx = worldX / WIDTH + 0.5
        val ry = worldZ / DEPTH + 0.5
        return if (rx in 0.0..1.0 && ry in 0.0..1.0) rx to ry else null
    }

    /**
     * 화면을 탭한 지점(px)이 바닥의 어디인지 비율로 돌려준다. 바닥 밖이면 null.
     * 카메라에서 탭한 방향으로 쏜 광선이 y = 0 평면과 만나는 점을 구한다.
     */
    fun tapToRatio(cameraNode: CameraNode, xPx: Float, yPx: Float): Pair<Double, Double>? {
        val ray = cameraNode.screenPointToRay(xPx, yPx)
        val o = ray.getOrigin()
        val d = ray.getDirection()
        if (d.y >= -1e-4f) return null // 하늘 쪽
        val t = -o.y / d.y
        return toRatio(o.x + d.x * t, o.z + d.z * t)
    }

    /**
     * 월드 좌표가 화면 어디에 보이는지 (px, 화면 위쪽 기준). 닉네임 말풍선 위치에 쓴다.
     * 카메라가 아직 화면 크기를 모르면 null.
     */
    @Suppress("DEPRECATION")
    fun worldToScreen(cameraNode: CameraNode, p: Position, viewHeightPx: Float): Pair<Float, Float>? {
        val s = cameraNode.worldToScreenPoint(Vector3(p.x, p.y, p.z))
        if (s.x <= 2f && s.y <= 2f) return null // viewport 가 아직 없음 (0~1 비율로 돌아옴)
        return s.x to (viewHeightPx - s.y) // Filament 는 아래가 0
    }
}

/** 색 → 재질. 같은 색은 한 번만 만든다. */
class MaterialCache(private val loader: MaterialLoader) {
    private val cache = HashMap<ULong, MaterialInstance>()

    fun of(color: Color): MaterialInstance =
        cache.getOrPut(color.value) { loader.createColorInstance(color, metallic = 0f, roughness = 0.85f) }
}

/**
 * 공간이 바뀌어도 3D 엔진은 하나만 쓴다 (MerryNavHost 에서 만들어 내려준다).
 * 화면마다 엔진을 새로 만들면 전환할 때 느려지고 메모리를 많이 쓴다.
 */
class Merry3D(
    val engine: Engine,
    val modelLoader: ModelLoader,
    val materialLoader: MaterialLoader,
    val materials: MaterialCache,
)

val LocalMerry3D = staticCompositionLocalOf<Merry3D> { error("Merry3D 가 없습니다. MerryNavHost 안에서만 쓸 수 있어요.") }
