package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    uiState: GameUiState
) {
    var showSettingsInGame by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 3D ASCII Hardware Rasterizer Surface
        AndroidView(
            factory = { ctx ->
                GameCanvasView(ctx).apply {
                    engine = viewModel.engine
                    asciiRamp = uiState.asciiRamp
                    ansiMode = uiState.ansiMode
                    filterMode = uiState.filterMode
                }
            },
            update = { view ->
                view.engine = viewModel.engine
                view.asciiRamp = uiState.asciiRamp
                view.ansiMode = uiState.ansiMode
                view.filterMode = uiState.filterMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Interactive HUD Overlay
        if (!viewModel.engine.isGameOver && !viewModel.engine.isMissionComplete) {
            GameHudOverlay(
                engine = viewModel.engine,
                sensitivity = uiState.touchSensitivity,
                onPauseClick = { viewModel.togglePause() },
                onInventoryClick = { viewModel.toggleInventory() },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. Game Over Screen
        if (viewModel.engine.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE090305)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14080B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .border(1.5.dp, Color(0xFFFF1744), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "SIGNAL TERMINATED",
                            color = Color(0xFFFF1744),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Operative flatlined during infiltration of ${viewModel.engine.world.district.title}.",
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Restart District
                        Button(
                            onClick = { viewModel.startMission() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F0FF)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().testTag("restart_district_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RETRY MISSION", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }

                        // Load Last Save if exists
                        uiState.latestSave?.let { save ->
                            Button(
                                onClick = { viewModel.loadSavedGame(save) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131B26)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00FF66), RoundedCornerShape(6.dp)).testTag("reload_save_button")
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = Color(0xFF00FF66))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RELOAD QUICKSAVE", color = Color(0xFF00FF66), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Return to Menu
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.MAIN_MENU) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131B26)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().testTag("game_over_menu_button")
                        ) {
                            Text("RETURN TO MAIN MENU", color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // 4. Modals and Dialogs
        viewModel.engine.activeHacking?.let { hackSession ->
            HackingDialog(
                session = hackSession,
                onNodeClick = { nodeId ->
                    hackSession.nodes.find { it.id == nodeId }?.let { node ->
                        if (!node.isCaptured) {
                            val isAdjacent = hackSession.nodes.any { it.isCaptured && it.connectedNodeIds.contains(node.id) }
                            if (isAdjacent) {
                                node.currentHp -= 35
                                if (node.currentHp <= 0) {
                                    node.currentHp = 0
                                    node.isCaptured = true
                                    viewModel.audioEngine.playHackBeep(true)
                                } else {
                                    viewModel.audioEngine.playUiClick()
                                }
                            }
                        }
                    }
                },
                onDismiss = { viewModel.closeHackingModal() }
            )
        }

        if (uiState.isInventoryOpen) {
            InventoryDialog(
                engine = viewModel.engine,
                onDismiss = { viewModel.toggleInventory() }
            )
        }

        if (uiState.isPaused) {
            PauseMenuDialog(
                onDismiss = { viewModel.togglePause() },
                onQuickSave = { viewModel.quickSaveGame() },
                onOpenSettings = { showSettingsInGame = true },
                onReturnToMainMenu = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
            )
        }

        if (showSettingsInGame) {
            SettingsDialog(
                currentRamp = uiState.asciiRamp,
                currentAnsi = uiState.ansiMode,
                currentFilter = uiState.filterMode,
                currentSens = uiState.touchSensitivity,
                soundVol = uiState.soundVolume,
                musicVol = uiState.musicVolume,
                onSaveSettings = { ramp, ansi, filter, sens, sVol, mVol ->
                    viewModel.updateSettings(ramp, ansi, filter, sens, sVol, mVol)
                },
                onDismiss = { showSettingsInGame = false }
            )
        }
    }
}
