// ui/game/GameActivity.kt
package com.hambaksanta.merrycri.ui.game

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hambaksanta.merrycri.databinding.ActivityGameBinding

/** 선물 벽돌깨기 전용 Activity. 실제 게임은 C가 구현한다. */
class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
