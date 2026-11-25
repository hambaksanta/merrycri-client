// ui/tree/TreeLettersSheet.kt
package com.hambaksanta.merrycri.ui.tree

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.data.model.Letter
import com.hambaksanta.merrycri.databinding.ItemTreeLetterBinding
import com.hambaksanta.merrycri.databinding.SheetTreeLettersBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 트리 탭의 '받은 편지' 아래 시트. 담당: A
 * 상태는 넷 중 하나: 불러오는 중 / 아직 편지 없음 / 12/25 전 잠금 / 편지 목록(누르면 펼치고 읽음 처리)
 */
class TreeLettersSheet(
    context: Context,
    private val onOpenLetter: (Letter) -> Unit,
) : BottomSheetDialog(context) {

    private val binding = SheetTreeLettersBinding.inflate(LayoutInflater.from(context))
    private val adapter = LetterAdapter()

    init {
        setContentView(binding.root)
        binding.listLetters.layoutManager = LinearLayoutManager(context)
        binding.listLetters.adapter = adapter
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    fun showLoading(count: Int) = with(binding) {
        textSheetTitle.text = context.getString(R.string.tree_letters_title, count)
        textSheetSub.isVisible = false
        display(progress = true)
    }

    fun showEmpty() = with(binding) {
        textSheetTitle.setText(R.string.tree_letters_empty_title)
        textSheetSub.isVisible = false
        imageLocked.setImageResource(R.drawable.orn_bauble)
        textLockedTitle.setText(R.string.tree_letters_empty_title_card)
        textLockedBody.setText(R.string.tree_letters_empty_body)
        display(locked = true)
    }

    /** 12/25 전. [ornamentType]이 있으면 누른 장식 그림으로 보여준다 */
    fun showLocked(count: Int, daysLeft: Long, ornamentType: String?) = with(binding) {
        textSheetTitle.text = context.getString(R.string.tree_letters_title, count)
        textSheetSub.setText(R.string.tree_letters_sub_locked)
        textSheetSub.isVisible = true
        imageLocked.setImageResource(ornamentType?.let(TreeArt::ornamentRes) ?: R.drawable.orn_gift)
        textLockedTitle.text = if (daysLeft > 0) {
            context.getString(R.string.tree_letters_locked_title, daysLeft.toInt())
        } else {
            context.getString(R.string.tree_letters_locked_title_soon)
        }
        textLockedBody.text = if (ornamentType != null) {
            context.getString(R.string.tree_letters_locked_ornament)
        } else {
            context.getString(R.string.tree_letters_locked_body, count)
        }
        display(locked = true)
    }

    fun showError() = with(binding) {
        textSheetSub.isVisible = false
        display(error = true)
    }

    /** 편지 목록. [focus]가 있으면 그 편지를 펼치고 그 자리로 스크롤한다 */
    fun showLetters(letters: List<Letter>, focus: Letter?) = with(binding) {
        textSheetTitle.text = context.getString(R.string.tree_letters_title, letters.size)
        textSheetSub.setText(R.string.tree_letters_sub_open)
        textSheetSub.isVisible = true
        adapter.submit(letters, focus?.id)
        display(list = true)
        if (focus != null) {
            val index = letters.indexOfFirst { it.id == focus.id }
            if (index >= 0) {
                listLetters.scrollToPosition(index)
                onOpenLetter(focus)
            }
        }
    }

    private fun display(progress: Boolean = false, locked: Boolean = false, error: Boolean = false, list: Boolean = false) =
        with(binding) {
            progressLetters.isVisible = progress
            layoutLocked.isVisible = locked
            textLettersError.isVisible = error
            listLetters.isVisible = list
        }

    private inner class LetterAdapter : RecyclerView.Adapter<LetterAdapter.Holder>() {
        private var items: List<Letter> = emptyList()
        private val expanded = mutableSetOf<String>()
        private val read = mutableSetOf<String>()
        private val dateFormat = SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }

        fun submit(list: List<Letter>, focusId: String?) {
            items = list
            expanded.clear()
            focusId?.let { expanded += it; read += it }
            @Suppress("NotifyDataSetChanged")
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemTreeLetterBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
            val letter = items[position]
            val isOpen = letter.id in expanded
            imageOrnament.setImageResource(TreeArt.ornamentRes(letter.ornamentType))
            textSender.text = letter.senderNickname
            textDate.text = dateFormat.format(Date(letter.createdAt))
            textContent.text = letter.content
            textContent.maxLines = if (isOpen) Int.MAX_VALUE else 2
            textNew.isVisible = !letter.opened && letter.id !in read
            cardLetter.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
                val item = items[pos]
                if (!expanded.remove(item.id)) {
                    expanded += item.id
                    if (read.add(item.id)) onOpenLetter(item)
                }
                notifyItemChanged(pos)
            }
        }

        inner class Holder(val binding: ItemTreeLetterBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
