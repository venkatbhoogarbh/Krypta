package com.venkatbhoogarbh.krypta.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.venkatbhoogarbh.krypta.R
import com.venkatbhoogarbh.krypta.ui.viewmodel.PasswordViewModel
import com.venkatbhoogarbh.krypta.util.ClipboardHelper
import com.venkatbhoogarbh.krypta.util.showDeleteConfirmation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PasswordDetailsFragment : Fragment() {

    private lateinit var viewModel: PasswordViewModel
    private var entryId: Long = -1L
    private var isPasswordVisible = false
    private var maskJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_password_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[PasswordViewModel::class.java]

        entryId = arguments?.getLong("entryId", -1L) ?: -1L

        val titleText = view.findViewById<TextView>(R.id.details_title)
        val usernameText = view.findViewById<TextView>(R.id.details_username)
        val passwordText = view.findViewById<TextView>(R.id.details_password)
        val websiteText = view.findViewById<TextView>(R.id.details_website)
        val notesText = view.findViewById<TextView>(R.id.details_notes)
        val peekButton = view.findViewById<ImageButton>(R.id.details_peek_button)
        val copyButton = view.findViewById<ImageButton>(R.id.details_copy_button)
        val editButton = view.findViewById<Button>(R.id.edit_button)
        val deleteButton = view.findViewById<Button>(R.id.delete_button)

        viewModel.getPasswordById(entryId).observe(viewLifecycleOwner) { entry ->
            entry?.let { currentEntry ->
                titleText.text = currentEntry.accountName
                usernameText.text = currentEntry.username
                passwordText.text = if (isPasswordVisible) currentEntry.password else "••••••••"
                websiteText.text = currentEntry.website ?: "No website provided"
                notesText.text = currentEntry.notes ?: "No notes provided"

                copyButton.setOnClickListener {
                    ClipboardHelper.copyToClipboard(requireContext(), currentEntry.password, R.string.password_copied)
                }

                fun updatePasswordVisibility(visible: Boolean) {
                    isPasswordVisible = visible
                    passwordText.text = if (isPasswordVisible) currentEntry.password else "••••••••"
                    peekButton.setImageResource(
                        if (isPasswordVisible) android.R.drawable.ic_menu_close_clear_cancel
                        else android.R.drawable.ic_menu_view
                    )

                    maskJob?.cancel()
                    if (isPasswordVisible) {
                        maskJob = viewLifecycleOwner.lifecycleScope.launch {
                            delay(30000) // 30 seconds
                            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                updatePasswordVisibility(false)
                            }
                        }
                    }
                }
                
                editButton.setOnClickListener {
                    val bundle = Bundle().apply { putLong("entryId", entryId) }
                    findNavController().navigate(R.id.action_details_to_edit, bundle)
                }

                deleteButton.setOnClickListener {
                    showDeleteConfirmation {
                        viewModel.delete(currentEntry)
                        findNavController().popBackStack()
                    }
                }
                
                peekButton.setOnClickListener {
                    updatePasswordVisibility(!isPasswordVisible)
                }
            }
        }
    }
}
