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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun SettingsDialog(
    currentRamp: String,
    currentAnsi: String,
    currentFilter: String,
    currentRenderer: String = "GL_ASCII_3D",
    currentSens: Float,
    soundVol: Float,
    musicVol: Float,
    onSaveSettings: (ramp: String, ansi: String, filter: String, renderer: String, sens: Float, sVol: Float, mVol: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var ramp by remember { mutableStateOf(currentRamp) }
    var ansi by remember { mutableStateOf(currentAnsi) }
    var filter by remember { mutableStateOf(currentFilter) }
    var renderer by remember { mutableStateOf(currentRenderer) }
    var sens by remember { mutableFloatStateOf(currentSens) }
    var sVol by remember { mutableFloatStateOf(soundVol) }
    var mVol by remember { mutableFloatStateOf(musicVol) }

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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Renderer & System Config",
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
                            .testTag("close_settings_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }

                // ASCII Character Ramp Selection
                SectionTitle("ASCII GLYPH RAMP")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("classic", "dense", "cyber", "blocks", "custom").forEach { r ->
                        OptionPill(label = r.uppercase(), isSelected = ramp == r, onClick = { ramp = r })
                    }
                }

                // ANSI Color Mode
                SectionTitle("ANSI COLOR QUANTIZATION")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("GAME", "ANSI_16", "ANSI_256").forEach { a ->
                        OptionPill(label = a, isSelected = ansi == a, onClick = { ansi = a })
                    }
                }

                // Resolution Downsampling Filter
                SectionTitle("MALI-400 DOWNSAMPLING FILTER")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NEAREST", "BOX", "CINEMATIC").forEach { f ->
                        OptionPill(label = f, isSelected = filter == f, onClick = { filter = f })
                    }
                }

                // Touch Sensitivity
                SectionTitle("TOUCH LOOK SENSITIVITY: ${(sens * 100).toInt()}%")
                Slider(
                    value = sens,
                    onValueChange = { sens = it },
                    valueRange = 0.4f..2.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = colorScheme.primary,
                        activeTrackColor = colorScheme.primary,
                        inactiveTrackColor = colorScheme.surfaceVariant
                    )
                )

                // Audio Volume
                SectionTitle("SOUND EFFECTS VOLUME: ${(sVol * 100).toInt()}%")
                Slider(
                    value = sVol,
                    onValueChange = { sVol = it },
                    valueRange = 0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = colorScheme.primary, activeTrackColor = colorScheme.primary)
                )

                SectionTitle("SYNTHWAVE MUSIC VOLUME: ${(mVol * 100).toInt()}%")
                Slider(
                    value = mVol,
                    onValueChange = { mVol = it },
                    valueRange = 0f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = colorScheme.primary, activeTrackColor = colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Renderer Pipeline (GLSurfaceView 3D vs Canvas)
                SectionTitle("RENDERER PIPELINE")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("GL_ASCII_3D", "CANVAS_ASCII").forEach { r ->
                        OptionPill(
                            label = if (r == "GL_ASCII_3D") "3D GL SURFACE" else "2D CANVAS",
                            isSelected = renderer == r,
                            onClick = { renderer = r }
                        )
                    }
                }

                // Save button
                Button(
                    onClick = {
                        onSaveSettings(ramp, ansi, filter, renderer, sens, sVol, mVol)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_settings_button")
                ) {
                    Text("APPLY CONFIGURATION", color = colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = colorScheme.primary
    )
}

@Composable
private fun OptionPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) colorScheme.primary else colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}
