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
 * 친구 목록 줄. "게임하러 가기" 칩을 탭하면 그 친구 트리 경쟁 모드로 미니게임을 띄우고(onClick),
 * "편지 쓰기"/"편지 또 쓰기" 칩을 탭하면 그 친구에게 편지 쓰기 화면으로 바로 이동한다(onWriteLetterClick).
 * 두 동작 모두 전용 버튼으로만 트리거되며, 카드 전체 탭에는 아무 동작도 연결하지 않는다. 담당: C
 */
class FriendAdapter(
    private val onClick: (FriendListItem) -> Unit = {},
    private val onWriteLetterClick: (FriendListItem) -> Unit = {},
) : ListAdapter<FriendListItem, FriendAdapter.FriendViewHolder>(DiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FriendViewHolder(binding, onClick, onWriteLetterClick)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FriendViewHolder(
        private val binding: ItemFriendBinding,
        private val onClick: (FriendListItem) -> Unit,
        private val onWriteLetterClick: (FriendListItem) -> Unit,
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
            statusDone.isVisible = item.hasWritten
            statusTodo.isVisible = !item.hasWritten
            writeAgainButton.isVisible = item.hasWritten
            // "편지 쓰기"/"편지 또 쓰기"/"게임하러 가기" 칩은 각자 따로 클릭을 소비한다.
            // 카드 전체(root)에는 클릭 리스너를 걸지 않아, 의도치 않게 게임으로 넘어가는 일이 없게 한다.
            statusTodo.setOnClickListener { onWriteLetterClick(item) }
            writeAgainButton.setOnClickListener { onWriteLetterClick(item) }
            playGameButton.setOnClickListener { onClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FriendListItem>() {
        override fun areItemsTheSame(oldItem: FriendListItem, newItem: FriendListItem): Boolean =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: FriendListItem, newItem: FriendListItem): Boolean =
            oldItem == newItem
    }
}
