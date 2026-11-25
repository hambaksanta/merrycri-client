// ui/letter/WriteFragment.kt
package com.hambaksanta.merrycri.ui.letter

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.hambaksanta.merrycri.MerryCriApp
import com.hambaksanta.merrycri.R
import com.hambaksanta.merrycri.databinding.FragmentLetterWriteBinding
import kotlinx.coroutines.launch

/** 롤링페이퍼 편지 작성 화면. 담당: C */
class WriteFragment : Fragment() {
    // FirebaseRepository.FRIEND_CODE_LENGTH는 private이라 직접 참조 못 해서 값만 맞춰 둔다.
    private val friendCodeLength = 6
    private var _binding: FragmentLetterWriteBinding? = null
    private val binding get() = requireNotNull(_binding)
    private val app get() = requireActivity().application as MerryCriApp

    private var selectedOrnamentType: String? = null
    private var selectedOrnamentView: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLetterWriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applyPrefilledRecipient()

        binding.backButton.setOnClickListener { findNavController().popBackStack() }

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

        listOf(
            binding.ornamentBauble to "bauble",
            binding.ornamentStar to "star",
            binding.ornamentCane to "cane",
            binding.ornamentSnow to "snow",
            binding.ornamentGift to "gift",
            binding.ornamentBell to "bell",
        ).forEach { (ornamentView, ornamentType) ->
            ornamentView.setOnClickListener { selectOrnament(ornamentView, ornamentType) }
        }

        binding.saveButton.setOnClickListener { submitLetter() }
    }

    /** 친구 목록에서 특정 친구에게 바로 쓰기로 들어온 경우, 받는 사람 코드를 미리 채우고 수정 못 하게 잠근다. */
    private fun applyPrefilledRecipient() {
        val code = arguments?.getString(ARG_RECIPIENT_CODE)?.takeIf { it.isNotBlank() } ?: return
        binding.recipientInput.setText(code)
        binding.recipientInput.isEnabled = false
    }

    private fun selectOrnament(view: ImageView, ornamentType: String) {
        selectedOrnamentView?.isSelected = false
        view.isSelected = true
        selectedOrnamentView = view
        selectedOrnamentType = ornamentType
        binding.ornamentErrorText.isVisible = false
    }

    private fun submitLetter() {
        val content = binding.letterInput.text?.toString().orEmpty()
        val friendCode = binding.recipientInput.text?.toString().orEmpty().trim()
        val ornamentType = selectedOrnamentType

        binding.letterInputLayout.error = null
        binding.recipientInputLayout.error = null

        var hasError = false
        if (content.isBlank()) {
            binding.letterInputLayout.error = getString(R.string.letter_write_error_blank)
            hasError = true
        }
        if (friendCode.isBlank()) {
            binding.recipientInputLayout.error = getString(R.string.letter_write_error_recipient_blank)
            hasError = true
        } else if (friendCode.length != friendCodeLength) {
            binding.recipientInputLayout.error = getString(R.string.letter_write_error_recipient_invalid)
            hasError = true
        }
        if (ornamentType == null) {
            binding.ornamentErrorText.isVisible = true
            hasError = true
        }
        if (hasError) return
        val confirmedOrnamentType = checkNotNull(ornamentType)

        binding.saveButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                app.repository.sendLetter(friendCode, content, confirmedOrnamentType)
                Toast.makeText(requireContext(), R.string.letter_write_success, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (e: NoSuchElementException) {
                binding.recipientInputLayout.error = getString(R.string.letter_write_error_recipient_not_found)
            } catch (e: IllegalArgumentException) {
                // 친구코드 길이·본문 길이·장식 종류는 위에서 이미 검증했으므로,
                // 여기 남는 IllegalArgumentException은 자기 자신에게 보내는 경우뿐이다.
                binding.recipientInputLayout.error = getString(R.string.letter_write_error_self)
            } catch (e: Exception) {
                binding.letterInputLayout.error = getString(R.string.letter_write_error_generic)
            } finally {
                binding.saveButton.isEnabled = !binding.letterInput.text.isNullOrBlank()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_RECIPIENT_CODE = "recipient_code"
    }
}
