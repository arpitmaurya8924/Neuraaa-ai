package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "User",
    val email: String = "",
    val avatarUrl: String = "",
    val authProvider: String = "email", // "google" or "email"
    val isAuthenticated: Boolean = true,
    val sessionToken: String = UUID.randomUUID().toString(),
    val isFirstLogin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "auth_credentials")
data class AuthCredentialEntity(
    @PrimaryKey val userId: String,
    val email: String,
    val passwordHash: String,
    val salt: String,
    val isEmailVerified: Boolean = false,
    val verificationCode: String? = null,
    val authProvider: String = "email",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val mode: String = "general", // general, study, coding, vision, research
    val projectId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val imageBase64: String? = null,
    val imageUri: String? = null,
    val codeSnippet: String? = null,
    val language: String? = null,
    val citations: String? = null, // JSON string or comma-separated links for research mode
    val createdAt: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val category: String = "General",
    val instructions: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val keyCategory: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_progress")
data class StudyProgressEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subject: String,
    val topic: String,
    val score: Int,
    val totalQuestions: Int,
    val activityType: String, // "quiz", "teach_me", "solve"
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: String = "current_settings",
    val language: String = "Auto (English / Hindi / Hinglish)", // "English", "Hindi", "Hinglish", "Auto"
    val personality: String = "Friendly 😄", // "Friendly 😄", "Teacher 🧑🏫", "Professional 💼", "Coder 💻", "Creative ✨"
    val responseLength: String = "Balanced", // "Concise", "Balanced", "Detailed"
    val themeMode: String = "Dark", // "Dark", "Light", "System"
    val memoryEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val studyRemindersEnabled: Boolean = true
)
