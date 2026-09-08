package com.example.ui.screens.learn

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
import com.example.data.local.entity.ConceptEntity
import com.example.data.model.ConceptStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

@Composable
fun LearnScreen(
    viewModel: AppViewModel,
    onNavigateToPractice: () -> Unit
) {
    val subjects by viewModel.subjects.collectAsState()
    val allConcepts by viewModel.allConcepts.collectAsState()
    val allMastery by viewModel.allMastery.collectAsState()

    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(subjects) {
        if (selectedSubjectId == null && subjects.isNotEmpty()) {
            selectedSubjectId = subjects.first().id
        }
    }

    val masteryMap = remember(allMastery) {
        allMastery.associateBy { it.conceptId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("learn_screen_content")
    ) {
        // Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text(
                text = "Exam Curriculum & Knowledge Graph",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Track your multidimensional mastery across topics and prerequisites",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subject Scrollable / Scroll Tab Row
            if (subjects.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = subjects.indexOfFirst { it.id == selectedSubjectId }.coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {},
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    subjects.forEach { subject ->
                        val isSelected = subject.id == selectedSubjectId
                        Tab(
                            selected = isSelected,
                            onClick = { selectedSubjectId = subject.id },
                            text = {
                                Text(
                                    text = subject.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outline)

        // Concepts List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allConcepts) { concept ->
                val mastery = masteryMap[concept.id]
                val score = mastery?.conceptScore ?: 0

                val (statusText, statusBg, statusColor) = when {
                    score >= 80 -> Triple("MASTERED", SuccessGreenLight, SuccessGreen)
                    score >= 50 -> Triple("PRACTICING", Color(0xFFEFF6FF), PrimaryBlue)
                    score > 0 -> Triple("WEAK GAP", WarningAmberLight, WarningAmber)
                    else -> Triple("NOT STARTED", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.startConceptPractice(concept)
                            onNavigateToPractice()
                        }
                        .testTag("concept_card_${concept.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = concept.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = concept.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        if (concept.formulaSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "📐 Key Relation: ${concept.formulaSummary}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mastery Dimensions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("Concept Mastery", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$score%", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = statusColor)
                            }

                            Column {
                                Text("Speed Score", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${mastery?.speedScore ?: 0}/100", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            Column {
                                Text("Hint Dependency", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${mastery?.hintDependency ?: 0}%", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            IconButton(
                                onClick = {
                                    viewModel.startConceptPractice(concept)
                                    onNavigateToPractice()
                                }
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = "Practice", tint = PrimaryBlue, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
