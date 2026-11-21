// MainActivity.kt
package com.hambaksanta.merrycri

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.hambaksanta.merrycri.databinding.ActivityMainBinding

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
        // 첫 화면·온보딩에서는 하단 탭을 숨긴다
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = destination.id !in FULL_SCREEN_DESTINATIONS
        }
        // 앱을 새로 켤 때만 첫 화면(편지 트리 인트로)을 홈 위에 띄운다. 회전 등으로 다시 만들 때는 건너뜀.
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

    override fun onStop() {
        app.audio.pause()
        super.onStop()
    }

    companion object {
        private val FULL_SCREEN_DESTINATIONS = setOf(R.id.introFragment, R.id.onboardingFragment)
    }
}
