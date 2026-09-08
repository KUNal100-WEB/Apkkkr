package com.example.ui.screens.mock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.JsonUtils
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@Composable
fun MockExamScreen(
    viewModel: AppViewModel,
    onFinish: () -> Unit
) {
    val activeMockState by viewModel.activeMock.collectAsState()
    val state = activeMockState

    if (state == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    var showSubmitConfirmation by remember { mutableStateOf(false) }

    // If already submitted, show results screen
    if (state.isSubmitted && state.resultSummary != null) {
        val res = state.resultSummary!!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mock_result_card")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Mock Test Completed!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Here is your scored performance with negative marking calibration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Net Score", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%.1f", res.netScore), fontWeight = FontWeight.Bold, fontSize = 26.sp, color = PrimaryBlue)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Accuracy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${res.accuracyPercentage.toInt()}%", fontWeight = FontWeight.Bold, fontSize = 26.sp, color = SuccessGreen)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Negative Penalty", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-${String.format("%.2f", res.negativePenalty)}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = ErrorRose)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Divider(color = MaterialTheme.colorScheme.outline)

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Correct Answers: ${res.correctCount}", color = SuccessGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Incorrect: ${res.incorrectCount}", color = ErrorRose, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Skipped: ${res.skippedCount}", color = Color.Gray, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    if (res.weakTopics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("⚠️ High Priority Weak Areas:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                                Text(res.weakTopics.joinToString(", "), fontSize = 12.sp, color = Color(0xFF78350F))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.exitMockTest()
                            onFinish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("finish_mock_button")
                    ) {
                        Text("Return to Learning Dashboard", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    val currentQ = state.questions.getOrNull(state.currentIndex)
    val currentPalette = state.paletteStates.getOrNull(state.currentIndex)
    val options = remember(currentQ?.optionsJson) {
        if (currentQ != null) JsonUtils.jsonToStringList(currentQ.optionsJson) else emptyList()
    }

    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("mock_exam_screen")
    ) {
        // Exam Top App Bar with Timer
        Surface(
            color = ExamIndigo,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = state.mockTest.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Question ${state.currentIndex + 1} of ${state.questions.size}",
                        fontSize = 12.sp,
                        color = AccentCyan
                    )
                }

                Surface(
                    color = if (state.remainingSeconds < 180) ErrorRose else Color(0x33FFFFFF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Button(
                    onClick = { showSubmitConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("submit_mock_header_button")
                ) {
                    Text("Submit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Question Palette Grid (Mini)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(10),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(state.paletteStates) { index, pal ->
                    val isCurrent = index == state.currentIndex
                    val (badgeBg, badgeTextColor) = when {
                        pal.isMarkedForReview -> Pair(Color(0xFF8B5CF6), Color.White)
                        pal.selectedOption != null -> Pair(SuccessGreen, Color.White)
                        pal.isVisited -> Pair(WarningAmber, Color.White)
                        else -> Pair(Color(0xFFE2E8F0), Color.DarkGray)
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .clickable { viewModel.navigateMockQuestion(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = badgeTextColor,
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline)

        // Question Body
        if (currentQ != null) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = currentQ.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp
                    )
                }

                items(options.size) { optIdx ->
                    val opt = options[optIdx]
                    val isSelected = currentPalette?.selectedOption == opt
                    val optLabel = ('A' + optIdx).toString()

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.selectMockOption(opt) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optLabel,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = opt, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Bottom Navigation Controls
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.toggleMockMarkForReview() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (currentPalette?.isMarkedForReview == true) "Unmark" else "Mark for Review", fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.currentIndex > 0) {
                        OutlinedButton(
                            onClick = { viewModel.navigateMockQuestion(state.currentIndex - 1) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Prev", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (state.currentIndex < state.questions.size - 1) {
                                viewModel.navigateMockQuestion(state.currentIndex + 1)
                            } else {
                                showSubmitConfirmation = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_and_next_mock_button")
                    ) {
                        Text(if (state.currentIndex < state.questions.size - 1) "Save & Next ➔" else "Finish", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showSubmitConfirmation) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirmation = false },
            title = { Text("Submit Mock Test?", fontWeight = FontWeight.Bold) },
            text = {
                val answered = state.paletteStates.count { it.selectedOption != null }
                val marked = state.paletteStates.count { it.isMarkedForReview }
                val remaining = state.paletteStates.size - answered
                Text("Answered: $answered\nMarked for Review: $marked\nUnanswered: $remaining\n\nAre you sure you want to submit your exam now?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmation = false
                        viewModel.submitMockTest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Yes, Submit Test")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmation = false }) {
                    Text("Resume Test")
                }
            }
        )
    }
}
