package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.MemoryEntity
import com.example.data.local.UserSettingsEntity
import com.example.ui.theme.NeuraCyan
import com.example.ui.theme.NeuraPurple

import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Info
import com.example.data.backend.GeminiModelConfig
import com.example.data.config.NeuraConfig
import com.example.ui.components.AppUpdateDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettingsEntity,
    memories: List<MemoryEntity>,
    onUpdateSettings: (UserSettingsEntity) -> Unit,
    onAddMemory: (category: String, content: String) -> Unit,
    onDeleteMemory: (id: String) -> Unit,
    onClearMemories: () -> Unit,
    onClearAllData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var memoryCategoryInput by remember { mutableStateOf("User Preference") }
    var memoryContentInput by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var autoCheckUpdates by remember { mutableStateOf(true) }
    var showBackendSecretDialog by remember { mutableStateOf(false) }
    var activeGeminiModel by remember { mutableStateOf(GeminiModelConfig.primaryModel) }

    val personalities = listOf("Friendly 😄", "Teacher 🧑🏫", "Professional 💼", "Coder 💻", "Creative ✨")
    val languages = listOf("Auto (English / Hindi / Hinglish)", "English", "Hindi", "Hinglish")
    val responseLengths = listOf("Concise", "Balanced", "Detailed")
    val themes = listOf("Dark", "Light", "System")

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Personality
            item {
                Text("AI Personality Mode", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    personalities.forEach { p ->
                        FilterChip(
                            selected = settings.personality == p,
                            onClick = { onUpdateSettings(settings.copy(personality = p)) },
                            label = { Text(p) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Language
            item {
                Text("Primary Language Support", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    languages.forEach { lang ->
                        FilterChip(
                            selected = settings.language == lang,
                            onClick = { onUpdateSettings(settings.copy(language = lang)) },
                            label = { Text(lang) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Response Length
            item {
                Text("Response Length Style", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    responseLengths.forEach { len ->
                        FilterChip(
                            selected = settings.responseLength == len,
                            onClick = { onUpdateSettings(settings.copy(responseLength = len)) },
                            label = { Text(len) }
                        )
                    }
                }
            }

            // Appearance Theme
            item {
                Text("Theme & Appearance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEach { th ->
                        FilterChip(
                            selected = settings.themeMode == th,
                            onClick = { onUpdateSettings(settings.copy(themeMode = th)) },
                            label = { Text(th) }
                        )
                    }
                }
            }

            // NEURA Memory Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("NEURA Memory", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = "Allows NEURA to remember your goals, preferences, and style across chats",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.memoryEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(memoryEnabled = it)) }
                    )
                }
            }

            if (settings.memoryEnabled) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saved Memories (${memories.size}):", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
                        Row {
                            TextButton(onClick = { showAddMemoryDialog = true }) {
                                Text("+ Add Memory", color = NeuraCyan)
                            }
                            if (memories.isNotEmpty()) {
                                TextButton(onClick = onClearMemories) {
                                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                items(memories, key = { it.id }) { mem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = mem.keyCategory, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NeuraCyan)
                                Text(text = mem.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { onDeleteMemory(mem.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete memory", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // AI Backend & Gemini Model Configuration
            item {
                Text(
                    text = "AI Backend & Gemini Model",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeuraCyan.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NeuraCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = NeuraCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Secure Gemini Service Layer",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Zero Client Secret Exposure • Server-Side Proxy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuraCyan
                                )
                            }
                        }

                        Text(
                            text = "Active Gemini Model (Modular Switching):",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GeminiModelConfig.AVAILABLE_MODELS.forEach { modelDesc ->
                                FilterChip(
                                    selected = activeGeminiModel == modelDesc.id,
                                    onClick = {
                                        activeGeminiModel = modelDesc.id
                                        GeminiModelConfig.primaryModel = modelDesc.id
                                        Toast.makeText(context, "Switched to ${modelDesc.displayName}", Toast.LENGTH_SHORT).show()
                                    },
                                    label = {
                                        Text(
                                            text = when (modelDesc.id) {
                                                GeminiModelConfig.MODEL_GEMINI_3_5_FLASH -> "3.5 Flash"
                                                GeminiModelConfig.MODEL_GEMINI_3_1_PRO -> "3.1 Pro"
                                                GeminiModelConfig.MODEL_GEMINI_3_6_FLASH -> "3.6 Flash"
                                                else -> modelDesc.displayName
                                            },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                )
                            }
                        }

                        Text(
                            text = when (activeGeminiModel) {
                                GeminiModelConfig.MODEL_GEMINI_3_1_PRO -> "Recommended for complex coding, STEM reasoning, and architecture."
                                GeminiModelConfig.MODEL_GEMINI_3_6_FLASH -> "High throughput failover backup model."
                                else -> "High speed, multimodal, balanced reasoning and conversational fluency."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showBackendSecretDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeuraCyan.copy(alpha = 0.2f),
                                contentColor = NeuraCyan
                            )
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backend Security & Secret Setup Guide", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Application & Updates
            item {
                Text(
                    text = "Application & Updates",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeuraCyan.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(NeuraPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = NeuraCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${NeuraConfig.APP_NAME} v${NeuraConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Release: ${NeuraConfig.RELEASE_DATE} • Up to date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeuraCyan
                                )
                            }
                        }

                        Button(
                            onClick = { showUpdateDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("check_update_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeuraPurple,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check for Updates", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Check for Updates",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Notify when new AI capabilities or models release",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoCheckUpdates,
                                onCheckedChange = { autoCheckUpdates = it }
                            )
                        }
                    }
                }
            }

            // Privacy & Data Reset
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All Chats & Reset Data", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showUpdateDialog) {
        AppUpdateDialog(
            onDismissRequest = { showUpdateDialog = false }
        )
    }

    if (showBackendSecretDialog) {
        AlertDialog(
            onDismissRequest = { showBackendSecretDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NeuraCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backend & Secret Setup")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🔒 Security Architecture:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "The Gemini API key is never hardcoded or sent to the client APK. It is retrieved securely as a server-side secret named GEMINI_API_KEY.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🛠️ How to configure in AI Studio:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "1. Open the Secrets panel in AI Studio.\n2. Add the secret: GEMINI_API_KEY with your Google Gemini API key.\n3. The backend proxy automatically authenticates with Google Generative AI.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showBackendSecretDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }

    if (showAddMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemoryDialog = false },
            title = { Text("Add NEURA Memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = memoryCategoryInput,
                        onValueChange = { memoryCategoryInput = it },
                        label = { Text("Category") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = memoryContentInput,
                        onValueChange = { memoryContentInput = it },
                        label = { Text("Memory Details") },
                        placeholder = { Text("e.g. Always explain mathematical proofs step-by-step") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (memoryContentInput.isNotBlank()) {
                        onAddMemory(memoryCategoryInput, memoryContentInput)
                        memoryContentInput = ""
                        showAddMemoryDialog = false
                    }
                }) {
                    Text("Save Memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Data?") },
            text = { Text("This will permanently delete all conversations, memories, and progress on this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "All data reset successfully", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
