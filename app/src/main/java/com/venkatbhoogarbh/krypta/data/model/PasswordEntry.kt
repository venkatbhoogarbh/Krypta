package com.venkatbhoogarbh.krypta.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "password_entries")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountName: String,
    val username: String,
    val password: String,
    val website: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
