package com.example.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tick_occurrences")
data class TickOccurrence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbolCode: String,
    val digit: Int,
    val timestamp: Long
)

@Dao
interface TickOccurrenceDao {
    @Query("SELECT * FROM tick_occurrences ORDER BY timestamp DESC LIMIT 50")
    fun getRecentTicksFlow(): Flow<List<TickOccurrence>>

    @Query("SELECT * FROM tick_occurrences ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTick(): TickOccurrence?

    @Insert
    suspend fun insertTick(tick: TickOccurrence)

    @Query("DELETE FROM tick_occurrences")
    suspend fun clearTicks()
}
