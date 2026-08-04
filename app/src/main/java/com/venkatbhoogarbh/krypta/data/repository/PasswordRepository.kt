package com.venkatbhoogarbh.krypta.data.repository

import com.venkatbhoogarbh.krypta.data.local.PasswordDao
import com.venkatbhoogarbh.krypta.data.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

class PasswordRepository(private val passwordDao: PasswordDao) {
    val allPasswords: Flow<List<PasswordEntry>> = passwordDao.getAllPasswords()

    fun searchPasswords(query: String): Flow<List<PasswordEntry>> {
        return passwordDao.searchPasswords("%$query%")
    }

    suspend fun getPasswordById(id: Long): PasswordEntry? {
        return passwordDao.getPasswordById(id)
    }

    suspend fun getEntriesByPassword(password: String): List<PasswordEntry> {
        return passwordDao.getEntriesByPassword(password)
    }

    suspend fun insert(entry: PasswordEntry) {
        passwordDao.insert(entry)
    }

    suspend fun update(entry: PasswordEntry) {
        passwordDao.update(entry)
    }

    suspend fun delete(entry: PasswordEntry) {
        passwordDao.delete(entry)
    }
}
