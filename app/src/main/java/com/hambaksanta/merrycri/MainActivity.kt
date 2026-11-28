// MainActivity.kt
package com.hambaksanta.merrycri

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.hambaksanta.merrycri.data.AuthState
import com.hambaksanta.merrycri.databinding.ActivityMainBinding
import com.hambaksanta.merrycri.notify.DebugMenu
import com.hambaksanta.merrycri.notify.GiftOpenWorker
import com.hambaksanta.merrycri.notify.NotificationHelper
import com.hambaksanta.merrycri.ui.game.GameActivity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** v2의 단일 Activity. Navigation Component로 Fragment 전환을 담당한다. 담당: B */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val app get() = application as MerryCriApp
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) GiftOpenWorker.scheduleChristmas(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeNewLetters()

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNavigation.setupWithNavController(navController)
        if (BuildConfig.DEBUG) {
            binding.bottomNavigation.findViewById<View>(R.id.navigation_tree)
                ?.setOnLongClickListener {
                    DebugMenu.show(this)
                    true
                }
        }
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
            if (destination.id == R.id.navigation_tree) {
                binding.root.post {
                    if (!isFinishing && !isDestroyed && navController.currentDestination?.id == R.id.navigation_tree) {
                        requestLetterNotificationsOnce()
                    }
                }
            }
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

    private fun observeNewLetters() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.repository.authState.collectLatest { state ->
                    if (state != AuthState.SignedIn) return@collectLatest
                    val uid = app.repository.uid ?: return@collectLatest
                    app.repository.observeOrnaments(uid)
                        .onEach { ornaments ->
                            val arrivals = ornaments.associate { it.writerUid to it.createdAt }
                            if (app.settings.recordLetterArrivals(uid, arrivals)) {
                                NotificationHelper.showNewLetterNotification(this@MainActivity)
                            }
                        }
                        .catch { error -> Log.w("MainActivity", "편지 알림 처리 실패", error) }
                        .collect()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.audio.resume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            GiftOpenWorker.scheduleChristmas(this)
        }
    }

    private fun requestLetterNotificationsOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return

        val preferences = getPreferences(MODE_PRIVATE)
        if (preferences.getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false)) return
        preferences.edit().putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true).apply()
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onStop() {
        app.audio.pause()
        super.onStop()
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUESTED = "letter_notification_permission_requested"
        private val FULL_SCREEN_DESTINATIONS = setOf(R.id.introFragment, R.id.onboardingFragment)
    }
}
