package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.QuestionEntity
import com.example.data.util.JsonUtils
import com.example.engine.MistakeDiagnosis
import com.example.ui.theme.*

@Composable
fun QuestionHeaderBadge(question: QuestionEntity) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Source Badge
        val (badgeBg, badgeText, badgeColor) = when {
            question.sourceType == "verified_pyq" -> {
                val shift = question.pyqShift ?: ""
                val year = question.pyqYear ?: 2023
                val exam = question.pyqExam ?: "PYQ"
                Triple(Color(0xFFEDE9FE), "★ $exam $year $shift", Color(0xFF6D28D9))
            }
            question.sourceType == "user_upload" -> {
                Triple(Color(0xFFE0F2FE), "📷 Scanned Question", Color(0xFF0284C7))
            }
            question.sourceType == "uncertain" -> {
                Triple(WarningAmberLight, "⚠️ Verify OCR Reading", WarningAmber)
            }
            else -> {
                Triple(Color(0xFFF1F5F9), "AI Validated (${question.questionFamily})", Color(0xFF475569))
            }
        }

        Surface(
            color = badgeBg,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = badgeText,
                color = badgeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Difficulty & Estimated Seconds
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = when {
                    question.difficulty <= 2 -> SuccessGreenLight
                    question.difficulty <= 5 -> WarningAmberLight
                    else -> ErrorRoseLight
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Lvl ${question.difficulty}/8",
                    color = when {
                        question.difficulty <= 2 -> SuccessGreen
                        question.difficulty <= 5 -> WarningAmber
                        else -> ErrorRose
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Target Time",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${question.estimatedSeconds}s",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionOptionItem(
    optionIndex: Int,
    optionText: String,
    isSelected: Boolean,
    isAnswerChecked: Boolean,
    isCorrectOption: Boolean,
    onSelect: () -> Unit
) {
    val optionLabel = ('A' + optionIndex).toString()

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isAnswerChecked && isCorrectOption -> SuccessGreen
            isAnswerChecked && isSelected && !isCorrectOption -> ErrorRose
            isSelected -> PrimaryBlue
            else -> MaterialTheme.colorScheme.outline
        },
        label = "borderColor"
    )

    val cardBgColor by animateColorAsState(
        targetValue = when {
            isAnswerChecked && isCorrectOption -> Color(0xFFF0FDF4)
            isAnswerChecked && isSelected && !isCorrectOption -> Color(0xFFFEF2F2)
            isSelected -> Color(0xFFEFF6FF)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "bgColor"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected || isAnswerChecked) 2.dp else 1.dp, cardBorderColor),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(enabled = !isAnswerChecked) { onSelect() }
            .testTag("option_$optionLabel")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isAnswerChecked && isCorrectOption -> SuccessGreen
                            isAnswerChecked && isSelected && !isCorrectOption -> ErrorRose
                            isSelected -> PrimaryBlue
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLabel,
                    color = if (isSelected || (isAnswerChecked && isCorrectOption)) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = optionText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (isAnswerChecked) {
                if (isCorrectOption) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Correct",
                        tint = SuccessGreen,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Incorrect",
                        tint = ErrorRose,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MistakeDiagnosisBanner(
    diagnosis: MistakeDiagnosis,
    onTryRepair: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("mistake_diagnosis_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Mistake Diagnosis",
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Error Diagnosis: ${diagnosis.category.title}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9F1239),
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "• What happened: ${diagnosis.whatWentWrong}",
                fontSize = 13.sp,
                color = Color(0xFF4C0519)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• Root cause: ${diagnosis.whyItHappened}",
                fontSize = 13.sp,
                color = Color(0xFF4C0519)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = Color(0xFFFFE4E6),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "💡 Better Mental Model:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF881337)
                    )
                    Text(
                        text = diagnosis.betterMentalModel,
                        fontSize = 13.sp,
                        color = Color(0xFF4C0519)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "🛡️ How to avoid: ${diagnosis.howToAvoid}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF9F1239)
            )

            if (diagnosis.repairQuestion != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onTryRepair,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("try_repair_button")
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Diagnostic Repair Drill", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
