package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.BuildConfig
import com.example.data.backend.GeminiModelConfig
import com.example.data.backend.NeuraBackendService
import com.example.data.config.NeuraConfig
import com.example.data.engine.NeuraIntelligenceEngine
import com.example.data.local.ConversationEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.MessageEntity
import com.example.data.local.NeuraDatabase
import com.example.data.local.ProjectEntity
import com.example.data.local.StudyProgressEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

class NeuraRepository(private val context: Context) {
    private val db = NeuraDatabase.getDatabase(context)
    private val userDao = db.userDao()
    private val authCredentialDao = db.authCredentialDao()
    private val convDao = db.conversationDao()
    private val msgDao = db.messageDao()
    private val projectDao = db.projectDao()
    private val memoryDao = db.memoryDao()
    private val studyDao = db.studyProgressDao()
    private val settingsDao = db.userSettingsDao()

    val authManager = com.example.data.auth.NeuraAuthManager(userDao, authCredentialDao)
    val backendService = NeuraBackendService(context)

    val conversations: Flow<List<ConversationEntity>> = convDao.getAllConversations()
    val user: Flow<UserEntity?> = userDao.getUser()
    val settings: Flow<UserSettingsEntity?> = settingsDao.getSettings()
    val memories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()
    val projects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val studyProgress: Flow<List<StudyProgressEntity>> = studyDao.getAllProgress()

    fun getMessages(conversationId: String): Flow<List<MessageEntity>> =
        msgDao.getMessagesForConversation(conversationId)

    fun searchConversations(query: String): Flow<List<ConversationEntity>> =
        convDao.searchConversations(query)

    fun isOnline(): Boolean = backendService.isNetworkAvailable()

    fun hasValidGeminiKey(): Boolean = backendService.isBackendConfigured()

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        // We do not inject a placeholder user; authentications are strictly driven by real user login / signup.
        val currentSettings = settingsDao.getSettingsSync()
        if (currentSettings == null) {
            settingsDao.saveSettings(
                UserSettingsEntity(
                    id = "current_settings",
                    language = "Auto (English / Hindi / Hinglish)",
                    personality = "Friendly 😄",
                    responseLength = "Balanced",
                    themeMode = "Dark",
                    memoryEnabled = true,
                    notificationsEnabled = true
                )
            )
        }

        val projectList = projectDao.getAllProjects().firstOrNull()
        if (projectList.isNullOrEmpty()) {
            projectDao.insertProject(
                ProjectEntity(
                    name = "Class 12 Study",
                    description = "Physics, Chemistry & Math revision notes and practice problems",
                    category = "Education"
                )
            )
            projectDao.insertProject(
                ProjectEntity(
                    name = "Python & Android Project",
                    description = "Code snippets, architecture notes, and algorithmic scripts",
                    category = "Development"
                )
            )
        }

        val memList = memoryDao.getAllMemories().firstOrNull()
        if (memList.isNullOrEmpty()) {
            memoryDao.insertMemory(
                MemoryEntity(
                    keyCategory = "Language Preference",
                    content = "Comfortable with English, Hindi, and Hinglish naturally."
                )
            )
            memoryDao.insertMemory(
                MemoryEntity(
                    keyCategory = "Learning Style",
                    content = "Prefers direct answers for simple questions and step-by-step reasoning for complex ones."
                )
            )
        }

        val convList = convDao.getAllConversations().firstOrNull()
        if (convList.isNullOrEmpty()) {
            val welcomeId = UUID.randomUUID().toString()
            convDao.insertConversation(
                ConversationEntity(
                    id = welcomeId,
                    title = "Welcome to NEURA",
                    mode = "general"
                )
            )
            msgDao.insertMessage(
                MessageEntity(
                    conversationId = welcomeId,
                    role = "assistant",
                    content = "Namaste! 👋 I am **${NeuraConfig.APP_NAME} — ${NeuraConfig.TAGLINE}**.\n\nI am fast when simple and thoughtful when complex. Ask me anything in English, Hindi, or Hinglish!"
                )
            )
        }
    }

    suspend fun createConversation(title: String, mode: String = "general", projectId: String? = null): String =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            convDao.insertConversation(
                ConversationEntity(
                    id = id,
                    title = title,
                    mode = mode,
                    projectId = projectId
                )
            )
            id
        }

    suspend fun createConversationWithId(id: String, title: String, mode: String = "general", projectId: String? = null): ConversationEntity =
        withContext(Dispatchers.IO) {
            val conv = ConversationEntity(
                id = id,
                title = title,
                mode = mode,
                projectId = projectId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            convDao.insertConversation(conv)
            conv
        }

    suspend fun getConversation(id: String): ConversationEntity? = withContext(Dispatchers.IO) {
        convDao.getConversationById(id)
    }

    suspend fun renameConversation(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        convDao.renameConversation(id, newTitle)
    }

    suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
        msgDao.deleteMessagesForConversation(id)
        convDao.deleteConversation(id)
    }

    suspend fun saveMessage(message: MessageEntity) = withContext(Dispatchers.IO) {
        msgDao.insertMessage(message)
        convDao.getConversationById(message.conversationId)?.let { conv ->
            convDao.updateConversation(conv.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
        msgDao.deleteMessage(id)
    }

    suspend fun updateSettings(settings: UserSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.saveSettings(settings)
    }

    suspend fun addMemory(category: String, content: String) = withContext(Dispatchers.IO) {
        memoryDao.insertMemory(MemoryEntity(keyCategory = category, content = content))
    }

    suspend fun deleteMemory(id: String) = withContext(Dispatchers.IO) {
        memoryDao.deleteMemory(id)
    }

    suspend fun clearMemories() = withContext(Dispatchers.IO) {
        memoryDao.clearMemories()
    }

    suspend fun createProject(name: String, description: String, category: String, instructions: String = ""): String =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            projectDao.insertProject(
                ProjectEntity(
                    id = id,
                    name = name,
                    description = description,
                    category = category,
                    instructions = instructions
                )
            )
            id
        }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        projectDao.deleteProject(id)
    }

    suspend fun recordStudyProgress(
        subject: String,
        topic: String,
        score: Int,
        totalQuestions: Int,
        activityType: String
    ) = withContext(Dispatchers.IO) {
        studyDao.insertProgress(
            StudyProgressEntity(
                subject = subject,
                topic = topic,
                score = score,
                totalQuestions = totalQuestions,
                activityType = activityType
            )
        )
    }

    suspend fun saveUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.saveUser(user)
    }

    suspend fun updateUser(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun clearUser() = withContext(Dispatchers.IO) {
        authManager.signOut(context)
    }

    suspend fun deleteUserAccount(userId: String) = withContext(Dispatchers.IO) {
        authManager.deleteAccount(userId, context)
        convDao.clearAll()
        memoryDao.clearMemories()
        projectDao.clearAll()
        studyDao.clearProgress()
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        convDao.clearAll()
        memoryDao.clearMemories()
        studyDao.clearProgress()
        initializeDefaultsIfNeeded()
    }

    /**
     * High-Speed Gemini API Streaming Pipeline via NeuraBackendService:
     * 1. Preserves multi-turn conversation context and memory.
     * 2. Enriches context if user provides follow-up prompt ('ye samjha', 'iska code').
     * 3. Calls the secure backend service layer with streaming SSE and automatic failover.
     * 4. API keys are managed on the server side and never sent to the client.
     */
    fun askNeuraStream(
        conversationId: String,
        userPrompt: String,
        imageBase64: String? = null,
        mode: String = "general",
        customSystemInstruction: String? = null,
        currentMessageId: String? = null
    ): Flow<String> = flow {
        // Retrieve past messages for conversation, excluding the current in-flight message to prevent duplicates
        val rawMessages = msgDao.getMessagesForConversationSync(conversationId)
        val historyMessages = rawMessages.filter { msg ->
            (currentMessageId == null || msg.id != currentMessageId) &&
            msg.content.isNotBlank() &&
            !msg.isError
        }

        val historyList = historyMessages.map { "${it.role}: ${it.content}" }

        // Context follow-up enrichment if appropriate
        val fastResult = NeuraIntelligenceEngine.evaluateFastIntent(userPrompt, historyList)
        val effectivePrompt = if (fastResult is NeuraIntelligenceEngine.FastIntentResult.ContextEnriched) {
            fastResult.resolvedPrompt
        } else {
            userPrompt
        }

        val effectiveCustomInstruction = if (fastResult is NeuraIntelligenceEngine.FastIntentResult.ContextEnriched && fastResult.systemModeHint != null) {
            (customSystemInstruction ?: "") + "\n" + fastResult.systemModeHint
        } else {
            customSystemInstruction
        }

        val settings = settingsDao.getSettingsSync() ?: UserSettingsEntity()
        val memories = if (settings.memoryEnabled) memoryDao.getAllMemories().firstOrNull().orEmpty() else emptyList()
        val memoryContext = if (memories.isNotEmpty()) {
            memories.joinToString("\n") { "- ${it.keyCategory}: ${it.content}" }
        } else ""

        val systemPrompt = NeuraIntelligenceEngine.buildMasterSystemPrompt(
            personality = settings.personality,
            languagePreference = settings.language,
            responseLength = settings.responseLength,
            memoryContext = memoryContext,
            customModeInstruction = effectiveCustomInstruction
        )

        val historyPairs = historyMessages.map { Pair(it.role, it.content) }
        val maxTokens = when (settings.responseLength) {
            "Concise" -> 800
            "Detailed" -> 3500
            else -> 2000
        }

        // Delegate to secure backend service layer
        backendService.streamChatResponse(
            conversationHistory = historyPairs,
            userPrompt = effectivePrompt,
            imageBase64 = imageBase64,
            systemInstruction = systemPrompt,
            mode = mode,
            maxTokens = maxTokens
        ).collect { chunk ->
            emit(chunk)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Non-streaming convenience wrapper.
     */
    suspend fun askNeura(
        conversationId: String,
        userPrompt: String,
        imageBase64: String? = null,
        mode: String = "general",
        customSystemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        askNeuraStream(conversationId, userPrompt, imageBase64, mode, customSystemInstruction).collect { chunk ->
            sb.append(chunk)
        }
        sb.toString()
    }
}
