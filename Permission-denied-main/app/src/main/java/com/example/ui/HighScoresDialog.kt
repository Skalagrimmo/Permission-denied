package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.HighScoreEntity

@Composable
fun HighScoresDialog(
    scores: List<HighScoreEntity>,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Leaderboard Scores",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant)
                            .testTag("close_leaderboard_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (scores.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No recorded mission scores yet.\nInfiltrate Census Spire to register entry.",
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(scores) { index, score ->
                            val rankColor = when (index) {
                                0 -> Color(0xFF6750A4)
                                1 -> Color(0xFF7D5260)
                                2 -> Color(0xFF625B71)
                                else -> colorScheme.onSurfaceVariant
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#${index + 1}",
                                            color = rankColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.width(32.dp)
                                        )
                                        Column {
                                            Text(
                                                text = score.districtName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${score.stealthRating} • ${score.endingChoice} • ${score.timeElapsedSeconds}s",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${score.finalScore} PTS",
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
