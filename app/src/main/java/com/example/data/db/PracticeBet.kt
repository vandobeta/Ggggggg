package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "practice_bets")
data class PracticeBet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val signalDescription: String,
    val isWin: Boolean
)

@Dao
interface PracticeBetDao {
    @Query("SELECT * FROM practice_bets ORDER BY timestamp DESC")
    fun getAllBets(): Flow<List<PracticeBet>>

    @Insert
    suspend fun insertBet(bet: PracticeBet)

    @Query("DELETE FROM practice_bets")
    suspend fun clearAllBets()
}
