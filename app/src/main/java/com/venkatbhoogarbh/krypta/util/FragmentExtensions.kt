package com.venkatbhoogarbh.krypta.util

import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.venkatbhoogarbh.krypta.R

/**
 * Shows a standard delete confirmation dialog.
 *
 * @param onConfirm Callback executed when the user confirms deletion.
 */
fun Fragment.showDeleteConfirmation(onConfirm: () -> Unit) {
    showDeleteConfirmationDialog(
        title = "Delete Password?",
        message = "Are you sure you want to delete this password? This action cannot be undone.",
        onConfirm = onConfirm
    )
}

fun Fragment.showDocumentDeleteConfirmation(onConfirm: () -> Unit) {
    showDeleteConfirmationDialog(
        title = getString(R.string.delete_document_title),
        message = getString(R.string.delete_document_message),
        onConfirm = onConfirm
    )
}

private fun Fragment.showDeleteConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(title)
        .setMessage(message)
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        .setPositiveButton("Delete") { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        .show()
}
