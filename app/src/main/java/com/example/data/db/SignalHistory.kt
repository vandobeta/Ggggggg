package com.example.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "signal_history")
data class SignalHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val signalId: String,
    val symbolCode: String,
    val displayName: String,
    val riskProfile: String,
    val contractType: String,
    val barrierValue: Int,
    val payoutPct: String,
    val message: String,
    val winDigits: String, // comma-separated candidates
    var isWin: Boolean? = null, // null = PENDING, true = WIN, false = LOSS
    var exitDigit: Int? = null, // null if pending
    var ticksObserved: Int = 0,
    var targetTicks: Int = 2 // 2 ticks contract duration
)

@Dao
interface SignalHistoryDao {
    @Query("SELECT * FROM signal_history ORDER BY timestamp DESC")
    fun getAllSignalsFlow(): Flow<List<SignalHistory>>

    @Query("SELECT * FROM signal_history WHERE isWin IS NULL")
    suspend fun getPendingSignals(): List<SignalHistory>

    @Insert
    suspend fun insertSignal(signal: SignalHistory): Long

    @Update
    suspend fun updateSignal(signal: SignalHistory)

    @Query("DELETE FROM signal_history")
    suspend fun clearHistory()
}
