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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DistrictId

@Composable
fun DistrictSelectScreen(
    maxUnlocked: Int,
    onDistrictSelected: (DistrictId) -> Unit,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var selectedDistrict by remember {
        mutableStateOf(DistrictId.entries.firstOrNull { it.level <= maxUnlocked } ?: DistrictId.DISTRICT_01)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
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
                        text = "Infiltration Districts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "Target: Census Spire Ghost Index Mainframe",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Landscape Dual-Pane
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left Pane: District Selector List
                LazyColumn(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DistrictId.entries) { district ->
                        val isUnlocked = district.level <= maxUnlocked
                        val isCurrentSelected = district == selectedDistrict
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    isCurrentSelected -> colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    isUnlocked -> colorScheme.surface
                                    else -> colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isCurrentSelected) colorScheme.primary else if (isUnlocked) colorScheme.outlineVariant else colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = isUnlocked) { selectedDistrict = district }
                                .testTag("district_item_${district.level}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isUnlocked) colorScheme.primaryContainer else colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "0${district.level}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isUnlocked) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = district.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isUnlocked) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = district.theme,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                }
                                if (isUnlocked) {
                                    if (isCurrentSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Pane: Intel Dossier & Deploy Action
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TACTICAL INTEL DOSSIER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                                Text(
                                    text = "THREAT LVL ${selectedDistrict.level}/5",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFB800)
                                )
                            }

                            Text(
                                text = selectedDistrict.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )

                            Text(
                                text = "Sector layout generated via procedural partitioning with high-security ICE terminals, automated drone patrols, and laser grid tripwires.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DistrictStatBadge("ICE RATING", "T-0${selectedDistrict.level}", colorScheme.primary)
                                DistrictStatBadge("MAP SIZE", "${selectedDistrict.size}x${selectedDistrict.size}", colorScheme.onSurfaceVariant)
                            }
                        }

                        Button(
                            onClick = { onDistrictSelected(selectedDistrict) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = CircleShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("deploy_district_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "INFILTRATE ${selectedDistrict.name.replace('_', ' ')}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DistrictStatBadge(label: String, value: String, color: Color) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.surface)
            .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column {
            Text(label, fontSize = 8.sp, color = colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
