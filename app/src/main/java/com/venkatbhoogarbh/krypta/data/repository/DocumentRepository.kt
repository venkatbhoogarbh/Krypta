package com.venkatbhoogarbh.krypta.data.repository

import com.venkatbhoogarbh.krypta.data.local.DocumentDao
import com.venkatbhoogarbh.krypta.data.model.DocumentEntry
import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val documentDao: DocumentDao) {
    val allDocuments: Flow<List<DocumentEntry>> = documentDao.getAllDocuments()
    val favoriteDocuments: Flow<List<DocumentEntry>> = documentDao.getFavoriteDocuments()

    suspend fun getDocumentById(id: Long): DocumentEntry? {
        return documentDao.getDocumentById(id)
    }

    suspend fun insert(document: DocumentEntry): Long {
        return documentDao.insert(document)
    }

    suspend fun update(document: DocumentEntry) {
        documentDao.update(document)
    }

    suspend fun delete(document: DocumentEntry) {
        documentDao.delete(document)
    }

    fun searchDocuments(query: String): Flow<List<DocumentEntry>> {
        return documentDao.searchDocuments("%$query%")
    }
}
