package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.renderer.FirstPersonCameraController

/**
 * Overlay HUD displayed during the 3D Cyberpunk Primitives Showcase.
 * Provides camera controller status, touch controls instructions, reset camera button,
 * and quick settings access.
 */
@Composable
fun ShowcaseHudOverlay(
    cameraController: FirstPersonCameraController?,
    onExitShowcase: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Top Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC050A10)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .border(1.dp, Color(0x6600F0FF), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onExitShowcase,
                        modifier = Modifier.size(36.dp).testTag("exit_showcase_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Showcase",
                            tint = Color(0xFF00F0FF)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "3D FIRST-PERSON INSPECTOR",
                            color = Color(0xFF00F0FF),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "GLSurfaceView • ASCII Post-Shader Engine",
                            color = Color(0xFF88A0B0),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { cameraController?.reset() },
                        modifier = Modifier.size(36.dp).testTag("reset_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Camera",
                            tint = Color(0xFFFF0077)
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(36.dp).testTag("showcase_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF00F0FF)
                        )
                    }
                }
            }
        }

        // Bottom Navigation Guide Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xCC050A10)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .border(1.dp, Color(0x4400F0FF), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🕹️ LEFT DRAG",
                        color = Color(0xFF00F0FF),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Move / Strafe 3D",
                        color = Color(0xFFC0D0E0),
                        fontSize = 10.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color(0x4400F0FF))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "👁️ RIGHT DRAG",
                        color = Color(0xFFFF0077),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "First-Person Look",
                        color = Color(0xFFC0D0E0),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
