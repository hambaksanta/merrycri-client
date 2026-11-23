// ui/letter/WriteFragment.kt
package com.hambaksanta.merrycri.ui.letter

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.databinding.FragmentLetterWriteBinding

/** 롤링페이퍼 편지 작성 화면. 담당: C */
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.letterInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(text: Editable?) {
                    binding.letterInputLayout.error = null
                    binding.saveButton.isEnabled = !text.isNullOrBlank()
                }
            },
        )

        binding.saveButton.setOnClickListener {
            val content = binding.letterInput.text?.toString().orEmpty()
            if (content.isBlank()) {
                binding.letterInputLayout.error = getString(R.string.letter_write_error_blank)
                return@setOnClickListener
            }
            binding.letterInputLayout.error = null
            // TODO: Firebase 저장 연결. data/ 에 편지 저장용 repository 함수가 아직 없어서(B에게 요청 필요) 미구현.
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
