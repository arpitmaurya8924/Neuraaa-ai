package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StudyProgressEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MarkdownContent
import com.example.ui.components.NeuraBrandHeader
import com.example.ui.components.ThinkingIndicator
import com.example.ui.theme.NeuraCyan
import com.example.ui.theme.NeuraPurple
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    studyProgressList: List<StudyProgressEntity>,
    onAskStudyQuestion: (prompt: String, mode: String, imageUri: Uri?) -> Unit,
    onRecordProgress: (subject: String, topic: String, score: Int, total: Int, type: String) -> Unit,
    onLaunchDirectChat: (topic: String, prompt: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val subjects = listOf("Physics", "Chemistry", "Mathematics", "Biology", "English", "Computer Science", "Custom")
    var selectedSubject by remember { mutableStateOf("Physics") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Teach Me, 1: Solve Question, 2: Quiz, 3: Flashcards, 4: Progress

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("study_screen")
            .padding(top = 12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeuraBrandHeader(subtitle = "Study Mode")
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeuraCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = NeuraCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("NEURA Mentor", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = NeuraCyan)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Subject Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjects.forEach { subj ->
                FilterChip(
                    selected = selectedSubject == subj,
                    onClick = { selectedSubject = subj },
                    label = { Text(subj, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sub-tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp
        ) {
            val tabs = listOf("Teach Me", "Solve Question", "Quiz Generator", "Flashcards", "Progress")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (selectedTab) {
                0 -> TeachMeTab(
                    subject = selectedSubject,
                    onStartTeachMe = { topic ->
                        val prompt = "Teach Me \"$topic\" in $selectedSubject using the 7-step pedagogical method:\n1. Prerequisites\n2. Basic concept\n3. Simple example\n4. Practical explanation\n5. Practice question\n6. Evaluate answer\n7. Increase difficulty gradually.\nSupport English, Hindi, and Hinglish as appropriate."
                        onLaunchDirectChat("Study: $topic", prompt)
                    }
                )
                1 -> SolveQuestionTab(
                    subject = selectedSubject,
                    onSolve = { question, imageUri ->
                        val prompt = "Please solve this $selectedSubject question step-by-step with clear rationale, formulas, and explanations:\n\n$question"
                        onAskStudyQuestion(prompt, "study", imageUri)
                    }
                )
                2 -> QuizGeneratorTab(
                    subject = selectedSubject,
                    onCompleteQuiz = { topic, score, total ->
                        onRecordProgress(selectedSubject, topic, score, total, "quiz")
                    }
                )
                3 -> FlashcardsTab(subject = selectedSubject)
                4 -> StudyProgressTab(progressList = studyProgressList)
            }
        }
    }
}

@Composable
fun TeachMeTab(
    subject: String,
    onStartTeachMe: (topic: String) -> Unit
) {
    var topicInput by remember { mutableStateOf("") }
    val suggestedTopics = when (subject) {
        "Physics" -> listOf("Newton's Laws of Motion", "Wave Optics & Interference", "Electromagnetic Induction", "Thermodynamics")
        "Chemistry" -> listOf("Chemical Bonding", "Organic Reaction Mechanisms", "Periodic Trends", "Electrochemistry")
        "Mathematics" -> listOf("Calculus & Integration", "Matrices and Determinants", "Probability & Statistics", "Vectors in 3D")
        "Biology" -> listOf("Cell Division & Mitosis", "DNA Replication & Genetics", "Photosynthesis", "Human Circulatory System")
        "Computer Science" -> listOf("Recursion & Dynamic Programming", "Object-Oriented Design", "SQL Queries & Joins", "Binary Search Trees")
        else -> listOf("Key Foundational Principles", "Advanced Applications", "Core Theorems")
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(
                text = "NEURA 7-Step Pedagogical Mastery",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Select or enter a topic to be taught systematically from prerequisites to advanced practice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeuraPurple.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        label = { Text("Enter any $subject topic (e.g. Optics, Quantum…)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (topicInput.isNotBlank()) onStartTeachMe(topicInput)
                        },
                        enabled = topicInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Teach Me Session")
                    }
                }
            }
        }

        item {
            Text(
                text = "Recommended $subject Topics:",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(suggestedTopics) { topic ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onStartTeachMe(topic) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = topic, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "7-Step Guided Exploration", style = MaterialTheme.typography.labelSmall, color = NeuraCyan)
                    }
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeuraCyan, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun SolveQuestionTab(
    subject: String,
    onSolve: (question: String, imageUri: Uri?) -> Unit
) {
    var questionText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        attachedImageUri = uri
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(
                text = "Step-by-Step Question Solver",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Type your homework problem or attach a photo for a full methodical breakdown.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeuraPurple.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        placeholder = { Text("Type question or paste numerical problem here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (attachedImageUri != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeuraCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = NeuraCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Question Image Attached", style = MaterialTheme.typography.labelSmall, color = NeuraCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("✕", modifier = Modifier.clickable { attachedImageUri = null }, color = NeuraCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attach Photo")
                        }

                        Button(
                            onClick = {
                                if (questionText.isNotBlank() || attachedImageUri != null) {
                                    val q = if (questionText.isNotBlank()) questionText else "Please solve the question shown in the attached image step-by-step."
                                    onSolve(q, attachedImageUri)
                                }
                            },
                            enabled = questionText.isNotBlank() || attachedImageUri != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Solve Step-by-Step")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizGeneratorTab(
    subject: String,
    onCompleteQuiz: (topic: String, score: Int, total: Int) -> Unit
) {
    var quizTopic by remember { mutableStateOf("Core $subject Concepts") }
    var questionCount by remember { mutableIntStateOf(5) }
    var difficulty by remember { mutableStateOf("Medium") }
    var isQuizActive by remember { mutableStateOf(false) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedAnswerIndex by remember { mutableIntStateOf(-1) }
    var score by remember { mutableIntStateOf(0) }
    var isSubmitted by remember { mutableStateOf(false) }
    var quizCompleted by remember { mutableStateOf(false) }

    // Sample dynamic questions based on subject
    val questions = remember(subject, questionCount, difficulty) {
        when (subject) {
            "Physics" -> listOf(
                QuizItem("What is the SI unit of electric capacitance?", listOf("Farad", "Henry", "Tesla", "Weber"), 0, "Farad is defined as one coulomb per volt."),
                QuizItem("Which law explains electromagnetic induction?", listOf("Ampere's Law", "Faraday's Law", "Ohm's Law", "Coulomb's Law"), 1, "Faraday's law dictates induced electromotive force."),
                QuizItem("A body falling under gravity in vacuum has constant:", listOf("Velocity", "Acceleration", "Momentum", "Kinetic energy"), 1, "Acceleration due to gravity (g = 9.8 m/s²) remains constant."),
                QuizItem("Total internal reflection occurs when angle of incidence is:", listOf("Less than critical angle", "Equal to 90°", "Greater than critical angle", "Zero"), 2, "TIR happens when angle of incidence exceeds critical angle."),
                QuizItem("Sound waves cannot travel through:", listOf("Steel", "Water", "Vacuum", "Air"), 2, "Sound is a mechanical wave and requires a material medium.")
            )
            "Chemistry" -> listOf(
                QuizItem("What type of hybridization is present in methane (CH4)?", listOf("sp", "sp2", "sp3", "dsp2"), 2, "Carbon in methane forms four equivalent sp3 hybrid orbitals."),
                QuizItem("Which acid is known as the King of Chemicals?", listOf("HCl", "HNO3", "H2SO4", "CH3COOH"), 2, "Sulfuric acid (H2SO4) has widespread industrial importance."),
                QuizItem("What is the oxidation state of Cr in K2Cr2O7?", listOf("+3", "+5", "+6", "+7"), 2, "2(+1) + 2(Cr) + 7(-2) = 0 => 2Cr = +12 => Cr = +6."),
                QuizItem("Avogadro's number is approximately:", listOf("6.022 × 10²³", "3.0 × 10⁸", "9.8 × 10¹¹", "1.6 × 10⁻¹⁹"), 0, "One mole of substance contains 6.022 × 10²³ elementary entities."),
                QuizItem("Which gas is liberated when reactive metal reacts with acid?", listOf("Oxygen", "Nitrogen", "Hydrogen", "Chlorine"), 2, "Acid + Reactive Metal -> Salt + Hydrogen gas (H2).")
            )
            else -> listOf(
                QuizItem("What is the derivative of sin(x)?", listOf("cos(x)", "-cos(x)", "tan(x)", "sec²(x)"), 0, "The standard calculus derivative of sin(x) is cos(x)."),
                QuizItem("If matrix A has dimension 2x3 and B is 3x2, what is AB dimension?", listOf("3x3", "2x2", "2x3", "Undefined"), 1, "Multiplying (2x3) by (3x2) results in a 2x2 matrix."),
                QuizItem("What is the limit of (sin x)/x as x approaches 0?", listOf("0", "1", "Infinity", "Undefined"), 1, "Fundamental trigonometric limit equals 1."),
                QuizItem("Two dice are rolled. Probability of getting sum = 7 is:", listOf("1/12", "1/6", "1/4", "5/36"), 1, "Combinations yielding 7: (1,6),(2,5),(3,4),(4,3),(5,2),(6,1) = 6/36 = 1/6."),
                QuizItem("Vector dot product of two perpendicular vectors is:", listOf("1", "-1", "0", "Infinity"), 2, "A · B = |A||B| cos(90°) = 0.")
            )
        }.take(questionCount)
    }

    if (!isQuizActive) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text(
                    text = "NEURA Quiz Generator",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Configure question count, difficulty, and topic to test your knowledge.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeuraPurple.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = quizTopic,
                            onValueChange = { quizTopic = it },
                            label = { Text("Quiz Topic") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Number of Questions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 20).forEach { count ->
                                FilterChip(
                                    selected = questionCount == count,
                                    onClick = { questionCount = count },
                                    label = { Text("$count Questions") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Difficulty Level", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Easy", "Medium", "Hard", "Mixed").forEach { diff ->
                                FilterChip(
                                    selected = difficulty == diff,
                                    onClick = { difficulty = diff },
                                    label = { Text(diff) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                currentQuestionIndex = 0
                                score = 0
                                selectedAnswerIndex = -1
                                isSubmitted = false
                                quizCompleted = false
                                isQuizActive = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Quiz Now")
                        }
                    }
                }
            }
        }
    } else if (quizCompleted) {
        // Quiz Results
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(NeuraCyan.copy(alpha = 0.2f))
                    .border(2.dp, NeuraCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$score / ${questions.size}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = NeuraCyan
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Quiz Completed! 🎉",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Great effort! Review your concepts and keep exploring.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { isQuizActive = false },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Return to Study Hub")
            }
        }
    } else {
        // Active Quiz Question
        val q = questions[currentQuestionIndex]
        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Score: $score",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = NeuraCyan
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeuraPurple.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = q.question,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Options
            items(q.options.indices.toList()) { idx ->
                val isSelected = selectedAnswerIndex == idx
                val isCorrect = q.correctIndex == idx
                val optionBorderColor = when {
                    isSubmitted && isCorrect -> Color(0xFF10B981)
                    isSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                    isSelected -> NeuraCyan
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isSubmitted) { selectedAnswerIndex = idx },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, optionBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${('A' + idx)}. ",
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) NeuraCyan else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = q.options[idx],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Submit or Next Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                if (!isSubmitted) {
                    Button(
                        onClick = {
                            if (selectedAnswerIndex != -1) {
                                isSubmitted = true
                                if (selectedAnswerIndex == q.correctIndex) {
                                    score++
                                }
                            }
                        },
                        enabled = selectedAnswerIndex != -1,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Submit Answer")
                    }
                } else {
                    // Explanation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (selectedAnswerIndex == q.correctIndex) "✅ Correct!" else "💡 Explanation:",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (selectedAnswerIndex == q.correctIndex) Color(0xFF10B981) else NeuraCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = q.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (currentQuestionIndex + 1 < questions.size) {
                                currentQuestionIndex++
                                selectedAnswerIndex = -1
                                isSubmitted = false
                            } else {
                                quizCompleted = true
                                onCompleteQuiz(quizTopic, score, questions.size)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(if (currentQuestionIndex + 1 < questions.size) "Next Question →" else "Finish Quiz")
                    }
                }
            }
        }
    }
}

data class QuizItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@Composable
fun FlashcardsTab(subject: String) {
    val flashcards = remember(subject) {
        listOf(
            FlashcardItem("What is Snell's Law?", "n1 * sin(θ1) = n2 * sin(θ2), governing optical refraction through boundary interfaces."),
            FlashcardItem("Define Le Chatelier's Principle", "When a system at equilibrium is disturbed, it shifts in the direction that counteracts the disturbance."),
            FlashcardItem("What is the Fundamental Theorem of Calculus?", "It connects differentiation with integration, stating ∫[a,b] f(x)dx = F(b) - F(a) where F'(x) = f(x)."),
            FlashcardItem("What is ATP in Cellular Respiration?", "Adenosine Triphosphate, the primary biochemical energy currency generated during glycolysis & Krebs cycle.")
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f, label = "card_flip")

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Flashcard ${currentIndex + 1} of ${flashcards.size}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        val card = flashcards[currentIndex]

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clip(RoundedCornerShape(20.dp))
                .clickable { isFlipped = !isFlipped },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFlipped) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeuraPurple.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .graphicsLayer {
                        if (isFlipped) rotationY = 180f
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isFlipped) "ANSWER" else "QUESTION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                        color = if (isFlipped) Color(0xFF10B981) else NeuraCyan
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isFlipped) card.back else card.front,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Tap to flip",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // Navigation controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {
                    if (currentIndex > 0) {
                        currentIndex--
                        isFlipped = false
                    }
                },
                enabled = currentIndex > 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("← Previous")
            }

            IconButton(onClick = { isFlipped = !isFlipped }) {
                Icon(Icons.Default.Flip, contentDescription = "Flip", tint = NeuraCyan)
            }

            Button(
                onClick = {
                    if (currentIndex < flashcards.size - 1) {
                        currentIndex++
                        isFlipped = false
                    }
                },
                enabled = currentIndex < flashcards.size - 1,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next →")
            }
        }
    }
}

data class FlashcardItem(val front: String, val back: String)

@Composable
fun StudyProgressTab(progressList: List<StudyProgressEntity>) {
    if (progressList.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.TrendingUp,
            title = "Start studying with NEURA",
            description = "Complete practice quizzes, concept teach-me sessions, and problems to track your learning milestones."
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(
                    text = "Your Learning Milestones",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(progressList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = item.topic, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "${item.subject} • ${item.activityType.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeuraCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${item.score}/${item.totalQuestions}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = NeuraCyan
                            )
                        }
                    }
                }
            }
        }
    }
}
