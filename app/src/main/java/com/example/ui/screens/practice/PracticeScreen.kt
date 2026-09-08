package com.example.ui.screens.practice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.engine.TutorEngine
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@Composable
fun PracticeScreen(
    viewModel: AppViewModel,
    onNavigateToLearn: () -> Unit
) {
    val activeQuestionState by viewModel.activeQuestion.collectAsState()
    val tutorMessage by viewModel.tutorMessage.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()

    var showHintLadder by remember { mutableStateOf(false) }
    var showSolveWithMe by remember { mutableStateOf(false) }
    var showAskWhyDialog by remember { mutableStateOf(false) }
    var studentQueryText by remember { mutableStateOf("") }

    // If no active question loaded yet, auto-load first available question
    LaunchedEffect(activeQuestionState, allQuestions) {
        if (activeQuestionState == null && allQuestions.isNotEmpty()) {
            viewModel.loadQuestion(allQuestions.first())
        }
    }

    val state = activeQuestionState

    if (state == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = PrimaryBlue)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Loading adaptive question...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val question = state.question
    val options = remember(question.optionsJson) {
        JsonUtils.jsonToStringList(question.optionsJson)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("practice_screen_content")
    ) {
        // Sticky Header with Question metadata
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                QuestionHeaderBadge(question = question)
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline)

        // Question Content & Options
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Repair Drill Indicator
            if (state.isRepairQuestion) {
                item {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Targeted Diagnostic Repair Drill",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }

            // Question Text
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("question_text_card")
                ) {
                    Text(
                        text = question.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }

            // Multiple Choice Options
            items(options.size) { index ->
                val optionText = options[index]
                val isSelected = state.selectedOption == optionText
                val isCorrect = optionText.equals(question.answer, ignoreCase = true) ||
                        optionText.startsWith(question.answer, ignoreCase = true) ||
                        (question.answer.length == 1 && optionText.startsWith(question.answer, ignoreCase = true))

                QuestionOptionItem(
                    optionIndex = index,
                    optionText = optionText,
                    isSelected = isSelected,
                    isAnswerChecked = state.isAnswerChecked,
                    isCorrectOption = isCorrect,
                    onSelect = { viewModel.selectOption(optionText) }
                )
            }

            // AI Tutor Assistance Row
            if (!state.isAnswerChecked) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Solve With Me
                        OutlinedButton(
                            onClick = {
                                viewModel.startSolveWithMe()
                                showSolveWithMe = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("solve_with_me_button")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Solve With Me", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Hint Ladder
                        OutlinedButton(
                            onClick = {
                                if (state.currentHintLevel == 0) viewModel.unlockNextHint()
                                showHintLadder = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hint_ladder_button")
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hint (${state.currentHintLevel}/6)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Ask Tutor Why
                        OutlinedButton(
                            onClick = { showAskWhyDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ask_tutor_why_button")
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ask Why", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Confidence Selector (1 to 5)
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Your Confidence:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (rating in 1..5) {
                                    val isSelected = rating <= state.confidence
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Confidence $rating",
                                        tint = if (isSelected) WarningAmber else Color.Gray,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { viewModel.setConfidence(rating) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Submit Button
                item {
                    Button(
                        onClick = { viewModel.submitCurrentAnswer() },
                        enabled = state.selectedOption != null,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_answer_button")
                    ) {
                        Text("Check Answer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Results & Explanations after check
            if (state.isAnswerChecked) {
                // Mistake diagnosis if incorrect
                if (!state.isCorrect && state.diagnosis != null) {
                    item {
                        MistakeDiagnosisBanner(
                            diagnosis = state.diagnosis,
                            onTryRepair = {
                                state.diagnosis.repairQuestion?.let { repairQ ->
                                    viewModel.loadQuestion(repairQ, isRepair = true)
                                }
                            }
                        )
                    }
                }

                // Solution & Explanation Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("solution_explanation_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (state.isCorrect) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (state.isCorrect) SuccessGreen else PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isCorrect) "Correct! Step-by-Step Solution" else "Detailed Solution & Method",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = question.solution,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (question.trap.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFFFFFBEB),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⚠️ Exam Trap Warning: ${question.trap}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Next Adaptive Question Button
                item {
                    Button(
                        onClick = {
                            val nextQ = allQuestions.firstOrNull { it.id != question.id } ?: question
                            viewModel.loadQuestion(nextQ)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("next_question_button")
                    ) {
                        Text("Next Adaptive Problem ➔", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Tutor Response Banner if any
            if (tutorMessage != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, PrimaryBlueLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.School, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AI Tutor Explanation", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 13.sp)
                                }
                                IconButton(onClick = { viewModel.clearTutorMessage() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = tutorMessage!!, fontSize = 13.sp, color = Color(0xFF1E3A8A), lineHeight = 19.sp)
                        }
                    }
                }
            }
        }
    }

    // 6-Level Hint Ladder Sheet
    if (showHintLadder) {
        val hints = remember(question) {
            com.example.data.repository.AppRepository.getInstance(viewModel.getApplication()).tutorEngine.getHintLadder(question)
        }
        HintLadderSheet(
            hints = hints,
            unlockedLevel = state.currentHintLevel,
            onUnlockNext = { viewModel.unlockNextHint() },
            onDismiss = { showHintLadder = false }
        )
    }

    // Solve With Me Dialog
    if (showSolveWithMe && state.solveWithMeSteps.isNotEmpty()) {
        SolveWithMeDialog(
            steps = state.solveWithMeSteps,
            currentStepIndex = state.currentStepIndex,
            onAdvanceStep = {
                viewModel.advanceSolveWithMeStep()
                if (state.currentStepIndex + 1 >= state.solveWithMeSteps.size) {
                    showSolveWithMe = false
                }
            },
            onDismiss = { showSolveWithMe = false }
        )
    }

    // Ask Why Dialog
    if (showAskWhyDialog) {
        AlertDialog(
            onDismissRequest = { showAskWhyDialog = false },
            title = { Text("Ask Socratic Tutor Why", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Ask any confusion in English, Hindi, or Hinglish (e.g. 'Bhai ye 1/5 kahan se aaya?')",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = studentQueryText,
                        onValueChange = { studentQueryText = it },
                        placeholder = { Text("Apna doubt yahan likho...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ask_why_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (studentQueryText.isNotBlank()) {
                            viewModel.askTutorWhy(studentQueryText)
                            studentQueryText = ""
                            showAskWhyDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Ask Tutor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAskWhyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
