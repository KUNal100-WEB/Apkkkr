package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import com.example.engine.ConceptBridgeData
import com.example.ui.theme.ExamIndigo
import com.example.ui.theme.SuccessGreen

@Composable
fun ConceptBridgeModal(
    bridge: ConceptBridgeData,
    onDismiss: () -> Unit,
    onProceed: () -> Unit
) {
    var selectedMicroOption by remember { mutableStateOf<String?>(null) }
    var isMicroChecked by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("concept_bridge_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Bridge Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEDE9FE),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AltRoute,
                                contentDescription = "Concept Bridge",
                                tint = ExamIndigo
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Concept Bridge",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${bridge.oldConceptName} ➔ ${bridge.newConceptName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ExamIndigo,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Relationship Explanation Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🔗 The Connecting Principle",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bridge.relationshipExplanation,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Intuition Example: ${bridge.tinyExample}",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Micro-Question Checkpoint
                Text(
                    text = "Quick Check: ${bridge.microQuestion}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Micro Options
                bridge.microOptions.forEach { opt ->
                    val isSelected = selectedMicroOption == opt
                    val isCorrect = opt.equals(bridge.correctMicroAnswer, ignoreCase = true) ||
                            opt.startsWith(bridge.correctMicroAnswer, ignoreCase = true)

                    val optBg = when {
                        isMicroChecked && isCorrect -> Color(0xFFDCFCE7)
                        isMicroChecked && isSelected && !isCorrect -> Color(0xFFFEE2E2)
                        isSelected -> Color(0xFFEFF6FF)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = optBg,
                        border = BorderStroke(1.dp, if (isSelected) ExamIndigo else MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = !isMicroChecked) {
                                selectedMicroOption = opt
                                isMicroChecked = true
                            }
                            .testTag("bridge_option_$opt")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isMicroChecked && isCorrect) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (isMicroChecked) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = bridge.bridgeFollowup,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SuccessGreen
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onProceed,
                    colors = ButtonDefaults.buttonColors(containerColor = ExamIndigo),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("proceed_practice_button")
                ) {
                    Text(
                        text = if (isMicroChecked) "Start Practicing ${bridge.newConceptName}" else "Skip to Practice",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
