package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trade_history")
data class TradeHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val symbolCode: String,
    val displayName: String,
    val contractType: String, // "UNDER", "OVER", "DIFFERS", "MATCHES"
    val barrierValue: Int,
    val tradeType: String, // "MANUAL" or "AUTOMATED"
    val accountType: String, // "DEMO" or "REAL"
    val stake: Double,
    val entryPrice: Double,
    val exitPrice: Double? = null,
    val entryDigit: Int,
    val exitDigit: Int? = null,
    val profitLoss: Double = 0.0,
    val status: String // "WIN", "LOSS", "PENDING"
)

@Dao
interface TradeHistoryDao {
    @Query("SELECT * FROM trade_history ORDER BY timestamp DESC")
    fun getAllTradesFlow(): Flow<List<TradeHistory>>

    @Query("SELECT * FROM trade_history WHERE status = 'PENDING'")
    suspend fun getPendingResultTrades(): List<TradeHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeHistory): Long

    @Update
    suspend fun updateTrade(trade: TradeHistory)

    @Query("DELETE FROM trade_history")
    suspend fun clearAllTrades()
}
