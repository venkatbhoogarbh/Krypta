package com.venkatbhoogarbh.krypta.data.local

import androidx.room.*
import com.venkatbhoogarbh.krypta.data.model.DocumentEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM document_entries ORDER BY dateImported DESC")
    fun getAllDocuments(): Flow<List<DocumentEntry>>

    @Query("SELECT * FROM document_entries WHERE id = :id")
    suspend fun getDocumentById(id: Long): DocumentEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntry): Long

    @Update
    suspend fun update(document: DocumentEntry)

    @Delete
    suspend fun delete(document: DocumentEntry)

    @Query("SELECT * FROM document_entries WHERE LOWER(displayName) LIKE LOWER(:query) OR LOWER(originalFileName) LIKE LOWER(:query) OR LOWER(notes) LIKE LOWER(:query)")
    fun searchDocuments(query: String): Flow<List<DocumentEntry>>

    @Query("SELECT * FROM document_entries WHERE isFavorite = 1 ORDER BY dateImported DESC")
    fun getFavoriteDocuments(): Flow<List<DocumentEntry>>
}
