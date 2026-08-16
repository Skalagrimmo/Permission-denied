package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.GameAudioEngine
import com.example.data.*
import com.example.engine.GameEngine
import com.example.engine.GameHapticsEngine
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_MENU,
    DISTRICT_SELECT,
    AUGMENTATION_SELECT,
    PLAYING,
    MISSION_REPORT,
    HIGH_SCORES,
    SETTINGS
}

data class GameUiState(
    val currentScreen: AppScreen = AppScreen.MAIN_MENU,
    val selectedDistrict: DistrictId = DistrictId.DISTRICT_01,
    val selectedAugmentations: Set<AugmentationType> = setOf(AugmentationType.ACTIVE_CAMO, AugmentationType.CYBER_OPTICS),
    val isPaused: Boolean = false,
    val isInventoryOpen: Boolean = false,
    val isHackingOpen: Boolean = false,
    val asciiRamp: String = "cyber",
    val ansiMode: String = "GAME",
    val filterMode: String = "BOX",
    val touchSensitivity: Float = 1.0f,
    val soundVolume: Float = 0.8f,
    val musicVolume: Float = 0.6f,
    val maxUnlockedDistrict: Int = 1,
    val latestSave: GameSaveEntity? = null,
    val currentMissionReport: MissionReport? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository.getInstance(application)
    val audioEngine = GameAudioEngine()
    val haptics = GameHapticsEngine.getInstance(application)
    val engine = GameEngine(audioEngine, haptics)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val topScores = repository.topScores
    val allSaves = repository.allSaves

    init {
        loadCampaignProgress()
        refreshLatestSave()
    }

    private fun loadCampaignProgress() {
        viewModelScope.launch {
            val progress = repository.getCampaignProgressSync()
            audioEngine.soundVolume = progress.soundVolume
            audioEngine.musicVolume = progress.musicVolume
            _uiState.update {
                it.copy(
                    maxUnlockedDistrict = progress.maxUnlockedDistrict,
                    asciiRamp = progress.asciiRamp,
                    ansiMode = progress.ansiMode,
                    filterMode = progress.resolutionFilter,
                    touchSensitivity = progress.touchSensitivity,
                    soundVolume = progress.soundVolume,
                    musicVolume = progress.musicVolume
                )
            }
        }
    }

    fun refreshLatestSave() {
        viewModelScope.launch {
            val latest = repository.getLatestSave()
            _uiState.update { it.copy(latestSave = latest) }
        }
    }

    fun navigateTo(screen: AppScreen) {
        audioEngine.playUiClick()
        if (screen == AppScreen.PLAYING) {
            audioEngine.startBgm()
        } else if (screen == AppScreen.MAIN_MENU) {
            audioEngine.stopBgm()
        }
        _uiState.update { it.copy(currentScreen = screen, isPaused = false, isInventoryOpen = false) }
    }

    fun selectDistrict(district: DistrictId) {
        audioEngine.playUiClick()
        _uiState.update { it.copy(selectedDistrict = district) }
        navigateTo(AppScreen.AUGMENTATION_SELECT)
    }

    fun toggleAugmentationSelection(aug: AugmentationType) {
        audioEngine.playUiClick()
        _uiState.update { state ->
            val current = state.selectedAugmentations.toMutableSet()
            if (current.contains(aug)) {
                if (current.size > 1) current.remove(aug)
            } else {
                if (current.size < 3) current.add(aug)
            }
            state.copy(selectedAugmentations = current)
        }
    }

    fun startMission() {
        audioEngine.playUiClick()
        val district = _uiState.value.selectedDistrict
        engine.loadDistrict(district)
        engine.activeAugmentations.clear()
        engine.activeAugmentations.addAll(_uiState.value.selectedAugmentations)

        audioEngine.startBgm()
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.PLAYING,
                isPaused = false,
                isInventoryOpen = false,
                isHackingOpen = false,
                currentMissionReport = null
            )
        }
        quickSaveGame("autosave_start")
    }

    fun togglePause() {
        audioEngine.playUiClick()
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun toggleInventory() {
        audioEngine.playUiClick()
        _uiState.update { it.copy(isInventoryOpen = !it.isInventoryOpen) }
    }

    fun closeHackingModal() {
        engine.activeHacking = null
        _uiState.update { it.copy(isHackingOpen = false) }
    }

    fun onEndingChosen(choice: EndingChoice) {
        engine.finishMission(choice)
        val report = engine.missionReport
        _uiState.update { it.copy(currentMissionReport = report, currentScreen = AppScreen.MISSION_REPORT) }

        viewModelScope.launch {
            report?.let { rep ->
                repository.insertScore(
                    HighScoreEntity(
                        playerName = "GHOST_OPERATIVE",
                        districtLevel = rep.districtLevel,
                        districtName = rep.districtName,
                        finalScore = rep.score,
                        stealthRating = rep.stealthRating,
                        endingChoice = choice.name,
                        timeElapsedSeconds = rep.timeElapsedSeconds,
                        alarmsTriggered = rep.alarmsTriggered
                    )
                )

                val progress = repository.getCampaignProgressSync()
                val nextDist = kotlin.math.max(progress.maxUnlockedDistrict, kotlin.math.min(5, rep.districtLevel + 1))
                repository.updateCampaignProgress(progress.copy(maxUnlockedDistrict = nextDist))
                _uiState.update { it.copy(maxUnlockedDistrict = nextDist) }
            }
        }
    }

    fun quickSaveGame(slot: String = "quicksave") {
        viewModelScope.launch {
            val save = GameSaveEntity(
                slotKey = slot,
                saveType = if (slot.startsWith("auto")) "AUTO" else "QUICK",
                districtLevel = engine.world.district.level,
                districtName = engine.world.district.title,
                districtSeed = engine.world.seed,
                elapsedSeconds = engine.elapsedSeconds,
                playerHealth = engine.playerHealth,
                playerArmor = engine.playerArmor,
                playerEnergy = engine.playerEnergy.toInt(),
                playerCredits = engine.playerCredits,
                currentWeaponId = engine.currentWeapon.type.name,
                inventoryJson = engine.inventory.joinToString(";") { "${it.type}:${it.count}" },
                augmentationsJson = engine.activeAugmentations.joinToString(",") { it.name },
                evidenceCollectedJson = engine.evidenceCollected.toString(),
                contractsCompletedJson = engine.contractsCompleted.toString(),
                alarmsTriggered = engine.alarmsTriggered,
                ghostIndexState = engine.ghostIndexAcquired.toString(),
                gameStateJson = "${engine.playerX},${engine.playerZ},${engine.playerYaw}"
            )
            repository.saveGame(save)
            refreshLatestSave()
            audioEngine.playHackBeep(true)
        }
    }

    fun loadSavedGame(save: GameSaveEntity) {
        val dist = DistrictId.entries.find { it.level == save.districtLevel } ?: DistrictId.DISTRICT_01
        engine.loadDistrict(dist, save.districtSeed)
        engine.playerHealth = save.playerHealth
        engine.playerArmor = save.playerArmor
        engine.playerEnergy = save.playerEnergy.toFloat()
        engine.playerCredits = save.playerCredits
        engine.alarmsTriggered = save.alarmsTriggered
        engine.ghostIndexAcquired = save.ghostIndexState.toBoolean()
        engine.evidenceCollected = save.evidenceCollectedJson.toIntOrNull() ?: 0
        engine.contractsCompleted = save.contractsCompletedJson.toIntOrNull() ?: 0
        engine.elapsedSeconds = save.elapsedSeconds

        val coords = save.gameStateJson.split(",")
        if (coords.size >= 3) {
            engine.playerX = coords[0].toFloatOrNull() ?: engine.playerX
            engine.playerZ = coords[1].toFloatOrNull() ?: engine.playerZ
            engine.playerYaw = coords[2].toFloatOrNull() ?: engine.playerYaw
        }

        navigateTo(AppScreen.PLAYING)
    }

    fun updateSettings(
        asciiRamp: String = _uiState.value.asciiRamp,
        ansiMode: String = _uiState.value.ansiMode,
        filterMode: String = _uiState.value.filterMode,
        touchSensitivity: Float = _uiState.value.touchSensitivity,
        soundVol: Float = _uiState.value.soundVolume,
        musicVol: Float = _uiState.value.musicVolume
    ) {
        audioEngine.soundVolume = soundVol
        audioEngine.musicVolume = musicVol

        _uiState.update {
            it.copy(
                asciiRamp = asciiRamp,
                ansiMode = ansiMode,
                filterMode = filterMode,
                touchSensitivity = touchSensitivity,
                soundVolume = soundVol,
                musicVolume = musicVol
            )
        }

        viewModelScope.launch {
            val progress = repository.getCampaignProgressSync()
            repository.updateCampaignProgress(
                progress.copy(
                    asciiRamp = asciiRamp,
                    ansiMode = ansiMode,
                    resolutionFilter = filterMode,
                    touchSensitivity = touchSensitivity,
                    soundVolume = soundVol,
                    musicVolume = musicVol
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
