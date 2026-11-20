// ui/letter/WriteFragment.kt
package com.hambaksanta.merrycri.ui.letter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hambaksanta.merrycri.databinding.FragmentLetterWriteBinding

/** 롤링페이퍼 편지 작성 화면. 실제 UI는 C가 구현한다. */
class WriteFragment : Fragment() {
    private var _binding: FragmentLetterWriteBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLetterWriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
