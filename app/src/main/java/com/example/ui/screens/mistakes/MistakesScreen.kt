package com.example.ui.screens.mistakes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@Composable
fun MistakesScreen(
    viewModel: AppViewModel,
    onNavigateToPractice: () -> Unit
) {
    val allMistakes by viewModel.allMistakes.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Mistake Notebook, 1 = Flashcards
    var selectedFilter by remember { mutableStateOf("all") } // all, repeated, conceptual, arithmetic

    val filteredMistakes = remember(allMistakes, selectedFilter) {
        when (selectedFilter) {
            "repeated" -> allMistakes.filter { it.repeatCount > 1 }
            "conceptual" -> allMistakes.filter { it.mistakeType == "conceptual" }
            "arithmetic" -> allMistakes.filter { it.mistakeType == "arithmetic" }
            else -> allMistakes
        }
    }

    // Flashcard flipper state
    var currentCardIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("mistakes_screen_content")
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                text = "Mistake Notebook & Revision",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Diagnose root causes, update mental models, and retain with active recall",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Mistake Notebook (${allMistakes.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Active Recall Flashcards (${flashcards.size})", fontWeight = FontWeight.Bold) }
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline)

        if (selectedTab == 0) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "repeated" to "Repeated (Priority)", "conceptual" to "Conceptual", "arithmetic" to "Calculation").forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            if (filteredMistakes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Mistakes in This Category!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Great job maintaining accuracy. Keep practicing to test higher difficulty levels.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMistakes) { mistake ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, if (mistake.repeatCount > 1) ErrorRose else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        color = if (mistake.repeatCount > 1) Color(0xFFFEE2E2) else Color(0xFFFFF1F2),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Category: ${mistake.mistakeType.replaceFirstChar { it.uppercase() }}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ErrorRose,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (mistake.repeatCount > 1) {
                                        Surface(
                                            color = ErrorRose,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Repeated ${mistake.repeatCount}x",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "🔍 Root Cause: ${mistake.rootCause}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("💡 Mental Model:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue)
                                        Text(mistake.mentalModel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        val q = allQuestions.firstOrNull { it.id == mistake.questionId } ?: allQuestions.firstOrNull()
                                        if (q != null) {
                                            viewModel.loadQuestion(q, isRepair = true)
                                            onNavigateToPractice()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fix My Weakness (Repair Drill)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Flashcards Tab
            if (flashcards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No flashcards available yet.")
                }
            } else {
                val card = flashcards[currentCardIndex.coerceIn(0, flashcards.size - 1)]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Card ${currentCardIndex + 1} of ${flashcards.size} (${card.cardType.uppercase()})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isFlipped) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.5.dp, if (isFlipped) SuccessGreen else PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clickable { isFlipped = !isFlipped }
                            .testTag("flashcard_flipper")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    color = if (isFlipped) SuccessGreenLight else Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isFlipped) "ANSWER / EXPLANATION" else "QUESTION / FORMULA (TAP TO FLIP)",
                                        color = if (isFlipped) SuccessGreen else PrimaryBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = if (isFlipped) card.back else card.front,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (currentCardIndex > 0) {
                                    currentCardIndex--
                                    isFlipped = false
                                }
                            },
                            enabled = currentCardIndex > 0,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Previous")
                        }

                        Button(
                            onClick = {
                                if (currentCardIndex < flashcards.size - 1) {
                                    currentCardIndex++
                                    isFlipped = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Next Card ➔")
                        }
                    }
                }
            }
        }
    }
}
