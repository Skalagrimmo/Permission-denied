package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.window.Dialog

@Composable
fun PauseMenuDialog(
    onDismiss: () -> Unit,
    onQuickSave: () -> Unit,
    onOpenSettings: () -> Unit,
    onReturnToMainMenu: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .border(1.dp, colorScheme.outlineVariant, RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Mission Paused",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Resume Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("resume_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESUME INFILTRATION", color = colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }

                // Quick Save Button
                Button(
                    onClick = onQuickSave,
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primaryContainer),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("quicksave_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QUICK SAVE MISSION", color = colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }

                // Settings Button
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RENDERER & CONTROLS", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }

                // Return to Main Menu Button
                TextButton(
                    onClick = onReturnToMainMenu,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().testTag("abandon_button")
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFB3261E))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ABANDON MISSION", color = Color(0xFFB3261E), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
