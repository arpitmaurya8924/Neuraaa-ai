package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.ConversationEntity
import com.example.ui.components.FirstLoginWelcomeDialog
import com.example.ui.components.NeuraBottomBar
import com.example.ui.components.NeuraErrorBoundary
import com.example.ui.components.Screen
import com.example.ui.components.VoiceAssistantDialog
import java.util.UUID
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ChatsListScreen
import com.example.ui.screens.CodingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.ResearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StudyScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.VisionScreen
import com.example.ui.theme.NeuraTheme
import com.example.ui.viewmodel.NeuraViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: NeuraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isDarkTheme = when (settings.themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            NeuraTheme(darkTheme = isDarkTheme) {
                NeuraApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun NeuraApp(viewModel: NeuraViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "splash"

    val user by viewModel.user.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val activeConversation by viewModel.activeConversation.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val studyProgress by viewModel.studyProgress.collectAsState()

    // Voice Engine State
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val recognizedSpeechText by viewModel.voiceManager.recognizedText.collectAsState()
    val voiceError by viewModel.voiceManager.voiceError.collectAsState()
    var showVoiceModal by remember { mutableStateOf(false) }

    // Audio Permission Launcher
    val context = androidx.compose.ui.platform.LocalContext.current
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceModal = true
            viewModel.voiceManager.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    val requestVoiceTrigger: () -> Unit = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            showVoiceModal = true
            viewModel.voiceManager.startListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Determine if bottom bar should be visible
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Chats.route,
        Screen.Study.route,
        Screen.Tools.route,
        Screen.Profile.route
    )

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(currentRoute) {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NeuraBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NeuraErrorBoundary(
                onGoHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                composable("splash") {
                    SplashScreen(
                        onSplashFinished = {
                            if (user?.isAuthenticated == true) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo("splash") { inclusive = true }
                                }
                            } else {
                                navController.navigate("auth") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable("auth") {
                    val authLoading by viewModel.isAuthLoading.collectAsState()
                    val authError by viewModel.authError.collectAsState()
                    val pendingEmail by viewModel.verificationPendingEmail.collectAsState()
                    val demoCode by viewModel.verificationDemoCode.collectAsState()

                    AuthScreen(
                        onLoginWithEmail = { em, pass ->
                            viewModel.loginWithEmail(em, pass) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        },
                        onRegisterWithEmail = { nm, em, pass ->
                            viewModel.registerWithEmail(nm, em, pass) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        },
                        onVerifyEmailCode = { code ->
                            viewModel.verifyEmailCode(code) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        },
                        onSignInWithGoogle = { activityContext ->
                            viewModel.signInWithGoogle(activityContext) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        },
                        isLoading = authLoading,
                        errorMessage = authError,
                        pendingVerificationEmail = pendingEmail,
                        verificationDemoCode = demoCode,
                        onClearError = { viewModel.clearAuthError() }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        userName = user?.name ?: "Arpit",
                        isOnline = viewModel.isOnline(),
                        hasApiKey = viewModel.hasValidGeminiKey(),
                        recentConversations = conversations,
                        onSendPrompt = { prompt, mode, imageUri, fileContent ->
                            val newId = viewModel.startNewChat(title = prompt.take(35), mode = mode)
                            viewModel.sendMessage(
                                prompt = prompt,
                                imageUri = imageUri,
                                mode = mode,
                                fileContent = fileContent,
                                targetConvId = newId
                            )
                            navController.navigate("chat_detail/$newId")
                        },
                        onOpenVoiceModal = requestVoiceTrigger,
                        onOpenConversation = { id ->
                            viewModel.selectConversation(id)
                            navController.navigate("chat_detail/$id")
                        },
                        onStartNewChat = {
                            val newId = viewModel.startNewChat()
                            navController.navigate("chat_detail/$newId")
                        },
                        onNavigateMode = { route ->
                            when (route) {
                                "study" -> navController.navigate(Screen.Study.route)
                                "coding" -> navController.navigate("coding")
                                "vision" -> navController.navigate("vision")
                                "tools" -> navController.navigate(Screen.Tools.route)
                                "research" -> navController.navigate("research")
                            }
                        }
                    )
                }

                composable(Screen.Chats.route) {
                    ChatsListScreen(
                        conversations = conversations,
                        onOpenConversation = { id ->
                            viewModel.selectConversation(id)
                            navController.navigate("chat_detail/$id")
                        },
                        onStartNewChat = {
                            val newId = viewModel.startNewChat()
                            navController.navigate("chat_detail/$newId")
                        },
                        onRenameConversation = { id, title ->
                            viewModel.renameConversation(id, title)
                        },
                        onDeleteConversation = { id ->
                            viewModel.deleteConversation(id)
                        }
                    )
                }

                composable("chat_detail/{convId}") { backStackEntry ->
                    val convId = backStackEntry.arguments?.getString("convId") ?: ""
                    LaunchedEffect(convId) {
                        if (convId.isNotBlank()) {
                            viewModel.selectConversation(convId)
                        }
                    }

                    val conv = activeConversation ?: remember(convId) {
                        ConversationEntity(
                            id = convId.ifBlank { UUID.randomUUID().toString() },
                            title = "Chat with NEURA",
                            mode = "general"
                        )
                    }

                    ChatScreen(
                        conversation = conv,
                        messages = activeMessages,
                        isThinking = isThinking,
                        onSendMessage = { text ->
                            viewModel.sendMessage(prompt = text, targetConvId = conv.id, mode = conv.mode)
                        },
                        onStopGeneration = { viewModel.stopGeneration() },
                        onRegenerate = { lastPrompt ->
                            viewModel.regenerateLast(lastPrompt)
                        },
                        onRenameConversation = { newTitle ->
                            viewModel.renameConversation(conv.id, newTitle)
                        },
                        onDeleteConversation = {
                            viewModel.deleteConversation(conv.id)
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Home.route)
                            }
                        },
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Home.route)
                            }
                        },
                        onSpeakText = { text ->
                            viewModel.voiceManager.speak(text)
                        },
                        onOpenVoiceModal = requestVoiceTrigger
                    )
                }

                composable(Screen.Study.route) {
                    StudyScreen(
                        studyProgressList = studyProgress,
                        onAskStudyQuestion = { prompt, mode, imageUri ->
                            val newId = viewModel.startNewChat(title = prompt.take(35), mode = mode)
                            viewModel.sendMessage(
                                prompt = prompt,
                                imageUri = imageUri,
                                mode = mode,
                                targetConvId = newId,
                                customInstruction = "Provide a step-by-step educational breakdown following the 7-step pedagogical model."
                            )
                            navController.navigate("chat_detail/$newId")
                        },
                        onRecordProgress = { subject, topic, score, total, type ->
                            viewModel.recordStudyProgress(subject, topic, score, total, type)
                        },
                        onLaunchDirectChat = { topic, prompt ->
                            val newId = viewModel.startNewChat(title = topic, mode = "study")
                            viewModel.sendMessage(
                                prompt = prompt,
                                mode = "study",
                                targetConvId = newId,
                                customInstruction = "Teach this concept systematically from prerequisites to interactive practice question."
                            )
                            navController.navigate("chat_detail/$newId")
                        }
                    )
                }

                composable("coding") {
                    CodingScreen(
                        onExecuteCodeAction = { prompt, mode ->
                            val newId = viewModel.startNewChat(title = prompt.take(35), mode = mode)
                            viewModel.sendMessage(
                                prompt = prompt,
                                mode = mode,
                                targetConvId = newId,
                                customInstruction = "Format all code inside clean markdown code blocks with syntax highlighting, discuss Big-O, and ensure null-safety."
                            )
                            navController.navigate("chat_detail/$newId")
                        }
                    )
                }

                composable("vision") {
                    VisionScreen(
                        onAnalyzeVisual = { prompt, imageUri ->
                            val newId = viewModel.startNewChat(title = "Vision: " + prompt.take(25), mode = "vision")
                            viewModel.sendMessage(
                                prompt = prompt,
                                imageUri = imageUri,
                                mode = "vision",
                                targetConvId = newId
                            )
                            navController.navigate("chat_detail/$newId")
                        }
                    )
                }

                composable(Screen.Tools.route) {
                    ToolsScreen(
                        onLaunchTool = { title, prompt ->
                            val newId = viewModel.startNewChat(title = title, mode = "general")
                            viewModel.sendMessage(prompt = prompt, targetConvId = newId)
                            navController.navigate("chat_detail/$newId")
                        }
                    )
                }

                composable("research") {
                    ResearchScreen(
                        onStartResearch = { query ->
                            val newId = viewModel.startNewChat(title = "Research: " + query.take(25), mode = "research")
                            viewModel.sendMessage(
                                prompt = query,
                                mode = "research",
                                targetConvId = newId,
                                customInstruction = "Produce an executive summary, detailed multi-source findings, verified source citations with links, and related inquiries."
                            )
                            navController.navigate("chat_detail/$newId")
                        }
                    )
                }

                composable("workspaces") {
                    ProjectsScreen(
                        projects = projects,
                        onCreateProject = { name, desc, cat ->
                            viewModel.createProject(name, desc, cat)
                        },
                        onDeleteProject = { id ->
                            viewModel.deleteProject(id)
                        },
                        onOpenProject = { project ->
                            val newId = viewModel.startNewChat(title = "${project.name} Chat", projectId = project.id)
                            navController.navigate("chat_detail/$newId")
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        user = user,
                        onNavigateToAccount = { navController.navigate("account") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToWorkspaces = { navController.navigate("workspaces") },
                        onNavigateToAbout = { navController.navigate("about") },
                        onSignOut = {
                            viewModel.signOut()
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable("account") {
                    AccountScreen(
                        user = user,
                        onUpdateProfile = { name, avatarUrl ->
                            viewModel.updateProfile(name, avatarUrl)
                        },
                        onSignOut = {
                            viewModel.signOut()
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onDeleteAccount = {
                            viewModel.deleteAccount {
                                navController.navigate("auth") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("settings") {
                    SettingsScreen(
                        settings = settings,
                        memories = memories,
                        onUpdateSettings = { newSettings -> viewModel.updateSettings(newSettings) },
                        onAddMemory = { cat, content -> viewModel.addMemory(cat, content) },
                        onDeleteMemory = { id -> viewModel.deleteMemory(id) },
                        onClearMemories = { viewModel.clearMemories() },
                        onClearAllData = { viewModel.clearAllData() },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("about") {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
            }

            // Voice Assistant Modal
            if (showVoiceModal) {
                VoiceAssistantDialog(
                    isListening = isListening,
                    transcript = recognizedSpeechText,
                    errorMessage = voiceError,
                    onToggleListening = {
                        if (isListening) viewModel.voiceManager.stopListening()
                        else viewModel.voiceManager.startListening()
                    },
                    onSendTranscript = { transcript ->
                        val newId = viewModel.startNewChat(title = transcript.take(35))
                        viewModel.sendMessage(prompt = transcript, targetConvId = newId)
                        navController.navigate("chat_detail/$newId")
                    },
                    onDismiss = {
                        viewModel.voiceManager.stopListening()
                        showVoiceModal = false
                    }
                )
            }

            // First Login Welcome Experience (shown only once on initial account creation)
            if (user?.isAuthenticated == true && user?.isFirstLogin == true) {
                FirstLoginWelcomeDialog(
                    userName = user?.name ?: "Companion",
                    onGetStarted = { viewModel.dismissFirstLoginWelcome() }
                )
            }
        }
    }
}
