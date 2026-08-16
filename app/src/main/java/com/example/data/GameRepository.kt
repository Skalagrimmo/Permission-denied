package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GameRepository private constructor(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "permission_denied.db"
    ).fallbackToDestructiveMigration().build()

    private val saveDao = db.gameSaveDao()
    private val scoreDao = db.highScoreDao()
    private val progressDao = db.campaignProgressDao()

    val allSaves: Flow<List<GameSaveEntity>> = saveDao.getAllSaves()
    val topScores: Flow<List<HighScoreEntity>> = scoreDao.getTopScores()
    val campaignProgress: Flow<CampaignProgressEntity?> = progressDao.getProgress()

    suspend fun getSaveByKey(key: String): GameSaveEntity? = withContext(Dispatchers.IO) {
        saveDao.getSaveByKey(key)
    }

    suspend fun getLatestSave(): GameSaveEntity? = withContext(Dispatchers.IO) {
        saveDao.getLatestSave()
    }

    suspend fun saveGame(save: GameSaveEntity) = withContext(Dispatchers.IO) {
        saveDao.saveGame(save)
    }

    suspend fun deleteSave(key: String) = withContext(Dispatchers.IO) {
        saveDao.deleteSave(key)
    }

    suspend fun insertScore(score: HighScoreEntity): Long = withContext(Dispatchers.IO) {
        scoreDao.insertScore(score)
    }

    suspend fun getCampaignProgressSync(): CampaignProgressEntity = withContext(Dispatchers.IO) {
        progressDao.getProgressSync() ?: CampaignProgressEntity().also {
            progressDao.updateProgress(it)
        }
    }

    suspend fun updateCampaignProgress(progress: CampaignProgressEntity) = withContext(Dispatchers.IO) {
        progressDao.updateProgress(progress)
    }

    companion object {
        @Volatile
        private var INSTANCE: GameRepository? = null

        fun getInstance(context: Context): GameRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GameRepository(context).also { INSTANCE = it }
            }
        }
    }
}
