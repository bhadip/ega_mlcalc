package com.example.data

import kotlinx.coroutines.flow.Flow

class MarginRepository(private val marginDao: MarginDao) {
    val allCalculations: Flow<List<MarginCalculation>> = marginDao.getAllCalculations()

    suspend fun insert(calculation: MarginCalculation): Long {
        return marginDao.insertCalculation(calculation)
    }

    suspend fun delete(calculation: MarginCalculation) {
        marginDao.deleteCalculation(calculation)
    }

    suspend fun clearAll() {
        marginDao.clearAllCalculations()
    }
}
