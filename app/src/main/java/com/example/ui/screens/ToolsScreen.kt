package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.ShortText
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeuraBrandHeader
import com.example.ui.theme.NeuraCyan
import com.example.ui.theme.NeuraPurple

data class ToolDefinition(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val placeholder: String
)

@Composable
fun ToolsScreen(
    onLaunchTool: (toolTitle: String, prompt: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val toolsList = listOf(
        ToolDefinition(
            id = "writer",
            title = "Writer",
            description = "Draft essays, articles, emails, or creative stories",
            icon = Icons.Default.Edit,
            accentColor = NeuraCyan,
            placeholder = "What would you like NEURA to write? (e.g. Formal leave letter, blog post on AI...)"
        ),
        ToolDefinition(
            id = "summarizer",
            title = "Summarizer",
            description = "Extract key takeaways and concise bullet points",
            icon = Icons.Default.ShortText,
            accentColor = NeuraPurple,
            placeholder = "Paste the long text or article you want to summarize..."
        ),
        ToolDefinition(
            id = "translator",
            title = "Translator",
            description = "Fluent English, Hindi, Hinglish, Spanish, French, etc.",
            icon = Icons.Default.Translate,
            accentColor = Color(0xFF38BDF8),
            placeholder = "Enter text to translate and target language..."
        ),
        ToolDefinition(
            id = "explainer",
            title = "Explainer (ELI5)",
            description = "Break down complex concepts into simple analogies",
            icon = Icons.Default.Lightbulb,
            accentColor = Color(0xFFF59E0B),
            placeholder = "What difficult concept do you want explained simply? (e.g. Quantum Entanglement...)"
        ),
        ToolDefinition(
            id = "calculator",
            title = "Calculator & Math",
            description = "Solve mathematical, algebraic, and calculus problems",
            icon = Icons.Default.Calculate,
            accentColor = Color(0xFF10B981),
            placeholder = "Enter mathematical equation or word problem to calculate step-by-step..."
        ),
        ToolDefinition(
            id = "document_ai",
            title = "Document AI",
            description = "Analyze documents, extract insights, and ask questions",
            icon = Icons.Default.Description,
            accentColor = Color(0xFFEC4899),
            placeholder = "Paste document text or question regarding your syllabus..."
        ),
        ToolDefinition(
            id = "quiz_maker",
            title = "Quiz Maker",
            description = "Generate custom quizzes with instant explanations",
            icon = Icons.Default.Quiz,
            accentColor = Color(0xFF8B5CF6),
            placeholder = "Enter subject/topic for a specialized quiz..."
        ),
        ToolDefinition(
            id = "code_assistant",
            title = "Code Assistant",
            description = "Write, debug, and optimize multi-language code",
            icon = Icons.Default.Code,
            accentColor = Color(0xFF06B6D4),
            placeholder = "Describe the programming task or paste code..."
        )
    )

    var activeTool by remember { mutableStateOf<ToolDefinition?>(null) }
    var toolInputText by remember { mutableStateOf("") }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier
            .fillMaxSize()
            .testTag("tools_screen")
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            NeuraBrandHeader(subtitle = "AI Productivity Suite")
        }

        item(span = { GridItemSpan(2) }) {
            Text(
                text = "Specialized AI Tools",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(toolsList, key = { it.id }) { tool ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        activeTool = tool
                        toolInputText = ""
                    }
                    .testTag("tool_card_${tool.id}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, tool.accentColor.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(tool.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.title,
                            tint = tool.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = tool.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tool.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Modal when a tool is tapped
    activeTool?.let { tool ->
        AlertDialog(
            onDismissRequest = { activeTool = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(tool.icon, contentDescription = null, tint = tool.accentColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tool.title, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = tool.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = toolInputText,
                        onValueChange = { toolInputText = it },
                        placeholder = { Text(tool.placeholder, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (toolInputText.isNotBlank()) {
                            val prompt = "[Tool: ${tool.title}]\n\n$toolInputText"
                            onLaunchTool("Tool: ${tool.title}", prompt)
                            activeTool = null
                        }
                    },
                    enabled = toolInputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tool.accentColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Run ${tool.title}")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeTool = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
