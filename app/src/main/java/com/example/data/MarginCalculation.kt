package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "margin_calculations")
data class MarginCalculation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val symbol: String,
    val positionType: String, // "BUY" or "SELL"
    val openPrice: Double,
    val currentPrice: Double,
    val positionPnl: Double,
    val equity: Double,
    val margin: Double,
    val price100: Double,
    val price30: Double,
    val imageUri: String? = null, // local uri/path of captured/loaded image
    val extractedMarginLevel: Double? = null,
    val positionsJson: String? = null // JSON string list of ParsedPosition
)
