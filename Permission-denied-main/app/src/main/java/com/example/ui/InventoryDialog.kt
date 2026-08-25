package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import com.example.engine.GameEngine
import com.example.model.AugmentationType
import com.example.model.InventoryItem
import com.example.model.ItemType

@Composable
fun InventoryDialog(
    engine: GameEngine,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gear & Inventory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colorScheme.surfaceVariant)
                            .testTag("close_inventory_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TabPill("ITEMS", isSelected = selectedTab == 0, onClick = { selectedTab = 0 })
                    TabPill("WEAPONS", isSelected = selectedTab == 1, onClick = { selectedTab = 1 })
                    TabPill("AUGMENTS", isSelected = selectedTab == 2, onClick = { selectedTab = 2 })
                    TabPill("INTEL", isSelected = selectedTab == 3, onClick = { selectedTab = 3 })
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> ItemsTab(engine)
                        1 -> WeaponsTab(engine)
                        2 -> AugmentsTab(engine)
                        3 -> IntelTab(engine)
                    }
                }

                // Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CREDITS: ¢${engine.playerCredits}",
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (engine.ghostIndexAcquired) "GHOST INDEX: ONLINE" else "GHOST INDEX: PENDING",
                        color = if (engine.ghostIndexAcquired) Color(0xFF48A252) else colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPill(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) colorScheme.primary else colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun ItemsTab(engine: GameEngine) {
    val colorScheme = MaterialTheme.colorScheme
    if (engine.inventory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No consumables in storage", color = colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(engine.inventory) { item ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.name} (x${item.count})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { engine.useItem(item.type) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("USE", color = colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponsTab(engine: GameEngine) {
    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(engine.weapons) { weapon ->
            val isSelected = engine.currentWeapon == weapon
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.4f) else colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { engine.currentWeaponIndex = engine.weapons.indexOf(weapon) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = weapon.type.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                        )
                        Text(
                            text = "MAG: ${weapon.ammoInMag}/${weapon.maxMag} | RESERVE: ${weapon.reserveAmmo} | DMG: ${weapon.damage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("EQUIPPED", color = colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AugmentsTab(engine: GameEngine) {
    val colorScheme = MaterialTheme.colorScheme
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AugmentationType.entries) { aug ->
            val isActive = engine.activeAugmentations.contains(aug)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) colorScheme.primaryContainer.copy(alpha = 0.4f) else colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isActive) colorScheme.primary else colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = aug.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isActive) colorScheme.primary else colorScheme.onSurface
                        )
                        Text(
                            text = aug.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF48A252))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("ACTIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntelTab(engine: GameEngine) {
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "GHOST INDEX CENSUS INTELLIGENCE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "District Target: ${engine.world.district.title}\nPrimary Target: Aegis Paladin on Spire Top Floor\nStatus: ${if (engine.ghostIndexAcquired) "Secured - Route to Extraction" else "Infiltrate Spire Core"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "FIELD EVIDENCE & CONTRACTS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Evidence Slates Found: ${engine.evidenceCollected}/${engine.world.evidenceCount}\nFaction Contracts: ${engine.contractsCompleted}/${engine.world.contractsCount}\nTerminals Compromised: ${engine.terminalsHacked}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}
