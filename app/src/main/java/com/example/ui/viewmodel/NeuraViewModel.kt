package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.MessageEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.StudyProgressEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.repository.NeuraRepository
import com.example.voice.VoiceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

class NeuraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NeuraRepository(application)
    val voiceManager = VoiceManager(application)

    val user: StateFlow<UserEntity?> = repository.user
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UserSettingsEntity> = repository.settings
        .map { it ?: UserSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettingsEntity())

    val memories: StateFlow<List<MemoryEntity>> = repository.memories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<ProjectEntity>> = repository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyProgress: StateFlow<List<StudyProgressEntity>> = repository.studyProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeConversation = MutableStateFlow<ConversationEntity?>(null)
    val activeConversation: StateFlow<ConversationEntity?> = _activeConversation.asStateFlow()

    private val _activeMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeMessages: StateFlow<List<MessageEntity>> = _activeMessages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private var activeGenerationJob: Job? = null
    private var messagesJob: Job? = null

    val authManager = repository.authManager

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _verificationPendingEmail = MutableStateFlow<String?>(null)
    val verificationPendingEmail: StateFlow<String?> = _verificationPendingEmail.asStateFlow()

    private val _verificationDemoCode = MutableStateFlow<String?>(null)
    val verificationDemoCode: StateFlow<String?> = _verificationDemoCode.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
        }
    }

    fun isOnline(): Boolean = repository.isOnline()
    fun hasValidGeminiKey(): Boolean = repository.hasValidGeminiKey()

    fun clearAuthError() {
        _authError.value = null
    }

    fun dismissFirstLoginWelcome() {
        viewModelScope.launch {
            user.value?.let { currentUser ->
                repository.updateUser(currentUser.copy(isFirstLogin = false))
            }
        }
    }

    fun updateProfile(name: String, avatarUrl: String = "") {
        viewModelScope.launch {
            user.value?.let { currentUser ->
                repository.updateUser(currentUser.copy(name = name.trim(), avatarUrl = avatarUrl.trim()))
            }
        }
    }

    fun loginWithEmail(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            when (val res = authManager.loginWithEmail(email, pass)) {
                is com.example.data.auth.AuthResult.Success -> {
                    _isAuthLoading.value = false
                    onSuccess()
                }
                is com.example.data.auth.AuthResult.VerificationRequired -> {
                    _isAuthLoading.value = false
                    _verificationPendingEmail.value = res.email
                    _verificationDemoCode.value = res.demoCode
                    _authError.value = res.message
                }
                is com.example.data.auth.AuthResult.Error -> {
                    _isAuthLoading.value = false
                    _authError.value = res.message
                }
                is com.example.data.auth.AuthResult.Cancelled -> {
                    _isAuthLoading.value = false
                    _authError.value = res.message
                }
            }
        }
    }

    fun registerWithEmail(name: String, email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            when (val res = authManager.registerWithEmail(name, email, pass)) {
                is com.example.data.auth.AuthResult.VerificationRequired -> {
                    _isAuthLoading.value = false
                    _verificationPendingEmail.value = res.email
                    _verificationDemoCode.value = res.demoCode
                }
                is com.example.data.auth.AuthResult.Success -> {
                    _isAuthLoading.value = false
                    onSuccess()
                }
                is com.example.data.auth.AuthResult.Error -> {
                    _isAuthLoading.value = false
                    _authError.value = res.message
                }
                is com.example.data.auth.AuthResult.Cancelled -> {
                    _isAuthLoading.value = false
                    _authError.value = res.message
                }
            }
        }
    }

    fun verifyEmailCode(code: String, onSuccess: () -> Unit) {
        val email = _verificationPendingEmail.value ?: return
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            when (val res = authManager.verifyEmail(email, code)) {
                is com.example.data.auth.AuthResult.Success -> {
                    _isAuthLoading.value = false
                    _verificationPendingEmail.value = null
                    _verificationDemoCode.value = null
                    onSuccess()
                }
                is com.example.data.auth.AuthResult.Error -> {
                    _isAuthLoading.value = false
                    _authError.value = res.message
                }
                else -> {
                    _isAuthLoading.value = false
                }
            }
        }
    }

    fun signInWithGoogle(context: android.content.Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            when (val res = authManager.signInWithGoogle(context)) {
                is com.example.data.auth.AuthResult.Success -> {
                    _isAuthLoading.value = false
                    onSuccess()
                }
                is com.example.data.auth.AuthResult.Cancelled -> {
                    _isAuthLoading.value = false
                    _authError.value = res.message
                }
                is com.example.data.auth.AuthResult.Error -> {
                    _isAuthLoading.value = false
                    _authError.value = res.message
                }
                else -> {
                    _isAuthLoading.value = false
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.clearUser()
            _activeConversation.value = null
            _activeMessages.value = emptyList()
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uid = user.value?.id ?: ""
            if (uid.isNotBlank()) {
                repository.deleteUserAccount(uid)
            }
            _activeConversation.value = null
            _activeMessages.value = emptyList()
            onSuccess()
        }
    }

    fun selectConversation(convId: String) {
        if (convId.isBlank()) return
        viewModelScope.launch {
            val conv = conversations.value.find { it.id == convId } 
                ?: repository.getConversation(convId)
                ?: ConversationEntity(id = convId, title = "Chat with NEURA", mode = "general")
            _activeConversation.value = conv
            observeMessages(convId)
        }
    }

    private fun observeMessages(convId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessages(convId).collect { msgs ->
                // Don't overwrite the active in-memory streaming buffer during generation
                if (activeGenerationJob?.isActive != true) {
                    _activeMessages.value = msgs
                }
            }
        }
    }

    fun startNewChat(title: String = "New Chat", mode: String = "general", projectId: String? = null): String {
        val newId = UUID.randomUUID().toString()
        val newConv = ConversationEntity(
            id = newId,
            title = title,
            mode = mode,
            projectId = projectId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _activeConversation.value = newConv
        _activeMessages.value = emptyList()
        viewModelScope.launch {
            repository.createConversationWithId(newId, title, mode, projectId)
            observeMessages(newId)
        }
        return newId
    }

    fun sendMessage(
        prompt: String,
        imageUri: Uri? = null,
        mode: String = "general",
        fileContent: String? = null,
        targetConvId: String? = null,
        customInstruction: String? = null
    ) {
        // Prevent duplicate API requests while already thinking/processing
        if (_isThinking.value) {
            return
        }

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            var convId = targetConvId ?: _activeConversation.value?.id

            if (convId == null) {
                // Auto-generate smart title from first 35 chars
                val title = prompt.take(35).replace("\n", " ").trim()
                val newId = UUID.randomUUID().toString()
                val newConv = repository.createConversationWithId(
                    id = newId,
                    title = if (title.isNotBlank()) title else "Chat with NEURA",
                    mode = mode
                )
                convId = newId
                _activeConversation.value = newConv
                observeMessages(convId)
            }

            // Image Base64 preparation
            val imageBase64 = imageUri?.let { encodeImageToBase64(it) }

            // Combine prompt and file content if provided
            val fullPrompt = if (!fileContent.isNullOrBlank()) {
                "$prompt\n\n[Attached Content]:\n$fileContent"
            } else prompt

            // Insert User Message with unique ID
            val userMsgId = UUID.randomUUID().toString()
            val userMsg = MessageEntity(
                id = userMsgId,
                conversationId = convId,
                role = "user",
                content = fullPrompt,
                imageBase64 = imageBase64,
                createdAt = System.currentTimeMillis()
            )
            repository.saveMessage(userMsg)

            // Prepare Assistant placeholder for real-time progressive streaming
            val assistantMsgId = UUID.randomUUID().toString()
            val tempAssistantMsg = MessageEntity(
                id = assistantMsgId,
                conversationId = convId,
                role = "assistant",
                content = "",
                createdAt = System.currentTimeMillis() + 1
            )
            _activeMessages.value = _activeMessages.value + tempAssistantMsg

            _isThinking.value = true
            val responseBuilder = StringBuilder()
            try {
                repository.askNeuraStream(
                    conversationId = convId,
                    userPrompt = fullPrompt,
                    imageBase64 = imageBase64,
                    mode = mode,
                    customSystemInstruction = customInstruction,
                    currentMessageId = userMsgId
                ).collect { chunk ->
                    _isThinking.value = false
                    responseBuilder.append(chunk)
                    val currentText = responseBuilder.toString()
                    _activeMessages.value = _activeMessages.value.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(content = currentText) else msg
                    }
                }

                val finalAssistantMsg = tempAssistantMsg.copy(content = responseBuilder.toString())
                repository.saveMessage(finalAssistantMsg)
            } catch (e: Exception) {
                val errorText = responseBuilder.toString().ifBlank {
                    "⚠️ **AI Service Notice**: Unable to complete request (${e.message ?: "network error"}). Please try again."
                }
                val errorMsg = tempAssistantMsg.copy(content = errorText, isError = true)
                repository.saveMessage(errorMsg)
                _activeMessages.value = _activeMessages.value.map { msg ->
                    if (msg.id == assistantMsgId) errorMsg else msg
                }
            } finally {
                _isThinking.value = false
            }
        }
    }

    fun regenerateLast(prompt: String) {
        _activeConversation.value?.id?.let { convId ->
            sendMessage(prompt = prompt, targetConvId = convId)
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        _isThinking.value = false
    }

    fun renameConversation(id: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameConversation(id, newTitle)
            if (_activeConversation.value?.id == id) {
                _activeConversation.value = _activeConversation.value?.copy(title = newTitle)
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_activeConversation.value?.id == id) {
                _activeConversation.value = null
                _activeMessages.value = emptyList()
            }
        }
    }

    fun updateSettings(newSettings: UserSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(newSettings)
        }
    }

    fun addMemory(category: String, content: String) {
        viewModelScope.launch {
            repository.addMemory(category, content)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            repository.clearMemories()
        }
    }

    fun createProject(name: String, description: String, category: String) {
        viewModelScope.launch {
            repository.createProject(name, description, category)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun recordStudyProgress(subject: String, topic: String, score: Int, total: Int, type: String) {
        viewModelScope.launch {
            repository.recordStudyProgress(subject, topic, score, total, type)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _activeConversation.value = null
            _activeMessages.value = emptyList()
        }
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Scale down if huge to fit within token limit
            val maxDimension = 1024
            val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val targetW = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                val targetH = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            } else bitmap

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
