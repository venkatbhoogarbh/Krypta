package com.venkatbhoogarbh.krypta.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.venkatbhoogarbh.krypta.data.local.AppDatabase
import com.venkatbhoogarbh.krypta.data.model.DocumentEntry
import com.venkatbhoogarbh.krypta.data.repository.DocumentRepository
import com.venkatbhoogarbh.krypta.util.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: DocumentRepository
    private val storageManager = StorageManager(application)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: LiveData<String> = _searchQuery.asLiveData()

    private val _importStatus = MutableLiveData<ImportStatus>()
    val importStatus: LiveData<ImportStatus> = _importStatus

    private val _deleteStatus = MutableLiveData<DeleteStatus>()
    val deleteStatus: LiveData<DeleteStatus> = _deleteStatus

    val allDocuments: LiveData<List<DocumentEntry>>

    sealed class ImportStatus {
        object Loading : ImportStatus()
        object Success : ImportStatus()
        data class Error(val message: String) : ImportStatus()
        object Idle : ImportStatus()
    }

    sealed class DeleteStatus {
        object Success : DeleteStatus()
        data class Error(val message: String) : DeleteStatus()
        object Idle : DeleteStatus()
    }

    init {
        val documentDao = AppDatabase.getDatabase(application).documentDao()
        repository = DocumentRepository(documentDao)

        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        allDocuments = _searchQuery.flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allDocuments
            } else {
                repository.searchDocuments(query)
            }
        }.asLiveData()
    }

    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            _importStatus.value = ImportStatus.Loading
            
            val result = withContext(Dispatchers.IO) {
                try {
                    val metadata = storageManager.getMetadata(uri) 
                        ?: return@withContext Result.failure<Unit>(Exception("Failed to read metadata"))
                    
                    val internalId = storageManager.saveFile(uri, metadata)
                        ?: return@withContext Result.failure<Unit>(Exception("Failed to save file"))
                    
                    val entry = DocumentEntry(
                        displayName = metadata.originalName,
                        originalFileName = metadata.originalName,
                        mimeType = metadata.mimeType,
                        fileSize = metadata.size,
                        internalIdentifier = internalId
                    )
                    
                    repository.insert(entry)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            if (result.isSuccess) {
                _importStatus.value = ImportStatus.Success
            } else {
                _importStatus.value = ImportStatus.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun resetImportStatus() {
        _importStatus.value = ImportStatus.Idle
    }

    fun deleteDocument(entry: DocumentEntry) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val file = storageManager.getFile(entry.internalIdentifier)
                    if (!file.exists()) {
                        // File missing, but we should still try to clean up the DB record
                        repository.delete(entry)
                        return@withContext Result.success(Unit)
                    }

                    if (storageManager.deleteFile(entry.internalIdentifier)) {
                        try {
                            repository.delete(entry)
                            Result.success(Unit)
                        } catch (e: Exception) {
                            // File deleted but DB failed - Inconsistency!
                            Result.failure<Unit>(Exception("File deleted but database record remain. Please try again."))
                        }
                    } else {
                        Result.failure<Unit>(Exception("Failed to delete the physical file."))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            if (result.isSuccess) {
                _deleteStatus.value = DeleteStatus.Success
            } else {
                _deleteStatus.value = DeleteStatus.Error(result.exceptionOrNull()?.message ?: "Delete failed")
            }
        }
    }

    fun resetDeleteStatus() {
        _deleteStatus.value = DeleteStatus.Idle
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getDocumentById(id: Long): LiveData<DocumentEntry?> {
        val result = MutableLiveData<DocumentEntry?>()
        viewModelScope.launch {
            result.value = repository.getDocumentById(id)
        }
        return result
    }

    fun insert(document: DocumentEntry) = viewModelScope.launch {
        repository.insert(document)
    }

    fun update(document: DocumentEntry) = viewModelScope.launch {
        repository.update(document)
    }

    fun delete(document: DocumentEntry) = viewModelScope.launch {
        repository.delete(document)
    }
}
