// ui/letter/OpenLettersFragment.kt
package com.hambaksanta.merrycri.ui.letter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hambaksanta.merrycri.databinding.FragmentLetterReadBinding

/** 받은 편지를 여는 화면. 실제 UI는 C가 구현한다. */
class OpenLettersFragment : Fragment() {
    private var _binding: FragmentLetterReadBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLetterReadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
