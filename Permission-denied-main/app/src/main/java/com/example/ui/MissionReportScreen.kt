package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EndingChoice
import com.example.model.MissionReport

@Composable
fun MissionReportScreen(
    report: MissionReport?,
    onChooseEnding: (EndingChoice) -> Unit,
    onContinue: () -> Unit
) {
    var selectedEnding by remember { mutableStateOf<EndingChoice?>(report?.endingChoice) }
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Mission Accomplished",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.primary
                        )
                        Text(
                            text = report?.districtName?.uppercase() ?: "DISTRICT CLEAR",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF48A252))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = report?.stealthRating ?: "GHOST (S+)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Landscape Dual-Pane
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Left Pane: Score & Telemetry Breakdown
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ReportRow("FINAL GHOST SCORE", "${report?.score ?: 0} PTS", colorScheme.primary, isHighlight = true)
                            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ReportRow("ALARMS TRIGGERED", "${report?.alarmsTriggered ?: 0}", if ((report?.alarmsTriggered ?: 0) == 0) Color(0xFF48A252) else Color(0xFFB3261E))
                            ReportRow("ENEMIES STUNNED", "${report?.enemiesStunned ?: 0}", colorScheme.primary)
                            ReportRow("ENEMIES KILLED", "${report?.enemiesKilled ?: 0}", colorScheme.onSurfaceVariant)
                            ReportRow("TERMINALS HACKED", "${report?.terminalsHacked ?: 0}", colorScheme.primary)
                            ReportRow("EVIDENCE SLATES", "${report?.evidenceFound ?: 0}", colorScheme.primary)
                            ReportRow("MISSION TIME", "${(report?.timeElapsedSeconds ?: 0) / 60}m ${(report?.timeElapsedSeconds ?: 0) % 60}s", colorScheme.onSurfaceVariant)
                        }
                    }

                    // Right Pane: Ghost Index Disposition & Continue Button
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "GHOST INDEX DISPOSITION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )

                            EndingOptionCard(
                                choice = EndingChoice.LEAK,
                                isSelected = selectedEnding == EndingChoice.LEAK,
                                onClick = {
                                    selectedEnding = EndingChoice.LEAK
                                    onChooseEnding(EndingChoice.LEAK)
                                }
                            )
                            EndingOptionCard(
                                choice = EndingChoice.SELL,
                                isSelected = selectedEnding == EndingChoice.SELL,
                                onClick = {
                                    selectedEnding = EndingChoice.SELL
                                    onChooseEnding(EndingChoice.SELL)
                                }
                            )
                            EndingOptionCard(
                                choice = EndingChoice.SURRENDER,
                                isSelected = selectedEnding == EndingChoice.SURRENDER,
                                onClick = {
                                    selectedEnding = EndingChoice.SURRENDER
                                    onChooseEnding(EndingChoice.SURRENDER)
                                }
                            )
                            EndingOptionCard(
                                choice = EndingChoice.DESTROY,
                                isSelected = selectedEnding == EndingChoice.DESTROY,
                                onClick = {
                                    selectedEnding = EndingChoice.DESTROY
                                    onChooseEnding(EndingChoice.DESTROY)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Continue / Next District
                        Button(
                            onClick = onContinue,
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("continue_button")
                        ) {
                            Text(
                                text = "SUBMIT INTEL & ADVANCE",
                                color = colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String, color: Color, isHighlight: Boolean = false) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = if (isHighlight) colorScheme.onSurface else colorScheme.onSurfaceVariant,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isHighlight) 11.sp else 10.sp
        )
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = if (isHighlight) 13.sp else 11.sp
        )
    }
}

@Composable
private fun EndingOptionCard(
    choice: EndingChoice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val borderColor = if (isSelected) colorScheme.primary else colorScheme.outlineVariant
    val bgColor = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.4f) else colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colorScheme.primary else colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (choice) {
                        EndingChoice.LEAK -> Icons.Default.Public
                        EndingChoice.SELL -> Icons.Default.Sell
                        EndingChoice.SURRENDER -> Icons.Default.Shield
                        EndingChoice.DESTROY -> Icons.Default.Whatshot
                    },
                    contentDescription = null,
                    tint = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${choice.title} (+${choice.scoreBonus} PTS)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                    fontSize = 11.sp
                )
                Text(
                    text = choice.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Color(0xFF48A252), modifier = Modifier.size(16.dp))
            }
        }
    }
}
