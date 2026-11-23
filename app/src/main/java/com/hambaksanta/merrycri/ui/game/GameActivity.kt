// ui/game/GameActivity.kt
package com.hambaksanta.merrycri.ui.game

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.audio.Sfx
import com.hambaksanta.merrycri.databinding.ActivityGameBinding
import kotlinx.coroutines.launch

/**
 * 선물 벽돌깨기 전용 Activity. 담당: C
 *
 * [EXTRA_OWNER_UID]가 있으면(친구 트리에서 들어온 경우) 종료 시 그 친구의 리더보드(`scores/{ownerUid}/{나}`)에
 * 최고점을 기록한다. 없으면(하단 '미니게임' 탭에서 바로 들어온 경우) 연습 모드로, 점수만 보여주고 저장하지 않는다.
 * 친구 트리 화면이 생기면 [forOwner]로 이 Activity를 띄우면 된다.
 */
class GameActivity : AppCompatActivity(), BreakoutListener {
    private lateinit var binding: ActivityGameBinding
    private val app get() = application as MerryCriApp
    private val ownerUid get() = intent.getStringExtra(EXTRA_OWNER_UID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.breakoutView.listener = this
        binding.timeText.text = getString(R.string.game_time_format, 60)
        binding.livesText.text = heartsText(3)
        binding.scoreText.text = getString(R.string.game_score_format, 0)

        binding.startOverlay.setOnClickListener {
            binding.startOverlay.isVisible = false
            binding.breakoutView.start()
        }
        binding.retryButton.setOnClickListener {
            binding.resultOverlay.isVisible = false
            binding.resultNote.isVisible = false
            binding.startOverlay.isVisible = true
            binding.breakoutView.restart()
            binding.timeText.text = getString(R.string.game_time_format, 60)
            binding.livesText.text = heartsText(3)
            binding.scoreText.text = getString(R.string.game_score_format, 0)
        }
        binding.closeButton.setOnClickListener { finish() }
        binding.exitButton.setOnClickListener { onExitRequested() }
    }

    /** 상단 나가기(✕) 버튼. 아직 시작 전/결과 화면이면 바로 닫고, 플레이 중이면 확인 후 닫는다. */
    private fun onExitRequested() {
        if (binding.startOverlay.isVisible || binding.resultOverlay.isVisible) {
            finish()
            return
        }
        binding.breakoutView.pauseGame()
        AlertDialog.Builder(this)
            .setTitle(R.string.game_exit_confirm_title)
            .setMessage(R.string.game_exit_confirm_message)
            .setPositiveButton(R.string.game_exit_confirm_positive) { _, _ -> finish() }
            .setNegativeButton(R.string.game_exit_confirm_negative) { _, _ -> binding.breakoutView.resumeGame() }
            .setOnCancelListener { binding.breakoutView.resumeGame() }
            .show()
    }

    override fun onPause() {
        super.onPause()
        binding.breakoutView.pauseGame()
    }

    override fun onResume() {
        super.onResume()
        if (!binding.startOverlay.isVisible && !binding.resultOverlay.isVisible) {
            binding.breakoutView.resumeGame()
        }
    }

    override fun onScoreChanged(score: Int) {
        binding.scoreText.text = getString(R.string.game_score_format, score)
    }

    override fun onLivesChanged(lives: Int) {
        binding.livesText.text = heartsText(lives)
    }

    override fun onTimeChanged(secondsLeft: Int) {
        binding.timeText.text = getString(R.string.game_time_format, secondsLeft)
    }

    override fun onBrickHit(golden: Boolean) {
        app.audio.playSfx(if (golden) Sfx.CHIME else Sfx.SNAP)
    }

    override fun onLifeLost() {
        app.audio.playSfx(Sfx.THUD)
    }

    override fun onGameOver(finalScore: Int) {
        app.audio.playSfx(Sfx.BELL)
        binding.resultScore.text = getString(R.string.game_score_format, finalScore)
        binding.resultNote.isVisible = false
        binding.resultOverlay.isVisible = true

        val owner = ownerUid ?: return
        lifecycleScope.launch {
            runCatching { app.repository.saveBestScore(owner, finalScore) }
                .onSuccess { isNewBest ->
                    if (isNewBest) {
                        binding.resultNote.text = getString(R.string.game_new_best)
                        binding.resultNote.isVisible = true
                    }
                }
        }
    }

    private fun heartsText(lives: Int): String =
        List(lives.coerceAtLeast(0)) { HEART_CHAR }.joinToString(" ")

    companion object {
        private const val HEART_CHAR = "♥"
        private const val EXTRA_OWNER_UID = "owner_uid"

        /** 친구 트리 화면에서 미니게임을 시작할 때 쓴다. 결과가 그 친구의 리더보드에 반영된다. */
        fun forOwner(context: Context, ownerUid: String): Intent =
            Intent(context, GameActivity::class.java).putExtra(EXTRA_OWNER_UID, ownerUid)

        /** 하단 '미니게임' 탭에서 바로 시작하는 연습 모드. 점수는 리더보드에 저장되지 않는다. */
        fun practice(context: Context): Intent = Intent(context, GameActivity::class.java)
    }
}
