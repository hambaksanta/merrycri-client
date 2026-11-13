// core/scene/MerryNavHost.kt
package com.hambaksanta.merrycri.core.scene

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hambaksanta.merrycri.feature.lounge.LoungeScene
import com.hambaksanta.merrycri.feature.plaza.PlazaScene
import com.hambaksanta.merrycri.feature.post.PostOfficeScene
import com.hambaksanta.merrycri.feature.sled.SledScene
import com.hambaksanta.merrycri.feature.snowball.SnowballScene
import com.hambaksanta.merrycri.feature.snowman.SnowmanScene
import com.hambaksanta.merrycri.feature.tree.TreeScene
import com.hambaksanta.merrycri.feature.wardrobe.WardrobeScene

/**
 * 화면 이동. 옷장 → 광장 → 각 공간. 담당: B
 * TODO(B): 기획서의 '눈보라 화이트아웃 전환'으로 바꾸기 (지금은 페이드)
 */
@Composable
fun MerryNavHost() {
    val nav = rememberNavController()
    val presence: PresenceViewModel = viewModel()
    val backToPlaza: () -> Unit = { nav.popBackStack(SceneId.PLAZA.route, inclusive = false) }

    NavHost(
        navController = nav,
        startDestination = SceneId.WARDROBE.route,
        enterTransition = { fadeIn(tween(500)) },
        exitTransition = { fadeOut(tween(500)) },
    ) {
        composable(SceneId.WARDROBE.route) {
            WardrobeScene(presence) {
                // 처음 시작이든 광장에서 옷장에 온 것이든, 스택을 비우고 광장 하나만 남긴다
                nav.navigate(SceneId.PLAZA.route) { popUpTo(nav.graph.id) { inclusive = true } }
            }
        }
        composable(SceneId.PLAZA.route) {
            PlazaScene(
                presence = presence,
                onGo = { nav.navigate(it.route) },
                onWardrobe = { nav.navigate(SceneId.WARDROBE.route) },
            )
        }
        composable(SceneId.TREE.route) { TreeScene(presence, backToPlaza) }
        composable(SceneId.POST.route) { PostOfficeScene(presence, backToPlaza) }
        composable(SceneId.LOUNGE.route) { LoungeScene(presence, backToPlaza) }
        composable(SceneId.SNOWMAN.route) { SnowmanScene(presence, backToPlaza) }
        composable(SceneId.SNOWBALL.route) { SnowballScene(presence, backToPlaza) }
        composable(SceneId.SLED.route) { SledScene(presence, backToPlaza) }
    }
}
