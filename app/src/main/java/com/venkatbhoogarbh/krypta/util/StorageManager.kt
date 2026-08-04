package com.venkatbhoogarbh.krypta.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Handles all physical file operations for the Document Vault.
 */
class StorageManager(private val context: Context) {

    private val documentsDir: File by lazy {
        File(context.filesDir, "vault_documents").apply {
            if (!exists()) mkdirs()
        }
    }

    data class FileMetadata(
        val originalName: String,
        val size: Long,
        val mimeType: String,
        val extension: String
    )

    /**
     * Extracts metadata from a content Uri.
     */
    fun getMetadata(uri: Uri): FileMetadata? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                val name = cursor.getString(nameIndex) ?: "unnamed_file"
                val size = cursor.getLong(sizeIndex)
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val extension = File(name).extension
                FileMetadata(name, size, mimeType, extension)
            } else null
        }
    }

    /**
     * Copies a file from a Uri to internal storage with encryption.
     * @return The unique internal identifier (filename) if successful, null otherwise.
     */
    fun saveFile(uri: Uri, metadata: FileMetadata): String? {
        val key = SessionManager.vaultKey ?: return null
        val internalId = UUID.randomUUID().toString() + if (metadata.extension.isNotEmpty()) ".${metadata.extension}" else ""
        val targetFile = File(documentsDir, internalId)

        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                val encryptedOutput = SecurityUtils.getEncryptStream(FileOutputStream(targetFile), key)
                encryptedOutput.use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                }
            } ?: throw Exception("Failed to open input stream")
            
            internalId
        } catch (e: Exception) {
            if (targetFile.exists()) targetFile.delete()
            null
        }
    }

    /**
     * Deletes a file from internal storage.
     */
    fun deleteFile(internalId: String): Boolean {
        val file = File(documentsDir, internalId)
        return if (file.exists()) file.delete() else false
    }

    /**
     * Resolves the absolute File object for an internal ID.
     */
    fun getFile(internalId: String): File {
        return File(documentsDir, internalId)
    }
}
