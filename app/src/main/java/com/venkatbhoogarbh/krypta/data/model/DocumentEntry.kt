package com.venkatbhoogarbh.krypta.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_entries")
data class DocumentEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String,
    val originalFileName: String,
    val mimeType: String,
    val fileSize: Long,
    val internalIdentifier: String, // Unique name/UUID used for the physical file in private storage
    val dateImported: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val isFavorite: Boolean = false
)
