// ui/letter/FriendTreeFragment.kt
package com.hambaksanta.merrycri.ui.letter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hambaksanta.merrycri.databinding.FragmentFriendTreeBinding

/** 친구 트리를 방문하는 화면. 실제 UI는 C가 구현한다. */
class FriendTreeFragment : Fragment() {
    private var _binding: FragmentFriendTreeBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFriendTreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
