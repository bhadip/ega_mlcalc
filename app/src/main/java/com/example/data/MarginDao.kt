package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MarginDao {
    @Query("SELECT * FROM margin_calculations ORDER BY timestamp DESC")
    fun getAllCalculations(): Flow<List<MarginCalculation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calculation: MarginCalculation): Long

    @Delete
    suspend fun deleteCalculation(calculation: MarginCalculation)

    @Query("DELETE FROM margin_calculations")
    suspend fun clearAllCalculations()
}
