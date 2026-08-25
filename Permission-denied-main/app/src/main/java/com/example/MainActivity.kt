package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF04060A)
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    val topScores by viewModel.topScores.collectAsState(initial = emptyList())
                    var showLeaderboard by remember { mutableStateOf(false) }
                    var showSettings by remember { mutableStateOf(false) }

                    when (uiState.currentScreen) {
                        AppScreen.MAIN_MENU -> {
                            MainMenuScreen(
                                latestSave = uiState.latestSave,
                                onNewGame = { viewModel.navigateTo(AppScreen.DISTRICT_SELECT) },
                                onContinueSave = { save -> viewModel.loadSavedGame(save) },
                                onOpenShowcase = {
                                    viewModel.toggleShowcaseMode()
                                    viewModel.navigateTo(AppScreen.PLAYING)
                                },
                                onOpenLeaderboard = { showLeaderboard = true },
                                onOpenSettings = { showSettings = true }
                            )
                        }
                        AppScreen.DISTRICT_SELECT -> {
                            DistrictSelectScreen(
                                maxUnlocked = uiState.maxUnlockedDistrict,
                                onDistrictSelected = { dist -> viewModel.selectDistrict(dist) },
                                onBack = { viewModel.navigateTo(AppScreen.MAIN_MENU) }
                            )
                        }
                        AppScreen.AUGMENTATION_SELECT -> {
                            AugmentationsSelectScreen(
                                district = uiState.selectedDistrict,
                                selectedAugmentations = uiState.selectedAugmentations,
                                onToggleAugmentation = { aug -> viewModel.toggleAugmentationSelection(aug) },
                                onStartMission = { viewModel.startMission() },
                                onBack = { viewModel.navigateTo(AppScreen.DISTRICT_SELECT) }
                            )
                        }
                        AppScreen.PLAYING -> {
                            GameScreen(
                                viewModel = viewModel,
                                uiState = uiState
                            )
                        }
                        AppScreen.MISSION_REPORT -> {
                            MissionReportScreen(
                                report = uiState.currentMissionReport,
                                onChooseEnding = { choice -> viewModel.onEndingChosen(choice) },
                                onContinue = { viewModel.navigateTo(AppScreen.DISTRICT_SELECT) }
                            )
                        }
                        else -> {}
                    }

                    if (showLeaderboard) {
                        HighScoresDialog(
                            scores = topScores,
                            onDismiss = { showLeaderboard = false }
                        )
                    }

                    if (showSettings) {
                        SettingsDialog(
                            currentRamp = uiState.asciiRamp,
                            currentAnsi = uiState.ansiMode,
                            currentFilter = uiState.filterMode,
                            currentRenderer = uiState.rendererType,
                            currentSens = uiState.touchSensitivity,
                            soundVol = uiState.soundVolume,
                            musicVol = uiState.musicVolume,
                            onSaveSettings = { ramp, ansi, filter, renderer, sens, sVol, mVol ->
                                viewModel.updateSettings(ramp, ansi, filter, renderer, sens, sVol, mVol)
                            },
                            onDismiss = { showSettings = false }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.audioEngine.stopBgm()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.uiState.value.currentScreen == AppScreen.PLAYING) {
            viewModel.audioEngine.startBgm()
        }
    }
}
