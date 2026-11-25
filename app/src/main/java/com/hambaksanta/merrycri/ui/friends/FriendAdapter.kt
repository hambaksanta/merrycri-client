// ui/friends/FriendAdapter.kt
package com.hambaksanta.merrycri.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.databinding.ItemFriendBinding

/**
 * 친구 목록 줄. 카드(root)를 탭하면 그 친구 트리를 구경하러 간다(onTreeClick).
 * "편지 쓰기" 칩을 탭하면 편지 쓰기 화면으로(onWriteLetterClick, 오늘 이미 썼으면 숨김),
 * "게임하러 가기" 칩을 탭하면 그 친구 트리 경쟁 모드로 미니게임을 띄운다(onPlayGameClick). 담당: C
 */
class FriendAdapter(
    private val onTreeClick: (FriendListItem) -> Unit = {},
    private val onWriteLetterClick: (FriendListItem) -> Unit = {},
    private val onPlayGameClick: (FriendListItem) -> Unit = {},
) : ListAdapter<FriendListItem, FriendAdapter.FriendViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding, onTreeClick, onWriteLetterClick, onPlayGameClick)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FriendViewHolder(
        private val binding: ItemFriendBinding,
        private val onTreeClick: (FriendListItem) -> Unit,
        private val onWriteLetterClick: (FriendListItem) -> Unit,
        private val onPlayGameClick: (FriendListItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendListItem) = with(binding) {
            friendNickname.text = item.nickname
            friendCode.text = root.context.getString(R.string.friends_code_value, item.friendCode)
            topScoreText.text = item.topScore?.let { score ->
                root.context.getString(R.string.friends_top_score_format, score.nickname, score.best)
            } ?: root.context.getString(R.string.friends_no_score_yet)
            friendTreeStage.text = root.context.getString(
                when (item.treeStage) {
                    1 -> R.string.tree_stage_1
                    2 -> R.string.tree_stage_2
                    3 -> R.string.tree_stage_3
                    4 -> R.string.tree_stage_4
                    else -> R.string.tree_stage_5
                },
            )
            friendLetterCount.text = root.context.getString(
                R.string.friends_letter_count,
                item.letterCount,
            )
            friendTreeImage.setImageResource(
                when (item.treeStage) {
                    1 -> R.drawable.tree_lv1
                    2 -> R.drawable.tree_lv2
                    3 -> R.drawable.tree_lv3
                    4 -> R.drawable.tree_lv4
                    else -> R.drawable.tree_lv5
                },
            )
            // 편지는 하루 한 통. 오늘 이미 썼으면 "편지 쓰기" 칩을 숨기고 '오늘 보냄' 표시만 남긴다.
            statusDone.isVisible = item.wroteToday
            statusTodo.isVisible = !item.wroteToday
            statusTodo.setOnClickListener { onWriteLetterClick(item) }
            playGameButton.setOnClickListener { onPlayGameClick(item) }
            // 카드 전체(root)를 탭하면 그 친구 트리 구경 화면으로 이동한다. 편지/게임 버튼은 각자 클릭을 따로 소비한다.
            root.setOnClickListener { onTreeClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FriendListItem>() {
        override fun areItemsTheSame(oldItem: FriendListItem, newItem: FriendListItem): Boolean =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: FriendListItem, newItem: FriendListItem): Boolean =
            oldItem == newItem
    }
}
