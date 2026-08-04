package com.venkatbhoogarbh.krypta.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.venkatbhoogarbh.krypta.data.model.DocumentEntry
import com.venkatbhoogarbh.krypta.data.model.PasswordEntry

@Database(entities = [PasswordEntry::class, DocumentEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "krypta_database",
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
