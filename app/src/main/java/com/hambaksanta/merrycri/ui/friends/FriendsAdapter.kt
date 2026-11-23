// ui/friends/FriendsAdapter.kt
package com.hambaksanta.merrycri.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.data.model.FriendProfile
import com.hambaksanta.merrycri.databinding.ItemFriendBinding

/** 친구 목록 줄. 탭하면 그 친구 트리의 리더보드로 이동한다(onClick). 담당: C */
class FriendsAdapter(
    private val onClick: (FriendProfile) -> Unit,
) : ListAdapter<FriendListItem, FriendsAdapter.ViewHolder>(DIFF) {

    class ViewHolder(val binding: ItemFriendBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.binding.root.context
        holder.binding.avatarText.text = item.friend.nickname.take(1)
        holder.binding.nicknameText.text = item.friend.nickname
        holder.binding.topScoreText.text = item.topScore?.let { score ->
            context.getString(R.string.friends_top_score_format, score.nickname, score.best)
        } ?: context.getString(R.string.friends_no_score_yet)
        holder.binding.root.setOnClickListener { onClick(item.friend) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FriendListItem>() {
            override fun areItemsTheSame(oldItem: FriendListItem, newItem: FriendListItem) =
                oldItem.friend.uid == newItem.friend.uid

            override fun areContentsTheSame(oldItem: FriendListItem, newItem: FriendListItem) =
                oldItem == newItem
        }
    }
}
