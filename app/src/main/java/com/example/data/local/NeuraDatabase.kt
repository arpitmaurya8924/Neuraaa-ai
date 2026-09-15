package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        AuthCredentialEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        ProjectEntity::class,
        MemoryEntity::class,
        StudyProgressEntity::class,
        UserSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NeuraDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun authCredentialDao(): AuthCredentialDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun projectDao(): ProjectDao
    abstract fun memoryDao(): MemoryDao
    abstract fun studyProgressDao(): StudyProgressDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: NeuraDatabase? = null

        fun getDatabase(context: Context): NeuraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NeuraDatabase::class.java,
                    "neura_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
