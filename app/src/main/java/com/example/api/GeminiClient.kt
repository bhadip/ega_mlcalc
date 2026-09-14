package com.example.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import kotlin.coroutines.resume

// --- API Data Models ---


@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = "application/json",
    val responseSchema: ResponseSchema? = null,
    val temperature: Float? = 0.1f
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    val type: String, // "OBJECT", "ARRAY", etc.
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null,
    val items: ResponseSchema? = null
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
    val type: String, // "STRING", "NUMBER", "INTEGER", "BOOLEAN", "ARRAY", "OBJECT"
    val description: String? = null,
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null,
    val items: SchemaProperty? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Parsed Trade Schema ---

@JsonClass(generateAdapter = true)
data class ParsedPosition(
    val symbol: String? = null,
    val positionType: String? = null, // "BUY" or "SELL"
    val openPrice: Double? = null,
    val currentPrice: Double? = null,
    val positionPnl: Double? = null
)

@JsonClass(generateAdapter = true)
data class ParsedPositionJson(
    val symbol: String? = null,
    val positionType: String? = null,
    val openPrice: String? = null,
    val currentPrice: String? = null,
    val positionPnl: String? = null
)

@JsonClass(generateAdapter = true)
data class ParsedTrade(
    val symbol: String? = null,
    val positionType: String? = null, // "BUY" or "SELL"
    val openPrice: Double? = null,
    val currentPrice: Double? = null,
    val positionPnl: Double? = null,
    val equity: Double? = null,
    val margin: Double? = null,
    val marginLevel: Double? = null,
    val positions: List<ParsedPosition>? = null
)

@JsonClass(generateAdapter = true)
data class ParsedTradeJson(
    val symbol: String? = null,
    val positionType: String? = null, // "BUY" or "SELL"
    val openPrice: String? = null,
    val currentPrice: String? = null,
    val positionPnl: String? = null,
    val equity: String? = null,
    val margin: String? = null,
    val marginLevel: String? = null,
    val positions: List<ParsedPositionJson>? = null
)

private fun parseDoubleClean(value: String?): Double? {
    if (value == null) return null
    return try {
        val cleaned = value.replace("\\s".toRegex(), "")
            .replace(",", "")
            .replace("—", "-")
            .replace("–", "-")
            .replace("[^\\d\\.\\-]".toRegex(), "")
        cleaned.toDoubleOrNull()
    } catch (e: Exception) {
        null
    }
}

// --- Retrofit Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Retrofit Client & Helper ---

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private suspend fun parseWithMlKit(bitmap: Bitmap): ParsedTrade? = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    try {
                        // Reconstruct horizontal lines by clustering text elements based on their vertical coordinates
                        val elementsList = mutableListOf<OcrElement>()
                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                for (element in line.elements) {
                                    val rect = element.boundingBox
                                    if (rect != null) {
                                        elementsList.add(
                                            OcrElement(
                                                text = element.text,
                                                left = rect.left,
                                                top = rect.top,
                                                right = rect.right,
                                                bottom = rect.bottom
                                            )
                                        )
                                    } else {
                                        elementsList.add(
                                            OcrElement(
                                                text = element.text,
                                                left = 0,
                                                top = 0,
                                                right = 0,
                                                bottom = 0
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        val groupedLinesList = mutableListOf<MutableList<OcrElement>>()
                        val sortedElements = elementsList.sortedWith(compareBy({ it.top }, { it.left }))

                        for (element in sortedElements) {
                            var placed = false
                            for (lineGroup in groupedLinesList) {
                                val rep = lineGroup.first()
                                val elementHeight = element.bottom - element.top
                                val repHeight = rep.bottom - rep.top
                                val avgHeight = (elementHeight + repHeight) / 2.0
                                val verticalDistance = Math.abs((element.top + element.bottom) / 2 - (rep.top + rep.bottom) / 2)
                                
                                // Group together if vertical distance of centers is less than 70% of average element height
                                if (verticalDistance < avgHeight * 0.7) {
                                    lineGroup.add(element)
                                    placed = true
                                    break
                                }
                            }
                            if (!placed) {
                                groupedLinesList.add(mutableListOf(element))
                            }
                        }

                        // Sort each line horizontally from left to right and join elements
                        val reconstructedLines = groupedLinesList.map { lineGroup ->
                            lineGroup.sortedBy { it.left }.joinToString(" ") { it.text }
                        }

                        val lines = if (reconstructedLines.size > 2) reconstructedLines else visionText.textBlocks.flatMap { it.lines }.map { it.text }
                        Log.d("GeminiClient", "Local ML Kit OCR reconstructed lines: $lines")

                        var balance: Double? = null
                        var equity: Double? = null
                        var freeMargin: Double? = null
                        var marginLevel: Double? = null
                        var margin: Double? = null

                        val balanceRegex = """(?i)Balance\s*:?\s*([\d\s,\.-]+)""".toRegex()
                        val equityRegex = """(?i)Equity\s*:?\s*([\d\s,\.-]+)""".toRegex()
                        val freeMarginRegex = """(?i)Free\s+margin\s*:?\s*([\d\s,\.-]+)""".toRegex()
                        val marginLevelRegex = """(?i)Margin\s+Level\s*(?:\(%\))?\s*:?\s*([\d\s,\.-]+)""".toRegex()
                        val marginRegex = """(?i)Margin\s*:?\s*([\d\s,\.-]+)""".toRegex()

                        for (line in lines) {
                            val cleanLine = line.trim()
                            if (cleanLine.contains("Balance", ignoreCase = true)) {
                                balance = balanceRegex.find(cleanLine)?.groupValues?.getOrNull(1)?.let { parseDoubleClean(it) } ?: balance
                            } else if (cleanLine.contains("Equity", ignoreCase = true)) {
                                equity = equityRegex.find(cleanLine)?.groupValues?.getOrNull(1)?.let { parseDoubleClean(it) } ?: equity
                            } else if (cleanLine.contains("Free margin", ignoreCase = true) || cleanLine.contains("FreeMargin", ignoreCase = true)) {
                                freeMargin = freeMarginRegex.find(cleanLine)?.groupValues?.getOrNull(1)?.let { parseDoubleClean(it) } ?: freeMargin
                            } else if (cleanLine.contains("Margin Level", ignoreCase = true) || cleanLine.contains("MarginLevel", ignoreCase = true)) {
                                marginLevel = marginLevelRegex.find(cleanLine)?.groupValues?.getOrNull(1)?.let { parseDoubleClean(it) } ?: marginLevel
                            } else if (cleanLine.contains("Margin", ignoreCase = true)) {
                                if (!cleanLine.contains("Free", ignoreCase = true) && !cleanLine.contains("Level", ignoreCase = true)) {
                                    margin = marginRegex.find(cleanLine)?.groupValues?.getOrNull(1)?.let { parseDoubleClean(it) } ?: margin
                                }
                            }
                        }

                        // Parse positions
                        val parsedPositions = mutableListOf<ParsedPosition>()
                        var i = 0
                        while (i < lines.size) {
                            val line = lines[i].trim()
                            // Match symbols (e.g. XAUUSD, EURUSD, buy/sell)
                            val positionHeaderRegex = """(?i)([A-Z0-9\.\-#]{3,10})[\s,]+(buy|sell)\s*([\d\.]+)?""".toRegex()
                            val match = positionHeaderRegex.find(line)
                            if (match != null) {
                                val symbol = match.groupValues[1].uppercase()
                                val type = match.groupValues[2].uppercase()

                                var openPrice: Double? = null
                                var currentPrice: Double? = null
                                var profit: Double? = null

                                // Scan next few lines
                                for (j in 1..4) {
                                    if (i + j >= lines.size) break
                                    val nextLine = lines[i + j].trim()
                                    if (positionHeaderRegex.containsMatchIn(nextLine)) {
                                        break
                                    }

                                    val priceTransitionRegex = """([\d\s,\.]+)(?:->|→|to)\s*([\d\s,\.\-]+)""".toRegex()
                                    val priceMatch = priceTransitionRegex.find(nextLine)
                                    if (priceMatch != null) {
                                        val part1 = priceMatch.groupValues[1].trim()
                                        val part2 = priceMatch.groupValues[2].trim()

                                        val profitIndex = part2.indexOfLast { it == '-' || it == '+' }
                                        if (profitIndex > 0) {
                                            val currentPriceStr = part2.substring(0, profitIndex).trim()
                                            val profitStr = part2.substring(profitIndex).trim()
                                            openPrice = parseDoubleClean(part1)
                                            currentPrice = parseDoubleClean(currentPriceStr)
                                            profit = parseDoubleClean(profitStr)
                                        } else {
                                            openPrice = parseDoubleClean(part1)
                                            currentPrice = parseDoubleClean(part2)
                                        }
                                    } else {
                                        val profitRegex = """^[-+]?\s*[\d\s,\.]+$""".toRegex()
                                        if (profitRegex.matches(nextLine)) {
                                            val pVal = parseDoubleClean(nextLine)
                                            if (pVal != null && profit == null) {
                                                profit = pVal
                                            }
                                        }
                                    }
                                }

                                parsedPositions.add(
                                    ParsedPosition(
                                        symbol = symbol,
                                        positionType = type,
                                        openPrice = openPrice,
                                        currentPrice = currentPrice,
                                        positionPnl = profit
                                    )
                                )
                            }
                            i++
                        }

                        // We consider local parsing successful if we have at least equity or margin
                        if (equity != null || margin != null || marginLevel != null) {
                            val firstPos = parsedPositions.firstOrNull()
                            val calculatedMarginLevel = marginLevel ?: (if (equity != null && margin != null && margin > 0) (equity / margin * 100.0) else null)
                            val result = ParsedTrade(
                                symbol = firstPos?.symbol ?: "EURUSD",
                                positionType = firstPos?.positionType ?: "BUY",
                                openPrice = firstPos?.openPrice,
                                currentPrice = firstPos?.currentPrice,
                                positionPnl = firstPos?.positionPnl,
                                equity = equity,
                                margin = margin,
                                marginLevel = calculatedMarginLevel,
                                positions = parsedPositions
                            )
                            Log.d("GeminiClient", "Local ML Kit OCR Success: $result")
                            continuation.resume(result)
                        } else {
                            Log.d("GeminiClient", "Local ML Kit OCR did not extract sufficient trading parameters, falling back.")
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        Log.e("GeminiClient", "Exception parsing ML Kit text output", e)
                        continuation.resume(null)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("GeminiClient", "ML Kit text recognition failed", e)
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            Log.e("GeminiClient", "Failed to initialize ML Kit text recognition", e)
            continuation.resume(null)
        }
    }

    suspend fun parseTradeScreenshot(bitmap: Bitmap): ParsedTrade? = withContext(Dispatchers.IO) {
        // 1. Try local ML Kit OCR first
        val localResult = parseWithMlKit(bitmap)
        if (localResult != null) {
            return@withContext localResult
        }

        // 2. Fall back to Gemini API
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured. Please add it to the Secrets panel.")
        }

        val positionProperties = mapOf(
            "symbol" to SchemaProperty("STRING", "The currency pair or asset name (e.g. EURUSD, XAUUSD)"),
            "positionType" to SchemaProperty("STRING", "Must be exactly 'BUY' or 'SELL'"),
            "openPrice" to SchemaProperty("STRING", "The opening/entry price exactly as shown (e.g. '1.1230' or '4 152.93')"),
            "currentPrice" to SchemaProperty("STRING", "The current price exactly as shown (e.g. '1.1245' or '4 152.99')"),
            "positionPnl" to SchemaProperty("STRING", "The floating profit or loss of this position exactly as shown (e.g. '0.06' or '-120.50')")
        )

        // Define a robust schema where all numeric extraction fields are represented as strings
        // This avoids strict JSON schema verification errors if spaces or formatting symbols are present
        val properties = mapOf(
            "symbol" to SchemaProperty("STRING", "The currency pair or asset name of the primary/first position (e.g. EURUSD, XAUUSD, Gold)"),
            "positionType" to SchemaProperty("STRING", "Must be exactly 'BUY' or 'SELL' for the primary/first position"),
            "openPrice" to SchemaProperty("STRING", "The opening/entry price of the primary/first position exactly as shown"),
            "currentPrice" to SchemaProperty("STRING", "The current market price of the primary/first position exactly as shown"),
            "positionPnl" to SchemaProperty("STRING", "The floating profit or loss of the primary/first position exactly as shown"),
            "equity" to SchemaProperty("STRING", "The current account equity exactly as shown (e.g., '1 925.37')"),
            "margin" to SchemaProperty("STRING", "The current used margin of the account exactly as shown (e.g., '16.61')"),
            "marginLevel" to SchemaProperty("STRING", "The Margin Level (%) exactly as shown on the screen (e.g., '11590.43' or '11590.43%')"),
            "positions" to SchemaProperty(
                type = "ARRAY",
                description = "All active positions displayed in the screenshot under Positions/Trades",
                items = SchemaProperty(
                    type = "OBJECT",
                    properties = positionProperties,
                    required = listOf("symbol", "positionType", "openPrice", "currentPrice", "positionPnl")
                )
            )
        )

        val schema = ResponseSchema(
            type = "OBJECT",
            properties = properties,
            required = listOf("equity", "margin", "marginLevel")
        )

        val promptText = "Analyze the provided CFD trade margin screenshot and extract the details for all active open positions and account metrics matching the requested schema. Ensure to read spaces in numbers correctly (e.g. '1 925.37')."

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = promptText),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                responseSchema = schema,
                temperature = 0.1f
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are an expert CFD trading assistant. Your task is to perform optical character recognition (OCR) " +
                                "on the trade screenshot and extract exact values for the schema as literal strings. " +
                                "Identify all open/active trade positions under the 'Positions' or 'Trades' list on the screen and put them in the 'positions' array. " +
                                "Return values exactly as they are shown on the screen (including any spaces, commas, or punctuation). " +
                                "Ensure positionType is either BUY or SELL. If unsure or missing, use reasonable defaults like Gold or EURUSD."
                    )
                )
            )
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText != null) {
                val rawParsed = moshi.adapter(ParsedTradeJson::class.java).fromJson(responseText)
                if (rawParsed != null) {
                    val parsedPositions = rawParsed.positions?.map { p ->
                        ParsedPosition(
                            symbol = p.symbol,
                            positionType = p.positionType,
                            openPrice = parseDoubleClean(p.openPrice),
                            currentPrice = parseDoubleClean(p.currentPrice),
                            positionPnl = parseDoubleClean(p.positionPnl)
                        )
                    } ?: emptyList()

                    val firstPos = parsedPositions.firstOrNull()

                    ParsedTrade(
                        symbol = firstPos?.symbol ?: rawParsed.symbol,
                        positionType = firstPos?.positionType ?: rawParsed.positionType,
                        openPrice = firstPos?.openPrice ?: parseDoubleClean(rawParsed.openPrice),
                        currentPrice = firstPos?.currentPrice ?: parseDoubleClean(rawParsed.currentPrice),
                        positionPnl = firstPos?.positionPnl ?: parseDoubleClean(rawParsed.positionPnl),
                        equity = parseDoubleClean(rawParsed.equity),
                        margin = parseDoubleClean(rawParsed.margin),
                        marginLevel = parseDoubleClean(rawParsed.marginLevel),
                        positions = parsedPositions
                    )
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

private data class OcrElement(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

