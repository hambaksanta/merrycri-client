// ui/game/GameActivity.kt
package com.hambaksanta.merrycri.ui.game

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.core.audio.Sfx
import com.hambaksanta.merrycri.databinding.ActivityGameBinding
import com.hambaksanta.merrycri.ui.leaderboard.ScoreAdapter
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
    private val scoreAdapter by lazy { ScoreAdapter(FirebaseAuth.getInstance().currentUser?.uid) }

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
            startPlaying()
        }
        binding.rankPlayButton.setOnClickListener {
            binding.rankOverlay.isVisible = false
            startPlaying()
        }
        setUpLeaderboardPreview()
        showPreGameScreen()
        binding.retryButton.setOnClickListener {
            binding.resultOverlay.isVisible = false
            binding.resultNote.isVisible = false
            resetHud()
            binding.breakoutView.restart()
            startPlaying()
        }
        binding.closeButton.setOnClickListener {
            binding.resultOverlay.isVisible = false
            binding.resultNote.isVisible = false
            resetHud()
            binding.breakoutView.restart()
            showPreGameScreen()
        }
        binding.exitButton.setOnClickListener { onExitRequested() }
        binding.rankBackButton.setOnClickListener { finish() }
    }

    /** 친구 트리 모드면 1단계(순위) 화면, 연습 모드면 바로 2단계(설명) 화면을 보여준다. 둘 다 상단 바는 숨긴다. */
    private fun showPreGameScreen() {
        binding.topBar.isVisible = false
        if (ownerUid != null) {
            binding.rankOverlay.isVisible = true
        } else {
            binding.startOverlay.isVisible = true
        }
    }

    /** 실제 플레이 시작. 이때만 상단 바(타이머·하트·점수·나가기)가 보인다. */
    private fun startPlaying() {
        binding.topBar.isVisible = true
        binding.breakoutView.start()
    }

    private fun resetHud() {
        binding.timeText.text = getString(R.string.game_time_format, 60)
        binding.livesText.text = heartsText(3)
        binding.scoreText.text = getString(R.string.game_score_format, 0)
        onComboChanged(0)
    }

    /** 상단 나가기(✕) 버튼. 아직 시작 전/결과 화면이면 바로 닫고, 플레이 중이면 확인 후 닫는다. */
    private fun onExitRequested() {
        if (binding.rankOverlay.isVisible || binding.startOverlay.isVisible || binding.resultOverlay.isVisible) {
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
        if (!binding.rankOverlay.isVisible && !binding.startOverlay.isVisible && !binding.resultOverlay.isVisible) {
            binding.breakoutView.resumeGame()
        }
    }

    override fun onScoreChanged(score: Int) {
        binding.scoreText.text = getString(R.string.game_score_format, score)
        pulseScore()
    }

    override fun onLivesChanged(lives: Int) {
        binding.livesText.text = heartsText(lives)
    }

    override fun onTimeChanged(secondsLeft: Int) {
        binding.timeText.text = getString(R.string.game_time_format, secondsLeft)
    }

    override fun onBrickHit(golden: Boolean, scoreGained: Int) {
        app.audio.playSfx(if (golden) Sfx.CHIME else Sfx.SNAP)
        showScorePopup(scoreGained)
    }

    override fun onLifeLost() {
        app.audio.playSfx(Sfx.THUD)
    }

    override fun onLevelUp(level: Int) {
        Toast.makeText(this, getString(R.string.game_level_up_format, level), Toast.LENGTH_SHORT).show()
    }

    /** 콤보 배율(x1~x3)에 따라 점수 글자 색·크기를 조금씩 키워서 연속 성공을 눈에 띄게 한다. */
    override fun onComboChanged(multiplier: Int) {
        val (colorRes, sizeSp) = when {
            multiplier >= 3 -> R.color.berry to 22f
            multiplier == 2 -> R.color.white to 19f
            else -> R.color.gold to 17f
        }
        binding.scoreText.setTextColor(getColor(colorRes))
        binding.scoreText.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    override fun onGameOver(finalScore: Int) {
        app.audio.playSfx(Sfx.BELL)
        binding.topBar.isVisible = false
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
                .onFailure { error ->
                    // 순위에 점수가 안 보인다는 문의가 있어 원인을 바로 확인할 수 있도록 임시로 에러를 노출한다.
                    android.util.Log.e("GameActivity", "saveBestScore 실패: owner=$owner", error)
                    Toast.makeText(
                        this@GameActivity,
                        "점수 저장 실패: ${error.message}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
        }
    }

    /** 친구 트리에서 들어온 경우(=owner 있음)만 [rank_overlay]의 순위 목록을 실시간으로 채운다. */
    private fun setUpLeaderboardPreview() {
        val owner = ownerUid ?: return
        binding.rankScoreRecycler.layoutManager = LinearLayoutManager(this)
        binding.rankScoreRecycler.adapter = scoreAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.repository.observeScores(owner).collect { scores ->
                    scoreAdapter.submitList(scores)
                    binding.rankBoardEmptyText.isVisible = scores.isEmpty()
                    binding.rankScoreRecycler.isVisible = scores.isNotEmpty()
                }
            }
        }
    }

    /** 목숨은 오른쪽 정렬, 잃은 목숨(빈 하트)이 왼쪽부터 보이도록 한다. */
    private fun heartsText(lives: Int): String {
        val filled = lives.coerceIn(0, MAX_LIVES)
        val empty = MAX_LIVES - filled
        return (List(empty) { HEART_EMPTY_CHAR } + List(filled) { HEART_CHAR }).joinToString(" ")
    }

    /** 점수가 오를 때마다 점수 글자가 살짝 커졌다 돌아오는 하트비트 효과. */
    private fun pulseScore() {
        binding.scoreText.animate().cancel()
        binding.scoreText.scaleX = 1f
        binding.scoreText.scaleY = 1f
        binding.scoreText.animate()
            .scaleX(1.35f)
            .scaleY(1.35f)
            .setDuration(90)
            .withEndAction {
                binding.scoreText.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            .start()
    }

    /** 벽돌이 깨질 때마다 "+N" 점수가 위로 떠오르며 사라지는 효과. */
    private fun showScorePopup(delta: Int) {
        val popup = binding.scorePopupText
        popup.animate().cancel()
        popup.text = getString(R.string.game_score_popup_format, delta)
        popup.translationY = 0f
        popup.alpha = 1f
        popup.animate()
            .translationY(-60f)
            .alpha(0f)
            .setDuration(650)
            .withEndAction {
                popup.alpha = 0f
                popup.translationY = 0f
            }
            .start()
    }

    companion object {
        private const val HEART_CHAR = "♥"
        private const val HEART_EMPTY_CHAR = "♡"
        private const val MAX_LIVES = 3
        private const val EXTRA_OWNER_UID = "owner_uid"

        /** 친구 트리 화면에서 미니게임을 시작할 때 쓴다. 결과가 그 친구의 리더보드에 반영된다. */
        fun forOwner(context: Context, ownerUid: String): Intent =
            Intent(context, GameActivity::class.java).putExtra(EXTRA_OWNER_UID, ownerUid)

        /** 하단 '미니게임' 탭에서 바로 시작하는 연습 모드. 점수는 리더보드에 저장되지 않는다. */
        fun practice(context: Context): Intent = Intent(context, GameActivity::class.java)
    }
}
