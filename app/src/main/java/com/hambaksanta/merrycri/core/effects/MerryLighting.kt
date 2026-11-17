// core/effects/MerryLighting.kt
package com.hambaksanta.merrycri.core.effects

import androidx.compose.runtime.Composable
import com.google.android.filament.LightManager
import io.github.sceneview.SceneScope
import io.github.sceneview.math.Direction

/**
 * 메리크리 3D 장면 조명. 담당: A
 *
 * 크리스마스 저녁 느낌으로 빛 세 개를 둔다.
 * - 주광(key): 오른쪽 위 앞에서 오는 따뜻한 빛. 그림자를 만든다.
 * - 보조광(fill): 왼쪽 앞에서 낮게 오는 푸른 빛. 그늘이 새까맣게 되지 않게 한다.
 * - 역광(rim): 뒤에서 오는 빛. 캐릭터 윤곽을 배경에서 떼어 준다.
 *
 * 쓰는 법: Scene/SceneView 의 content 블록 안에서 MerryLighting() 한 줄만 부른다.
 * 기본 조명(mainLightNode)과 겹치면 너무 밝아지므로, 장면 쪽에서는 mainLightNode = null 로 둔다.
 *
 * 밝기 단위: DIRECTIONAL 빛은 lux. 100,000 lux 가 한낮 햇빛 정도.
 * 전체가 너무 밝거나 어두우면 [brightness] 만 바꾼다. (0.6 ~ 1.4 사이에서 조절 추천)
 */
@Composable
fun SceneScope.MerryLighting(
    mood: LightMood = LightMood.EVENING,
    brightness: Float = 1f,
    shadows: Boolean = true,
) {
    // 주광: 오른쪽 위 앞 → 왼쪽 아래 뒤
    LightNode(
        type = LightManager.Type.DIRECTIONAL,
        intensity = mood.key * brightness,
        direction = Direction(-0.5f, -1f, -0.7f),
        apply = {
            color(mood.keyColor[0], mood.keyColor[1], mood.keyColor[2])
            castShadows(shadows)
            if (shadows) shadowOptions(LightManager.ShadowOptions().apply {
                // 그림자 지도를 작게(기본 1024 → 512), 걸을 수 있는 마을 안쪽까지만 그린다.
                // 카메라(0, 8.5, 12)에서 마을 가장 먼 모서리까지 약 20m.
                mapSize = SHADOW_MAP_SIZE
                shadowFar = SHADOW_FAR_M
            })
        },
    )
    // 보조광: 왼쪽 앞 낮은 곳 → 오른쪽 뒤
    LightNode(
        type = LightManager.Type.DIRECTIONAL,
        intensity = mood.fill * brightness,
        direction = Direction(0.7f, -0.4f, -0.5f),
        apply = {
            color(mood.fillColor[0], mood.fillColor[1], mood.fillColor[2])
            castShadows(false)
        },
    )
    // 역광: 뒤 위 → 앞
    LightNode(
        type = LightManager.Type.DIRECTIONAL,
        intensity = mood.rim * brightness,
        direction = Direction(0f, -0.6f, 1f),
        apply = {
            color(mood.rimColor[0], mood.rimColor[1], mood.rimColor[2])
            castShadows(false)
        },
    )
}

/** 그림자 지도 크기(px). 그림자 가장자리가 너무 계단처럼 보이면 1024 로 올린다. */
private const val SHADOW_MAP_SIZE = 512

/** 카메라에서 이 거리(m)까지만 그림자를 그린다. 마을(World3D) 크기를 바꾸면 같이 맞춘다. */
private const val SHADOW_FAR_M = 22f

/** 시간대별 조명 값. 색은 0~1 (선형) RGB. */
enum class LightMood(
    val key: Float, val keyColor: FloatArray,
    val fill: Float, val fillColor: FloatArray,
    val rim: Float, val rimColor: FloatArray,
) {
    /** 낮: 밝고 중립적인 빛. 옷장처럼 색을 정확히 봐야 하는 화면에 쓴다. */
    DAY(
        key = 90_000f, keyColor = floatArrayOf(1.00f, 0.97f, 0.92f),
        fill = 40_000f, fillColor = floatArrayOf(0.85f, 0.90f, 1.00f),
        rim = 30_000f, rimColor = floatArrayOf(1.00f, 1.00f, 1.00f),
    ),

    /** 저녁(기본): 따뜻한 주광 + 푸른 그늘. 광장 기본값. */
    EVENING(
        key = 75_000f, keyColor = floatArrayOf(1.00f, 0.86f, 0.68f),
        fill = 35_000f, fillColor = floatArrayOf(0.62f, 0.72f, 1.00f),
        rim = 45_000f, rimColor = floatArrayOf(1.00f, 0.80f, 0.60f),
    ),

    /** 밤: 어둡고 푸른 장면. 트리 불빛이 돋보이게 할 때. */
    NIGHT(
        key = 35_000f, keyColor = floatArrayOf(0.70f, 0.78f, 1.00f),
        fill = 20_000f, fillColor = floatArrayOf(0.45f, 0.55f, 0.95f),
        rim = 40_000f, rimColor = floatArrayOf(1.00f, 0.78f, 0.55f),
    ),
}
