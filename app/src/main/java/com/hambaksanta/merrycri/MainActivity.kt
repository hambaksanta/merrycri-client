// MainActivity.kt
package com.hambaksanta.merrycri

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.hambaksanta.merrycri.databinding.ActivityMainBinding
import com.hambaksanta.merrycri.ui.game.GameActivity

/** v2의 단일 Activity. Navigation Component로 Fragment 전환을 담당한다. 담당: B */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val app get() = application as MerryCriApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNavigation.setupWithNavController(navController)
        // '미니게임' 탭은 바로 Fragment로 가지 않고, 내 트리의 리더보드(+게임 시작하기)부터 보여줘야 해서 직접 처리한다.
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.navigation_game) {
                val myUid = FirebaseAuth.getInstance().currentUser?.uid
                startActivity(
                    if (myUid != null) GameActivity.forOwner(this, myUid) else GameActivity.practice(this),
                )
                true
            } else {
                NavigationUI.onNavDestinationSelected(item, navController)
            }
        }
        // 첫 화면·온보딩에서는 하단 탭을 숨긴다
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = destination.id !in FULL_SCREEN_DESTINATIONS
        }
        // 앱을 새로 켤 때만 첫 화면(편지 트리 인트로)을 시작 화면(온보딩) 위에 띄운다. 회전 등으로 다시 만들 때는 건너뜀.
        // 인트로가 끝나면 닫히고, 아래 온보딩이 기존 사용자는 홈으로, 새 사용자는 닉네임 입력으로 보낸다.
        if (savedInstanceState == null) {
            navController.navigate(
                R.id.introFragment,
                null,
                navOptions {
                    anim {
                        popEnter = android.R.anim.fade_in
                        popExit = android.R.anim.fade_out
                    }
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        app.audio.resume()
    }

    override fun onResume() {
        super.onResume()
        // GameActivity의 '홈으로 가기' 버튼에서 돌아온 경우, 트리(홈) 탭을 선택해 보여준다.
        if (pendingHomeTab) {
            pendingHomeTab = false
            binding.bottomNavigation.selectedItemId = R.id.navigation_tree
        }
    }

    override fun onStop() {
        app.audio.pause()
        super.onStop()
    }

    companion object {
        private val FULL_SCREEN_DESTINATIONS = setOf(R.id.introFragment, R.id.onboardingFragment)

        /** GameActivity의 '홈으로 가기' 버튼이 true로 설정하고 finish()한다. 담당: C(게임) → 소비는 B(MainActivity) 영역. */
        var pendingHomeTab = false
    }
}
