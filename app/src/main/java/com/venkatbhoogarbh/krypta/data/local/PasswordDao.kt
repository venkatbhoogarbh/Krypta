package com.venkatbhoogarbh.krypta.data.local

import androidx.room.*
import com.venkatbhoogarbh.krypta.data.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM password_entries ORDER BY accountName ASC")
    fun getAllPasswords(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PasswordEntry)

    @Update
    suspend fun update(entry: PasswordEntry)

    @Delete
    suspend fun delete(entry: PasswordEntry)

    @Query("SELECT * FROM password_entries WHERE password = :password")
    suspend fun getEntriesByPassword(password: String): List<PasswordEntry>

    @Query("SELECT * FROM password_entries WHERE LOWER(accountName) LIKE LOWER(:query) OR LOWER(username) LIKE LOWER(:query) OR LOWER(website) LIKE LOWER(:query)")
    fun searchPasswords(query: String): Flow<List<PasswordEntry>>
}
