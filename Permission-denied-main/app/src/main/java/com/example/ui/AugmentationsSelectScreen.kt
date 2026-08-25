package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.model.AugmentationType
import com.example.model.DistrictId

@Composable
fun AugmentationsSelectScreen(
    district: DistrictId,
    selectedAugmentations: Set<AugmentationType>,
    onToggleAugmentation: (AugmentationType) -> Unit,
    onStartMission: () -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceVariant)
                        .testTag("back_button")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = colorScheme.onSurface, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Cybernetic Loadout",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Configure neural cybernetics for ${district.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Pane: Augmentations Selection List
                LazyColumn(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AugmentationType.entries) { aug ->
                        val isSelected = selectedAugmentations.contains(aug)
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.45f) else colorScheme.surface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { onToggleAugmentation(aug) }
                                .testTag("aug_item_${aug.name}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = aug.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) colorScheme.primary else colorScheme.onSurface,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (isSelected) colorScheme.primary else colorScheme.surfaceVariant)
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = aug.subtitle,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = aug.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = Color(0xFF48A252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Pane: Neural Capacity Gauge & Deploy Button
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "NEURAL SYNC STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.primary
                            )

                            // Capacity Meter
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("CYBERNETIC SLOTS", fontSize = 10.sp, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Text("${selectedAugmentations.size} / 3 ACTIVE", fontSize = 10.sp, color = colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (selectedAugmentations.size / 3f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = if (selectedAugmentations.size <= 3) colorScheme.primary else Color(0xFFB3261E),
                                    trackColor = colorScheme.surface
                                )
                            }

                            Text(
                                text = "Equipped augments synergize with tactile suit sensors and HUD optics during infiltration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }

                        Button(
                            onClick = onStartMission,
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("launch_mission_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DEPLOY INFILTRATOR",
                                color = colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
