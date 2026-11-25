// ui/leaderboard/ScoreAdapter.kt
package com.hambaksanta.merrycri.ui.leaderboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.data.model.Score
import com.hambaksanta.merrycri.databinding.ItemScoreBinding

/** 리더보드 한 줄(순위 · 닉네임 · 점수). 내 줄은 [myUid]와 비교해 강조한다. 담당: C */
class ScoreAdapter(private val myUid: String?) : ListAdapter<Score, ScoreAdapter.ViewHolder>(DIFF) {

    class ViewHolder(val binding: ItemScoreBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val score = getItem(position)
        val context = holder.binding.root.context
        val rank = position + 1
        val isMe = myUid != null && myUid == score.writerUid

        holder.binding.rankBadge.text = rank.toString()
        holder.binding.rankBadge.background.mutate().setTint(
            ContextCompat.getColor(
                context,
                when (rank) {
                    1 -> R.color.gold
                    2 -> R.color.silver
                    3 -> R.color.bronze
                    else -> R.color.ink_muted
                },
            ),
        )

        holder.binding.scoreNicknameText.text =
            if (isMe) score.nickname + context.getString(R.string.leaderboard_me_suffix) else score.nickname
        holder.binding.scoreValueText.text = context.getString(R.string.leaderboard_score_format, score.best)
        holder.binding.scoreRow.setBackgroundResource(if (isMe) R.drawable.bg_my_rank else R.drawable.bg_score_row)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Score>() {
            override fun areItemsTheSame(oldItem: Score, newItem: Score) = oldItem.writerUid == newItem.writerUid
            override fun areContentsTheSame(oldItem: Score, newItem: Score) = oldItem == newItem
        }
    }
}
