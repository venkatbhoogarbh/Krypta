package com.venkatbhoogarbh.krypta.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.venkatbhoogarbh.krypta.data.local.AppDatabase
import com.venkatbhoogarbh.krypta.data.model.PasswordEntry
import com.venkatbhoogarbh.krypta.data.repository.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class PasswordViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PasswordRepository
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: LiveData<String> = _searchQuery.asLiveData()
    
    val allPasswords: LiveData<List<PasswordEntry>>

    init {
        val passwordDao = AppDatabase.getDatabase(application).passwordDao()
        repository = PasswordRepository(passwordDao)
        
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        allPasswords = _searchQuery.flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.allPasswords
            } else {
                repository.searchPasswords(query)
            }
        }.asLiveData()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getPasswordById(id: Long): LiveData<PasswordEntry?> {
        val result = MutableLiveData<PasswordEntry?>()
        viewModelScope.launch {
            result.value = repository.getPasswordById(id)
        }
        return result
    }

    suspend fun getDuplicateEntries(password: String, excludeId: Long = -1L): List<PasswordEntry> {
        return repository.getEntriesByPassword(password).filter { it.id != excludeId }
    }

    fun insert(entry: PasswordEntry) = viewModelScope.launch {
        repository.insert(entry)
    }

    fun update(entry: PasswordEntry) = viewModelScope.launch {
        repository.update(entry)
    }

    fun delete(entry: PasswordEntry) = viewModelScope.launch {
        repository.delete(entry)
    }
}
