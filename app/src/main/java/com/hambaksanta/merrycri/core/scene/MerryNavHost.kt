// core/scene/MerryNavHost.kt
package com.hambaksanta.merrycri.core.scene

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.core.effects.SnowstormTransition
import com.hambaksanta.merrycri.data.AuthState
import com.hambaksanta.merrycri.feature.lounge.LoungeScene
import com.hambaksanta.merrycri.feature.plaza.PlazaScene
import com.hambaksanta.merrycri.feature.post.PostOfficeScene
import com.hambaksanta.merrycri.feature.sled.SledScene
import com.hambaksanta.merrycri.feature.snowball.SnowballScene
import com.hambaksanta.merrycri.feature.snowman.SnowmanScene
import com.hambaksanta.merrycri.feature.tree.TreeScene
import com.hambaksanta.merrycri.feature.wardrobe.WardrobeScene
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader

/**
 * 화면 이동. 옷장 → 광장 → 각 공간. 담당: B
 * 3D 엔진은 여기서 하나만 만들어 모든 공간이 같이 쓴다 (LocalMerry3D).
 * 공간이 바뀌면 화면을 하얗게 덮었다가 걷히는 '눈보라 전환'을 보여준다.
 */
@Composable
fun MerryNavHost() {
    val app = LocalContext.current.applicationContext as MerryCriApp
    val authState by app.repository.authState.collectAsStateWithLifecycle()

    when (authState) {
        AuthState.Loading -> AuthLoadingScreen()
        AuthState.Error -> AuthErrorScreen(onRetry = { app.repository.signIn() })
        AuthState.SignedIn, AuthState.Offline -> MerryWorldNavHost()
    }
}

@Composable
private fun MerryWorldNavHost() {
    val nav = rememberNavController()
    val presence: PresenceViewModel = viewModel()
    val backToPlaza: () -> Unit = { nav.popBackStack(SceneId.PLAZA.route, inclusive = false) }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val merry3d = remember(engine, modelLoader, materialLoader) {
        Merry3D(engine, modelLoader, materialLoader, MaterialCache(materialLoader))
    }

    // 눈보라 전환: 경로가 바뀌는 순간 하얗게 덮고 0.9초 동안 걷힌다 (첫 화면은 제외)
    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val whiteout = remember { Animatable(0f) }
    var isFirstRoute by remember { mutableStateOf(true) }
    LaunchedEffect(route) {
        if (route == null) return@LaunchedEffect
        if (isFirstRoute) {
            isFirstRoute = false
            return@LaunchedEffect
        }
        whiteout.snapTo(1f)
        whiteout.animateTo(0f, tween(durationMillis = 900))
    }

    CompositionLocalProvider(LocalMerry3D provides merry3d) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = SceneId.WARDROBE.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
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
            SnowstormTransition(progress = whiteout.value)
        }
    }
}
