package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.api.ParsedPosition
import com.example.data.MarginCalculation
import com.example.data.MarginRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MarginViewModel(private val repository: MarginRepository) : ViewModel() {

    // Database flow for past calculations
    val allCalculations: StateFlow<List<MarginCalculation>> = repository.allCalculations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active Input States for adding/editing calculation
    var currentSymbol by mutableStateOf("")
    var currentPositionType by mutableStateOf("BUY") // "BUY" or "SELL"
    var currentOpenPrice by mutableStateOf("")
    var currentCurrentPrice by mutableStateOf("")
    var currentPositionPnl by mutableStateOf("")
    var currentEquity by mutableStateOf("")
    var currentMargin by mutableStateOf("")
    var currentExtractedMarginLevel by mutableStateOf("")

    // Multiple positions list and active selected position index
    var currentPositions by mutableStateOf<List<ParsedPosition>>(emptyList())
    var selectedPositionIndex by mutableStateOf<Int?>(null)

    // Simulation price state (starts as null, then holds the simulated price from the slider)
    var simulatedPriceValue by mutableStateOf<Double?>(null)
    // Simulation equity state (holds simulated equity value from slider)
    var simulatedEquityValue by mutableStateOf<Double?>(null)

    // Moshi JSON adapters for serializing/deserializing positions
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val positionsListAdapter = moshi.adapter<List<ParsedPosition>>(
        Types.newParameterizedType(List::class.java, ParsedPosition::class.java)
    )

    // Screen navigation / layout states
    var isHistoryViewActive by mutableStateOf(false)
    var isInputPanelOpen by mutableStateOf(false)
    var selectedHistoricItem by mutableStateOf<MarginCalculation?>(null)

    // OCR / Gemini states
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var selectedImageBitmap by mutableStateOf<Bitmap?>(null)
    var localImageUri by mutableStateOf<String?>(null)

    // Real-time calculated prices (Dynamic)
    val price100: Double
        get() = calculateTargetPrice(100.0)

    val price30: Double
        get() = calculateTargetPrice(30.0)

    fun resetInputs() {
        currentSymbol = ""
        currentPositionType = "BUY"
        currentOpenPrice = ""
        currentCurrentPrice = ""
        currentPositionPnl = ""
        currentEquity = ""
        currentMargin = ""
        currentExtractedMarginLevel = ""
        currentPositions = emptyList()
        selectedPositionIndex = null
        simulatedPriceValue = null
        simulatedEquityValue = null
        selectedImageBitmap = null
        localImageUri = null
        errorMessage = null
        isLoading = false
    }

    fun addManualPosition(position: ParsedPosition) {
        currentPositions = currentPositions + position
        if (selectedPositionIndex == null && currentPositions.isNotEmpty()) {
            selectPosition(0)
        }
        recalculateEquityFromPositions()
    }

    fun removePosition(index: Int) {
        if (index in currentPositions.indices) {
            val newList = currentPositions.toMutableList()
            newList.removeAt(index)
            currentPositions = newList
            if (currentPositions.isEmpty()) {
                selectedPositionIndex = null
                currentSymbol = ""
                currentOpenPrice = ""
                currentCurrentPrice = ""
                currentPositionPnl = ""
                simulatedPriceValue = null
            } else {
                val nextIndex = if ((selectedPositionIndex ?: 0) >= currentPositions.size) currentPositions.size - 1 else selectedPositionIndex ?: 0
                selectPosition(nextIndex)
            }
            recalculateEquityFromPositions()
        }
    }

    fun selectPosition(index: Int) {
        if (index in currentPositions.indices) {
            selectedPositionIndex = index
            val pos = currentPositions[index]
            currentSymbol = pos.symbol ?: ""
            currentPositionType = pos.positionType ?: "BUY"
            currentOpenPrice = pos.openPrice?.toString() ?: ""
            currentCurrentPrice = pos.currentPrice?.toString() ?: ""
            currentPositionPnl = pos.positionPnl?.toString() ?: ""
            simulatedPriceValue = pos.currentPrice
        }
    }

    fun selectHistoricItem(item: MarginCalculation?) {
        selectedHistoricItem = item
        if (item != null) {
            currentSymbol = item.symbol
            currentPositionType = item.positionType
            currentOpenPrice = item.openPrice.toString()
            currentCurrentPrice = item.currentPrice.toString()
            currentPositionPnl = item.positionPnl.toString()
            currentEquity = item.equity.toString()
            currentMargin = item.margin.toString()
            currentExtractedMarginLevel = item.extractedMarginLevel?.toString() ?: ""
            simulatedPriceValue = item.currentPrice
            simulatedEquityValue = item.equity

            // Deserialize positionsJson if present
            currentPositions = try {
                if (!item.positionsJson.isNullOrEmpty()) {
                    positionsListAdapter.fromJson(item.positionsJson) ?: emptyList()
                } else {
                    listOf(
                        ParsedPosition(
                            symbol = item.symbol,
                            positionType = item.positionType,
                            openPrice = item.openPrice,
                            currentPrice = item.currentPrice,
                            positionPnl = item.positionPnl
                        )
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
            if (currentPositions.isNotEmpty()) {
                selectedPositionIndex = 0
            } else {
                selectedPositionIndex = null
            }
        } else {
            resetInputs()
        }
    }

    private fun recalculateEquityFromPositions() {
        // Automatically sum up position PnLs if needed to suggest equity adjustments
    }

    fun parseImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val bitmap = loadAndOptimizeBitmap(context, uri)
                if (bitmap != null) {
                    selectedImageBitmap = bitmap
                    // Save image locally to app folder so we can reference it persistently
                    val savedPath = saveBitmapToInternal(context, bitmap)
                    localImageUri = savedPath

                    // Call Gemini API
                    val parsed = GeminiClient.parseTradeScreenshot(bitmap)
                    if (parsed != null) {
                        currentSymbol = parsed.symbol ?: "EURUSD"
                        currentPositionType = parsed.positionType ?: "BUY"
                        currentOpenPrice = parsed.openPrice?.toString() ?: ""
                        currentCurrentPrice = parsed.currentPrice?.toString() ?: ""
                        currentPositionPnl = parsed.positionPnl?.toString() ?: ""
                        currentEquity = parsed.equity?.toString() ?: ""
                        currentMargin = parsed.margin?.toString() ?: ""
                        currentExtractedMarginLevel = parsed.marginLevel?.toString() ?: ""
                        simulatedPriceValue = parsed.currentPrice
                        simulatedEquityValue = parsed.equity

                        // Set list of active positions
                        currentPositions = parsed.positions ?: emptyList()
                        if (currentPositions.isNotEmpty()) {
                            selectedPositionIndex = 0
                        } else {
                            // Synthesize a single position if positions list is empty
                            currentPositions = listOf(
                                ParsedPosition(
                                    symbol = parsed.symbol,
                                    positionType = parsed.positionType,
                                    openPrice = parsed.openPrice,
                                    currentPrice = parsed.currentPrice,
                                    positionPnl = parsed.positionPnl
                                )
                            )
                            selectedPositionIndex = 0
                        }
                    } else {
                        errorMessage = "Failed to extract text. You can enter values manually."
                    }
                } else {
                    errorMessage = "Failed to load image."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error scanning image: ${e.localizedMessage ?: e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun saveCalculation() {
        val symbol = currentSymbol.trim().ifEmpty { "Asset" }
        val open = currentOpenPrice.toDoubleOrNull() ?: 0.0
        val current = currentCurrentPrice.toDoubleOrNull() ?: 0.0
        val pnl = currentPositionPnl.toDoubleOrNull() ?: 0.0
        val eq = currentEquity.toDoubleOrNull() ?: 0.0
        val marg = currentMargin.toDoubleOrNull() ?: 0.0
        val extMarginLvl = currentExtractedMarginLevel.toDoubleOrNull()

        // Serialize currentPositions
        val pJson = try {
            positionsListAdapter.toJson(currentPositions)
        } catch (e: Exception) {
            null
        }

        val item = MarginCalculation(
            symbol = symbol,
            positionType = currentPositionType,
            openPrice = open,
            currentPrice = current,
            positionPnl = pnl,
            equity = eq,
            margin = marg,
            price100 = calculateTargetPrice(100.0, marg, eq, current, open, pnl, currentPositionType),
            price30 = calculateTargetPrice(30.0, marg, eq, current, open, pnl, currentPositionType),
            imageUri = localImageUri,
            extractedMarginLevel = extMarginLvl,
            positionsJson = pJson
        )

        viewModelScope.launch {
            repository.insert(item)
            isInputPanelOpen = false
            resetInputs()
        }
    }

    fun resetHistory() {
        viewModelScope.launch {
            repository.clearAll()
            selectedHistoricItem = null
        }
    }

    fun deleteItem(item: MarginCalculation) {
        viewModelScope.launch {
            repository.delete(item)
            if (selectedHistoricItem?.id == item.id) {
                selectedHistoricItem = null
            }
        }
    }

    // Dynamic price formula calculations (used inside the view model dynamically)
    private fun calculateTargetPrice(targetPercent: Double): Double {
        val marg = currentMargin.toDoubleOrNull() ?: return 0.0
        val eq = currentEquity.toDoubleOrNull() ?: return 0.0
        val current = currentCurrentPrice.toDoubleOrNull() ?: return 0.0
        val open = currentOpenPrice.toDoubleOrNull() ?: return 0.0
        val pnl = currentPositionPnl.toDoubleOrNull() ?: return 0.0
        return calculateTargetPrice(targetPercent, marg, eq, current, open, pnl, currentPositionType)
    }

    private fun calculateTargetPrice(
        targetPercent: Double,
        margin: Double,
        equity: Double,
        currentPrice: Double,
        openPrice: Double,
        positionPnl: Double,
        positionType: String
    ): Double {
        if (margin <= 0.0) return 0.0
        val targetEquity = (targetPercent / 100.0) * margin
        val deltaEquity = targetEquity - equity
        val priceDiff = currentPrice - openPrice

        if (positionPnl == 0.0 || priceDiff == 0.0) {
            return currentPrice
        }

        val kSigned = positionPnl / priceDiff
        val deltaPrice = deltaEquity / kSigned
        val targetPrice = currentPrice + deltaPrice

        return if (targetPrice < 0.0) 0.0 else targetPrice
    }

    private fun loadAndOptimizeBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Scale down if image is huge to avoid OutOfMemory error and reduce Gemini API payload
            var scale = 1
            val maxDimension = 1024
            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                scale = Math.pow(
                    2.0,
                    Math.round(
                        Math.log(maxDimension.toDouble() / Math.max(options.outWidth, options.outHeight).toDouble()) / Math.log(0.5)
                    ).toDouble()
                ).toInt()
            }

            val scaleOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val scaledInputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(scaledInputStream, null, scaleOptions)
            scaledInputStream?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapToInternal(context: Context, bitmap: Bitmap): String? {
        return try {
            val file = File(context.filesDir, "screenshot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

class MarginViewModelFactory(private val repository: MarginRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
