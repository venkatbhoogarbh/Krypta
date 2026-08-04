package com.venkatbhoogarbh.krypta.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.venkatbhoogarbh.krypta.R
import com.venkatbhoogarbh.krypta.data.model.PasswordEntry
import java.text.SimpleDateFormat
import java.util.*

class PasswordAdapter(
    private val onItemClick: (PasswordEntry) -> Unit,
    private val onEditClick: (PasswordEntry) -> Unit,
    private val onDeleteClick: (PasswordEntry) -> Unit,
    private val onCopyPasswordClick: (PasswordEntry) -> Unit,
    private val onCopyUsernameClick: (PasswordEntry) -> Unit
) : ListAdapter<PasswordEntry, PasswordAdapter.PasswordViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_password, parent, false)
        return PasswordViewHolder(view)
    }

    override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PasswordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val accountNameText: TextView = itemView.findViewById(R.id.account_name_text)
        private val passwordMaskedText: TextView = itemView.findViewById(R.id.password_masked_text)
        private val timestampText: TextView = itemView.findViewById(R.id.timestamp_text)
        private val optionsButton: ImageButton = itemView.findViewById(R.id.options_button)

        fun bind(entry: PasswordEntry) {
            val maskedUsername = maskUsername(entry.username)
            accountNameText.text = "${entry.accountName} ($maskedUsername)"
            passwordMaskedText.text = "••••••••"
            
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            timestampText.text = "Added ${sdf.format(Date(entry.createdAt))}"

            itemView.setOnClickListener { onItemClick(entry) }

            optionsButton.setOnClickListener { view ->
                showPopupMenu(view, entry)
            }
        }

        private fun maskUsername(username: String): String {
            return if (username.length > 6) {
                val visible = username.take(6)
                val atIndex = username.indexOf('@')
                if (atIndex != -1) {
                    "$visible****${username.substring(atIndex)}"
                } else {
                    "$visible****"
                }
            } else {
                username
            }
        }

        private fun showPopupMenu(view: View, entry: PasswordEntry) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.password_item_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_copy_username -> {
                        onCopyUsernameClick(entry)
                        true
                    }
                    R.id.action_copy_password -> {
                        onCopyPasswordClick(entry)
                        true
                    }
                    R.id.action_edit -> {
                        onEditClick(entry)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(entry)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PasswordEntry>() {
        override fun areItemsTheSame(oldItem: PasswordEntry, newItem: PasswordEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PasswordEntry, newItem: PasswordEntry): Boolean {
            return oldItem == newItem
        }
    }
}
