package com.venkatbhoogarbh.krypta.ui.fragments

import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.venkatbhoogarbh.krypta.R
import com.venkatbhoogarbh.krypta.data.model.PasswordEntry
import com.venkatbhoogarbh.krypta.ui.viewmodel.PasswordViewModel
import com.venkatbhoogarbh.krypta.util.PasswordUtils
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AddPasswordFragment : Fragment() {

    private lateinit var viewModel: PasswordViewModel
    private var editingEntryId: Long = -1L
    private var debounceJob: Job? = null
    private var maskJob: Job? = null
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]

        editingEntryId = arguments?.getLong("entryId", -1L) ?: -1L

        val titleInput = view.findViewById<TextInputEditText>(R.id.add_title)
        val usernameInput = view.findViewById<TextInputEditText>(R.id.add_username)
        val passwordInput = view.findViewById<TextInputEditText>(R.id.add_password)
        val passwordLayout = (passwordInput.parent.parent as? TextInputLayout)
        val generateButton = view.findViewById<Button>(R.id.generate_password_button)
        val strengthIndicator = view.findViewById<LinearProgressIndicator>(R.id.strength_indicator)
        val strengthLabel = view.findViewById<TextView>(R.id.strength_label)
        val duplicateWarning = view.findViewById<TextView>(R.id.duplicate_warning)
        val urlInput = view.findViewById<TextInputEditText>(R.id.add_url)
        val notesInput = view.findViewById<TextInputEditText>(R.id.add_notes)
        val saveButton = view.findViewById<Button>(R.id.save_button)

        fun updateStrengthUI(password: String) {
            if (password.isEmpty()) {
                strengthIndicator.visibility = View.GONE
                strengthLabel.visibility = View.GONE
                return
            }
            
            val strength = PasswordUtils.evaluateStrength(password)
            strengthIndicator.visibility = View.VISIBLE
            strengthLabel.visibility = View.VISIBLE
            
            strengthIndicator.progress = (strength.score * 25)
            strengthIndicator.setIndicatorColor(ContextCompat.getColor(requireContext(), strength.colorResId))
            strengthLabel.text = getString(R.string.password_strength_label, getString(strength.labelResId))
            strengthLabel.setTextColor(ContextCompat.getColor(requireContext(), strength.colorResId))
        }

        fun updatePasswordVisibility(visible: Boolean) {
            isPasswordVisible = visible
            maskJob?.cancel()

            if (isPasswordVisible) {
                passwordInput.transformationMethod = HideReturnsTransformationMethod.getInstance()
                maskJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(30000)
                    updatePasswordVisibility(false)
                }
            } else {
                passwordInput.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            // Move cursor to end after transformation change
            passwordInput.setSelection(passwordInput.text?.length ?: 0)
        }

        passwordLayout?.setEndIconOnClickListener {
            updatePasswordVisibility(!isPasswordVisible)
        }

        fun checkDuplicates(password: String) {
            debounceJob?.cancel()
            if (password.isEmpty()) {
                duplicateWarning.visibility = View.GONE
                return
            }

            debounceJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    val duplicates = viewModel.getDuplicateEntries(password, editingEntryId)
                    if (duplicates.isNotEmpty()) {
                        duplicateWarning.visibility = View.VISIBLE
                        if (duplicates.size == 1) {
                            duplicateWarning.text = getString(R.string.duplicate_password_warning)
                        } else {
                            duplicateWarning.text = getString(R.string.duplicate_password_warning_multiple, duplicates.size)
                        }
                    } else {
                        duplicateWarning.visibility = View.GONE
                    }
                }
            }
        }

        passwordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = s?.toString() ?: ""
                updateStrengthUI(pass)
                checkDuplicates(pass)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        generateButton.setOnClickListener {
            showGeneratorDialog(passwordInput)
        }

        if (editingEntryId != -1L) {
            viewModel.getPasswordById(editingEntryId).observe(viewLifecycleOwner) { entry ->
                entry?.let {
                    titleInput.setText(it.accountName)
                    usernameInput.setText(it.username)
                    passwordInput.setText(it.password)
                    urlInput.setText(it.website)
                    notesInput.setText(it.notes)
                    saveButton.text = "Update Entry"
                    updateStrengthUI(it.password)
                }
            }
        }

        saveButton.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val url = urlInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            if (title.isEmpty()) {
                titleInput.error = getString(R.string.error_field_required)
                return@setOnClickListener
            }
            if (username.isEmpty()) {
                usernameInput.error = getString(R.string.error_field_required)
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordInput.error = getString(R.string.error_field_required)
                return@setOnClickListener
            }

            if (editingEntryId == -1L) {
                val newEntry = PasswordEntry(
                    accountName = title,
                    username = username,
                    password = password,
                    website = url,
                    notes = notes
                )
                viewModel.insert(newEntry)
            } else {
                val updatedEntry = PasswordEntry(
                    id = editingEntryId,
                    accountName = title,
                    username = username,
                    password = password,
                    website = url,
                    notes = notes
                )
                viewModel.update(updatedEntry)
            }

            findNavController().popBackStack()
        }
    }

    private fun showGeneratorDialog(passwordInput: TextInputEditText) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_generator, null)
        val previewText = dialogView.findViewById<TextView>(R.id.generator_preview)
        val lengthText = dialogView.findViewById<TextView>(R.id.generator_length_text)
        val lengthSlider = dialogView.findViewById<Slider>(R.id.generator_length_slider)
        val checkUpper = dialogView.findViewById<MaterialCheckBox>(R.id.checkbox_upper)
        val checkLower = dialogView.findViewById<MaterialCheckBox>(R.id.checkbox_lower)
        val checkDigits = dialogView.findViewById<MaterialCheckBox>(R.id.checkbox_digits)
        val checkSymbols = dialogView.findViewById<MaterialCheckBox>(R.id.checkbox_symbols)
        val errorText = dialogView.findViewById<TextView>(R.id.generator_error)
        val btnRegenerate = dialogView.findViewById<Button>(R.id.btn_regenerate)

        fun updatePreview() {
            val length = lengthSlider.value.toInt()
            val upper = checkUpper.isChecked
            val lower = checkLower.isChecked
            val digits = checkDigits.isChecked
            val symbols = checkSymbols.isChecked

            val selectedCategoriesCount = listOf(upper, lower, digits, symbols).count { it }

            when {
                selectedCategoriesCount == 0 -> {
                    previewText.text = ""
                    errorText.text = getString(R.string.generator_error_no_category)
                    errorText.visibility = View.VISIBLE
                }
                length < selectedCategoriesCount -> {
                    previewText.text = ""
                    errorText.text = getString(R.string.generator_error_too_short)
                    errorText.visibility = View.VISIBLE
                }
                else -> {
                    val password = PasswordUtils.generatePassword(length, upper, lower, digits, symbols)
                    previewText.text = password
                    errorText.visibility = View.GONE
                }
            }
        }

        lengthSlider.addOnChangeListener { _, value, _ ->
            lengthText.text = getString(R.string.generator_length_label, value.toInt())
        }
        
        // Initial setup
        lengthText.text = getString(R.string.generator_length_label, lengthSlider.value.toInt())
        updatePreview()

        btnRegenerate.setOnClickListener { updatePreview() }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.generator_title)
            .setView(dialogView)
            .setNegativeButton(R.string.generator_btn_cancel, null)
            .setPositiveButton(R.string.generator_btn_insert) { _, _ ->
                val generated = previewText.text.toString()
                if (generated.isNotEmpty()) {
                    passwordInput.setText(generated)
                }
            }
            .show()
    }
}
