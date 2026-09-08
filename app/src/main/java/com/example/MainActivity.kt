package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamType
import com.example.ui.components.ConceptBridgeModal
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.learn.LearnScreen
import com.example.ui.screens.mistakes.MistakesScreen
import com.example.ui.screens.mock.MockExamScreen
import com.example.ui.screens.practice.PracticeScreen
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.scan.ScanScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

enum class NavigationTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    LEARN("learn", "Learn", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    PRACTICE("practice", "Practice", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline),
    SCAN("scan", "Scan", Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner),
    MISTAKES("mistakes", "Mistakes", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    PROGRESS("progress", "Progress", Icons.Filled.BarChart, Icons.Outlined.BarChart)
}

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val userProfile by viewModel.userProfile.collectAsState()
                val activeBridge by viewModel.activeConceptBridge.collectAsState()
                val activeMock by viewModel.activeMock.collectAsState()

                var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
                var showSettings by remember { mutableStateOf(false) }

                val targetExam = ExamType.values().firstOrNull { it.id == userProfile?.targetExamId } ?: ExamType.SSC_CGL

                // If taking a mock test, display full mock screen without standard chrome
                if (activeMock != null) {
                    MockExamScreen(
                        viewModel = viewModel,
                        onFinish = {
                            currentTab = NavigationTab.HOME
                        }
                    )
                } else if (showSettings) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("Settings & Profile", fontWeight = FontWeight.Bold) },
                                navigationIcon = {
                                    IconButton(onClick = { showSettings = false }) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            SettingsScreen(viewModel = viewModel)
                        }
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Text(
                                            text = "Adaptive Exam Tutor",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${targetExam.title} • ${userProfile?.preferredLanguage?.replaceFirstChar { it.uppercase() } ?: "Hinglish"}",
                                            fontSize = 11.sp,
                                            color = PrimaryBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = { showSettings = true },
                                        modifier = Modifier.testTag("settings_button")
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                                modifier = Modifier.testTag("bottom_navigation_bar")
                            ) {
                                NavigationTab.values().forEach { tab ->
                                    val isSelected = currentTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title
                                            )
                                        },
                                        label = { Text(tab.title, fontSize = 11.sp) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PrimaryBlue,
                                            selectedTextColor = PrimaryBlue,
                                            indicatorColor = Color(0xFFEFF6FF)
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                NavigationTab.HOME -> HomeScreen(
                                    viewModel = viewModel,
                                    onNavigateToLearn = { currentTab = NavigationTab.LEARN },
                                    onNavigateToPractice = { currentTab = NavigationTab.PRACTICE },
                                    onNavigateToScan = { currentTab = NavigationTab.SCAN },
                                    onNavigateToMistakes = { currentTab = NavigationTab.MISTAKES },
                                    onNavigateToMock = { /* Mock launched by activeMock state */ }
                                )
                                NavigationTab.LEARN -> LearnScreen(
                                    viewModel = viewModel,
                                    onNavigateToPractice = { currentTab = NavigationTab.PRACTICE }
                                )
                                NavigationTab.PRACTICE -> PracticeScreen(
                                    viewModel = viewModel,
                                    onNavigateToLearn = { currentTab = NavigationTab.LEARN }
                                )
                                NavigationTab.SCAN -> ScanScreen(
                                    viewModel = viewModel,
                                    onNavigateToPractice = { currentTab = NavigationTab.PRACTICE }
                                )
                                NavigationTab.MISTAKES -> MistakesScreen(
                                    viewModel = viewModel,
                                    onNavigateToPractice = { currentTab = NavigationTab.PRACTICE }
                                )
                                NavigationTab.PROGRESS -> ProgressScreen(
                                    viewModel = viewModel
                                )
                            }

                            // Global Socratic Concept Bridge Modal
                            activeBridge?.let { bridge ->
                                ConceptBridgeModal(
                                    bridge = bridge,
                                    onDismiss = { viewModel.dismissConceptBridge() },
                                    onProceed = {
                                        viewModel.dismissConceptBridge()
                                        currentTab = NavigationTab.PRACTICE
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
