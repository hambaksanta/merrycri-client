// MainActivity.kt
package com.hambaksanta.merrycri

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
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
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = destination.id != R.id.onboardingFragment
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
}
