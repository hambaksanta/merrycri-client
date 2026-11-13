// MainActivity.kt
package com.hambaksanta.merrycri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hambaksanta.merrycri.core.scene.MerryNavHost
import com.hambaksanta.merrycri.ui.theme.MerryTheme

/** 담당: B. 앱이 화면에서 사라지면 BGM을 멈추고, 돌아오면 이어서 튼다. */
class MainActivity : ComponentActivity() {
    private val app get() = application as MerryCriApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MerryTheme {
                MerryNavHost()
            }
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
