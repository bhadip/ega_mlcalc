package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  private fun parseDoubleClean(value: String?): Double? {
    if (value == null) return null
    return try {
      val cleaned = value.replace("\\s".toRegex(), "")
        .replace(",", "")
        .replace("[^\\d\\.\\-]".toRegex(), "")
      cleaned.toDoubleOrNull()
    } catch (e: Exception) {
      null
    }
  }

  @Test
  fun testOcrParserWithScreenshotText() {
    val lines = listOf(
      "0:19 c ...      R lll lll 89",
      "Trade",
      "-1.41 USD",
      "Balance:               1 925.31",
      "Equity:                1 923.90",
      "Free margin:           1 890.68",
      "Margin Level (%):       5791.47",
      "Margin:                   33.22",
      "Positions",
      "XAUUSD, buy 0.01",
      "4 152.93 -> 4 152.07      -0.86",
      "XAUUSD, sell 0.01",
      "4 152.08 -> 4 152.37      -0.29",
      "XAUUSD, buy 0.01",
      "4 152.33 -> 4 152.07      -0.26"
    )

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

    assertEquals(1925.31, balance ?: 0.0, 0.001)
    assertEquals(1923.90, equity ?: 0.0, 0.001)
    assertEquals(1890.68, freeMargin ?: 0.0, 0.001)
    assertEquals(5791.47, marginLevel ?: 0.0, 0.001)
    assertEquals(33.22, margin ?: 0.0, 0.001)

    val parsedPositions = mutableListOf<ParsedPositionDummy>()
    var i = 0
    while (i < lines.size) {
      val line = lines[i].trim()
      val positionHeaderRegex = """(?i)([A-Z0-9\.\-#]{3,10})[\s,]+(buy|sell)\s*([\d\.]+)?""".toRegex()
      val match = positionHeaderRegex.find(line)
      if (match != null) {
        val symbol = match.groupValues[1].uppercase()
        val type = match.groupValues[2].uppercase()

        var openPrice: Double? = null
        var currentPrice: Double? = null
        var profit: Double? = null

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
          ParsedPositionDummy(
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

    assertEquals(3, parsedPositions.size)

    assertEquals("XAUUSD", parsedPositions[0].symbol)
    assertEquals("BUY", parsedPositions[0].positionType)
    assertEquals(4152.93, parsedPositions[0].openPrice ?: 0.0, 0.001)
    assertEquals(4152.07, parsedPositions[0].currentPrice ?: 0.0, 0.001)
    assertEquals(-0.86, parsedPositions[0].positionPnl ?: 0.0, 0.001)

    assertEquals("XAUUSD", parsedPositions[1].symbol)
    assertEquals("SELL", parsedPositions[1].positionType)
    assertEquals(4152.08, parsedPositions[1].openPrice ?: 0.0, 0.001)
    assertEquals(4152.37, parsedPositions[1].currentPrice ?: 0.0, 0.001)
    assertEquals(-0.29, parsedPositions[1].positionPnl ?: 0.0, 0.001)

    assertEquals("XAUUSD", parsedPositions[2].symbol)
    assertEquals("BUY", parsedPositions[2].positionType)
    assertEquals(4152.33, parsedPositions[2].openPrice ?: 0.0, 0.001)
    assertEquals(4152.07, parsedPositions[2].currentPrice ?: 0.0, 0.001)
    assertEquals(-0.26, parsedPositions[2].positionPnl ?: 0.0, 0.001)
  }

  @Test
  fun testOcrHorizontalClustering() {
    val elementsList = listOf(
      OcrElementDummy("Balance:", 50, 100, 120, 120),
      OcrElementDummy("1 925.31", 400, 101, 480, 121),
      OcrElementDummy("Equity:", 50, 130, 110, 150),
      OcrElementDummy("1 923.90", 400, 129, 481, 149),
      OcrElementDummy("XAUUSD, buy", 50, 180, 150, 200),
      OcrElementDummy("0.01", 160, 180, 200, 200),
      OcrElementDummy("-0.86", 420, 181, 470, 201)
    )

    val groupedLinesList = mutableListOf<MutableList<OcrElementDummy>>()
    val sortedElements = elementsList.sortedWith(compareBy({ it.top }, { it.left }))

    for (element in sortedElements) {
      var placed = false
      for (lineGroup in groupedLinesList) {
        val rep = lineGroup.first()
        val elementHeight = element.bottom - element.top
        val repHeight = rep.bottom - rep.top
        val avgHeight = (elementHeight + repHeight) / 2.0
        val verticalDistance = Math.abs((element.top + element.bottom) / 2 - (rep.top + rep.bottom) / 2)
        
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

    val reconstructedLines = groupedLinesList.map { lineGroup ->
      lineGroup.sortedBy { it.left }.joinToString(" ") { it.text }
    }

    assertEquals(3, reconstructedLines.size)
    assertEquals("Balance: 1 925.31", reconstructedLines[0])
    assertEquals("Equity: 1 923.90", reconstructedLines[1])
    assertEquals("XAUUSD, buy 0.01 -0.86", reconstructedLines[2])
  }
}

data class ParsedPositionDummy(
  val symbol: String,
  val positionType: String,
  val openPrice: Double?,
  val currentPrice: Double?,
  val positionPnl: Double?
)

data class OcrElementDummy(
  val text: String,
  val left: Int,
  val top: Int,
  val right: Int,
  val bottom: Int
)


