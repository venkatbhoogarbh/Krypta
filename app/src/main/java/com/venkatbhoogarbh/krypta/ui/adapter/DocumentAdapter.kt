package com.venkatbhoogarbh.krypta.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.venkatbhoogarbh.krypta.R
import com.venkatbhoogarbh.krypta.data.model.DocumentEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentAdapter(
    private val onItemClick: (DocumentEntry) -> Unit,
    private val onDeleteClick: (DocumentEntry) -> Unit
) : ListAdapter<DocumentEntry, DocumentAdapter.DocumentViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.document_name_text)
        private val infoText: TextView = itemView.findViewById(R.id.document_info_text)
        private val dateText: TextView = itemView.findViewById(R.id.document_date_text)
        private val iconImage: ImageView = itemView.findViewById(R.id.document_type_icon)
        private val optionsButton: View = itemView.findViewById(R.id.options_button)

        fun bind(entry: DocumentEntry) {
            nameText.text = entry.displayName
            
            val formattedSize = formatFileSize(entry.fileSize)
            val fileType = getSimpleTypeName(entry.mimeType)
            infoText.text = itemView.context.getString(R.string.document_info_format, fileType, formattedSize)
            
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateText.text = itemView.context.getString(R.string.document_imported_at, sdf.format(Date(entry.dateImported)))
            
            iconImage.setImageResource(getDocumentIcon(entry.mimeType))

            itemView.setOnClickListener { onItemClick(entry) }

            optionsButton.setOnClickListener { view ->
                showPopupMenu(view, entry)
            }
        }

        private fun showPopupMenu(view: View, entry: DocumentEntry) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.password_item_menu, popup.menu) // Reusing same menu for now
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_delete -> {
                        onDeleteClick(entry)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun formatFileSize(size: Long): String {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }

        private fun getSimpleTypeName(mimeType: String): String {
            return when {
                mimeType.contains("pdf") -> "PDF"
                mimeType.contains("image") -> "Image"
                mimeType.contains("text") -> "Text"
                mimeType.contains("word") || mimeType.contains("docx") -> "Document"
                mimeType.contains("excel") || mimeType.contains("spreadsheet") -> "Spreadsheet"
                mimeType.contains("powerpoint") || mimeType.contains("presentation") -> "Presentation"
                else -> "File"
            }
        }

        private fun getDocumentIcon(mimeType: String): Int {
            return when {
                mimeType.contains("pdf") -> android.R.drawable.ic_menu_agenda
                mimeType.contains("image") -> android.R.drawable.ic_menu_gallery
                mimeType.contains("text") -> android.R.drawable.ic_menu_edit
                else -> android.R.drawable.ic_menu_save
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DocumentEntry>() {
        override fun areItemsTheSame(oldItem: DocumentEntry, newItem: DocumentEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DocumentEntry, newItem: DocumentEntry): Boolean {
            return oldItem == newItem
        }
    }
}
