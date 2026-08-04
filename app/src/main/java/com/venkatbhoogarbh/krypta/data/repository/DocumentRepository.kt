package com.venkatbhoogarbh.krypta.data.repository

import com.venkatbhoogarbh.krypta.data.local.DocumentDao
import com.venkatbhoogarbh.krypta.data.model.DocumentEntry
import com.venkatbhoogarbh.krypta.util.SecurityUtils
import com.venkatbhoogarbh.krypta.util.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DocumentRepository(private val documentDao: DocumentDao) {
    val allDocuments: Flow<List<DocumentEntry>> = documentDao.getAllDocuments().map { list ->
        list.map { decryptEntry(it) }
    }
    val favoriteDocuments: Flow<List<DocumentEntry>> = documentDao.getFavoriteDocuments().map { list ->
        list.map { decryptEntry(it) }
    }

    private fun encryptEntry(entry: DocumentEntry): DocumentEntry {
        val key = SessionManager.vaultKey ?: return entry
        return entry.copy(
            notes = entry.notes?.let { SecurityUtils.encrypt(it, key) }
        )
    }

    private fun decryptEntry(entry: DocumentEntry): DocumentEntry {
        val key = SessionManager.vaultKey ?: return entry
        return try {
            entry.copy(
                notes = entry.notes?.let { SecurityUtils.decrypt(it, key) }
            )
        } catch (e: Exception) {
            entry.copy(notes = "[Decryption Error]")
        }
    }

    suspend fun getDocumentById(id: Long): DocumentEntry? {
        return documentDao.getDocumentById(id)?.let { decryptEntry(it) }
    }

    suspend fun insert(document: DocumentEntry): Long {
        return documentDao.insert(encryptEntry(document))
    }

    suspend fun update(document: DocumentEntry) {
        documentDao.update(encryptEntry(document))
    }

    suspend fun delete(document: DocumentEntry) {
        documentDao.delete(document)
    }

    fun searchDocuments(query: String): Flow<List<DocumentEntry>> {
        return documentDao.searchDocuments("%$query%").map { list ->
            list.map { decryptEntry(it) }
        }
    }
}
