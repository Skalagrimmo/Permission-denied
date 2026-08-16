package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_saves")
data class GameSaveEntity(
    @PrimaryKey val slotKey: String, // e.g., "quicksave", "autosave_0", "autosave_1", "autosave_2", "manual_1"
    val saveType: String,            // "QUICK", "AUTO", "MANUAL"
    val timestamp: Long = System.currentTimeMillis(),
    val districtLevel: Int,
    val districtName: String,
    val districtSeed: Long,
    val elapsedSeconds: Long,
    val playerHealth: Int,
    val playerArmor: Int,
    val playerEnergy: Int,
    val playerCredits: Int,
    val currentWeaponId: String,
    val inventoryJson: String,
    val augmentationsJson: String,
    val evidenceCollectedJson: String,
    val contractsCompletedJson: String,
    val alarmsTriggered: Int,
    val ghostIndexState: String,
    val gameStateJson: String
)

@Entity(tableName = "high_scores")
data class HighScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val playerName: String,
    val districtLevel: Int,
    val districtName: String,
    val finalScore: Int,
    val stealthRating: String, // "GHOST", "SHADOW", "SPECTRE", "CHAOS"
    val endingChoice: String,  // "LEAK", "SELL", "SURRENDER", "DESTROY"
    val timeElapsedSeconds: Long,
    val alarmsTriggered: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "campaign_progress")
data class CampaignProgressEntity(
    @PrimaryKey val id: Int = 1,
    val maxUnlockedDistrict: Int = 1,
    val totalCreditsEarned: Int = 0,
    val unlockedAugmentationsJson: String = "[]",
    val unlockedEndingsJson: String = "[]",
    val soundVolume: Float = 0.8f,
    val musicVolume: Float = 0.6f,
    val touchSensitivity: Float = 1.0f,
    val asciiRamp: String = "cyber",
    val ansiMode: String = "GAME",
    val resolutionFilter: String = "BOX",
    val targetFps: Int = 60
)

@Dao
interface GameSaveDao {
    @Query("SELECT * FROM game_saves ORDER BY timestamp DESC")
    fun getAllSaves(): Flow<List<GameSaveEntity>>

    @Query("SELECT * FROM game_saves WHERE slotKey = :key LIMIT 1")
    suspend fun getSaveByKey(key: String): GameSaveEntity?

    @Query("SELECT * FROM game_saves ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSave(): GameSaveEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGame(save: GameSaveEntity)

    @Query("DELETE FROM game_saves WHERE slotKey = :key")
    suspend fun deleteSave(key: String)
}

@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores ORDER BY finalScore DESC LIMIT 50")
    fun getTopScores(): Flow<List<HighScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: HighScoreEntity): Long
}

@Dao
interface CampaignProgressDao {
    @Query("SELECT * FROM campaign_progress WHERE id = 1 LIMIT 1")
    fun getProgress(): Flow<CampaignProgressEntity?>

    @Query("SELECT * FROM campaign_progress WHERE id = 1 LIMIT 1")
    suspend fun getProgressSync(): CampaignProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProgress(progress: CampaignProgressEntity)
}

@Database(
    entities = [GameSaveEntity::class, HighScoreEntity::class, CampaignProgressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameSaveDao(): GameSaveDao
    abstract fun highScoreDao(): HighScoreDao
    abstract fun campaignProgressDao(): CampaignProgressDao
}
