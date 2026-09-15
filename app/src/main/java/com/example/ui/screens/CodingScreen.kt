package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeuraBrandHeader
import com.example.ui.theme.NeuraCyan
import com.example.ui.theme.NeuraPurple

@Composable
fun CodingScreen(
    onExecuteCodeAction: (prompt: String, mode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = listOf("Python", "Kotlin", "Java", "JavaScript", "TypeScript", "C++", "C", "SQL", "HTML/CSS")
    val actions = listOf(
        "Generate Code",
        "Debug & Fix",
        "Explain Code",
        "Optimize Performance",
        "Refactor Cleanly",
        "Generate Unit Tests",
        "Convert Language",
        "Compiler Error Fix"
    )

    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    var selectedAction by remember { mutableStateOf("Generate Code") }
    var targetLanguage by remember { mutableStateOf("Python") }
    var userCodeOrRequirement by remember { mutableStateOf("") }
    var compilerErrorInput by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("coding_screen")
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Brand Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuraBrandHeader(subtitle = "Coding & Architecture")
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeuraPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = NeuraCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Multi-Language", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NeuraCyan)
                }
            }
        }

        // Action Mode Chips
        item {
            Text(
                text = "Coding Operation",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actions.forEach { action ->
                    FilterChip(
                        selected = selectedAction == action,
                        onClick = { selectedAction = action },
                        label = { Text(action, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Language Selector
        item {
            Text(
                text = "Primary Language: $selectedLanguage",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { lang ->
                    FilterChip(
                        selected = selectedLanguage == lang,
                        onClick = { selectedLanguage = lang },
                        label = { Text(lang, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // Convert Language Selector if Action is "Convert Language"
        if (selectedAction == "Convert Language") {
            item {
                Text(
                    text = "Target Language to Convert into: $targetLanguage",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = NeuraCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { lang ->
                        FilterChip(
                            selected = targetLanguage == lang,
                            onClick = { targetLanguage = lang },
                            label = { Text(lang, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        // Compiler error input if relevant
        if (selectedAction == "Compiler Error Fix") {
            item {
                OutlinedTextField(
                    value = compilerErrorInput,
                    onValueChange = { compilerErrorInput = it },
                    label = { Text("Paste Compiler / Stacktrace Error") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )
            }
        }

        // Code Editor Input Area
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeuraPurple.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (selectedAction == "Generate Code") "Describe the feature or algorithm to generate:" else "Paste your $selectedLanguage code snippet:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = userCodeOrRequirement,
                        onValueChange = { userCodeOrRequirement = it },
                        placeholder = {
                            Text(
                                text = if (selectedAction == "Generate Code")
                                    "e.g. Implement a thread-safe LRU cache with O(1) get and put operations..."
                                else
                                    "// Paste code snippet here...",
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (userCodeOrRequirement.isNotBlank()) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                val prompt = buildString {
                                    append("[$selectedAction] in $selectedLanguage")
                                    if (selectedAction == "Convert Language") {
                                        append(" converting to $targetLanguage")
                                    }
                                    append(":\n\n$userCodeOrRequirement")
                                    if (compilerErrorInput.isNotBlank()) {
                                        append("\n\nStacktrace / Error Log:\n$compilerErrorInput")
                                    }
                                    append("\n\nPlease provide clean, idiomatic code inside markdown blocks, explain line-by-line logic, and outline time/space complexities.")
                                }
                                onExecuteCodeAction(prompt, "coding")
                            }
                        },
                        enabled = userCodeOrRequirement.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Execute $selectedAction",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Supported Capabilities Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚡ NEURA Coding Engine Capabilities:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NeuraCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Production-ready syntax & null-safety validation\n• Architectural design patterns (Clean, MVVM, Repository)\n• Asymptotic complexity (Big-O analysis)\n• Comprehensive unit test generation (JUnit, PyTest, Jest)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
