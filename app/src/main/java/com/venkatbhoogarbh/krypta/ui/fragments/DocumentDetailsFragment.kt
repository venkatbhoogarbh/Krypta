package com.venkatbhoogarbh.krypta.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.venkatbhoogarbh.krypta.R
import com.venkatbhoogarbh.krypta.data.model.DocumentEntry
import com.venkatbhoogarbh.krypta.ui.viewmodel.DocumentViewModel
import com.venkatbhoogarbh.krypta.util.ClipboardHelper
import com.venkatbhoogarbh.krypta.util.SecurityUtils
import com.venkatbhoogarbh.krypta.util.SessionManager
import com.venkatbhoogarbh.krypta.util.StorageManager
import com.venkatbhoogarbh.krypta.util.showDocumentDeleteConfirmation
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentDetailsFragment : Fragment() {

    private lateinit var viewModel: DocumentViewModel
    private lateinit var storageManager: StorageManager
    private var documentId: Long = -1L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_document_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[DocumentViewModel::class.java]
        storageManager = StorageManager(requireContext())

        documentId = arguments?.getLong("documentId", -1L) ?: -1L

        val titleText = view.findViewById<TextView>(R.id.document_details_title)
        val typeText = view.findViewById<TextView>(R.id.document_details_type)
        val sizeText = view.findViewById<TextView>(R.id.document_details_size)
        val dateText = view.findViewById<TextView>(R.id.document_details_date)
        val notesText = view.findViewById<TextView>(R.id.document_details_notes)
        val copyNotesButton = view.findViewById<android.widget.ImageButton>(R.id.copy_notes_button)
        val openButton = view.findViewById<Button>(R.id.open_document_button)
        val deleteButton = view.findViewById<Button>(R.id.delete_document_button)

        viewModel.getDocumentById(documentId).observe(viewLifecycleOwner) { entry ->
            entry?.let { doc ->
                titleText.text = doc.displayName
                typeText.text = getSimpleTypeName(doc.mimeType)
                sizeText.text = formatFileSize(doc.fileSize)
                
                val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                dateText.text = sdf.format(Date(doc.dateImported))
                
                notesText.text = doc.notes ?: "No notes provided"
                
                if (!doc.notes.isNullOrBlank()) {
                    copyNotesButton.visibility = View.VISIBLE
                    copyNotesButton.setOnClickListener {
                        ClipboardHelper.copyToClipboard(requireContext(), doc.notes, R.string.notes_copied)
                    }
                } else {
                    copyNotesButton.visibility = View.GONE
                }

                openButton.setOnClickListener {
                    openDocument(doc)
                }

                deleteButton.setOnClickListener {
                    showDocumentDeleteConfirmation {
                        viewModel.deleteDocument(doc)
                    }
                }
            }
        }

        viewModel.deleteStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is DocumentViewModel.DeleteStatus.Success -> {
                    Toast.makeText(context, R.string.document_deleted, Toast.LENGTH_SHORT).show()
                    viewModel.resetDeleteStatus()
                    findNavController().popBackStack()
                }
                is DocumentViewModel.DeleteStatus.Error -> {
                    Toast.makeText(context, "Error: ${status.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetDeleteStatus()
                }
                else -> {}
            }
        }
    }

    private fun openDocument(entry: DocumentEntry) {
        val key = SessionManager.ddk ?: return
        try {
            val encryptedFile = storageManager.getFile(entry.internalIdentifier)
            if (!encryptedFile.exists()) {
                Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
                return
            }

            // Decrypt to a temporary file in the private cache for secure viewing
            val tempFile = File(requireContext().cacheDir, entry.originalFileName)
            
            SecurityUtils.getDecryptStream(FileInputStream(encryptedFile), key).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                tempFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, entry.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Open with")
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(chooser)
                // Note: ideally we'd delete the temp file after the viewer closes, 
                // but Android doesn't guarantee a callback. 
                // A better approach for Phase 6 would be cleaning cache on startup/lock.
            } else {
                Toast.makeText(context, "No app found to open this file", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening document: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun getSimpleTypeName(mimeType: String): String {
        return when {
            mimeType.contains("pdf") -> "PDF Document"
            mimeType.contains("image") -> "Image"
            mimeType.contains("text") -> "Text File"
            else -> "Document"
        }
    }
}
