# ExaGuard — Technical Design Document & Mathematical Specification

This document provides the complete theoretical foundation, algebraic derivations, software architecture, and algorithm specifications for the **ExaGuard Margin Level Calculator** Android application.

---

## Table of Contents
1. [Mathematical Foundations & Derivations](#1-mathematical-foundations--derivations)
   - [1.1 Margin Accounting Mechanics](#11-margin-accounting-mechanics)
   - [1.2 Price Velocity Sensitivity Factor ($k_{\text{signed}}$)](#12-price-velocity-sensitivity-factor-k_textsigned)
   - [1.3 Exact Solution for Arbitrary Margin Level Targets](#13-exact-solution-for-arbitrary-margin-level-targets)
   - [1.4 Algebraic Proof for Long (BUY) and Short (SELL) Positions](#14-algebraic-proof-for-long-buy-and-short-sell-positions)
   - [1.5 Multi-Position Portfolio Extension](#15-multi-position-portfolio-extension)
2. [Dual-Engine OCR & Spatial Clustering Architecture](#2-dual-engine-ocr--spatial-clustering-architecture)
   - [2.1 High-Level Extraction Strategy](#21-high-level-extraction-strategy)
   - [2.2 Tier 1: Multimodal LLM Parser (Gemini Flash)](#22-tier-1-multimodal-llm-parser-gemini-flash)
   - [2.3 Tier 2: On-Device 2D Spatial Element Clustering (Google ML Kit)](#23-tier-2-on-device-2d-spatial-element-clustering-google-ml-kit)
3. [Software Architecture & Data Flow](#3-software-architecture--data-flow)
   - [3.1 MVVM & Clean Architecture Overview](#31-mvvm--clean-architecture-overview)
   - [3.2 State Management & Jetpack Compose Reactivity](#32-state-management--jetpack-compose-reactivity)
   - [3.3 Room SQLite Database Schema & Moshi Serialization](#33-room-sqlite-database-schema--moshi-serialization)
4. [Edge Case Handling & Numerical Safeguards](#4-edge-case-handling--numerical-safeguards)
5. [Resume / Technical Skills Overview](#5-resume--technical-skills-overview)

---

## 1. Mathematical Foundations & Derivations

### 1.1 Margin Accounting Mechanics

In margin trading accounts (MetaTrader 4/5, cTrader), brokers enforce risk controls through account metrics defined as follows:

$$\text{Balance} = \text{Initial Deposit} + \sum \text{Realized Closed PnL}$$

$$\text{Floating PnL} = \sum_{i=1}^N \text{Floating Profit/Loss of Open Position}_i$$

$$\text{Equity} = \text{Balance} + \text{Floating PnL}$$

$$\text{Margin Level \%} = \left( \frac{\text{Equity}}{\text{Used Margin}} \right) \times 100\%$$

$$\text{Free Margin} = \text{Equity} - \text{Used Margin}$$

Brokers define two critical margin safety thresholds:
- **Margin Call ($100\%$)**: When $\text{Equity} \le \text{Used Margin}$, the broker triggers an alert state. The trader is prevented from opening new orders.
- **Stop Out ($30\%$ to $50\%$, typically $30\%$ for retail FX)**: When $\text{Equity} \le 0.30 \times \text{Used Margin}$, the broker's liquidity server automatically issues market close orders to liquidate positions.

---

### 1.2 Price Velocity Sensitivity Factor ($k_{\text{signed}}$)

Calculating the exact liquidation price in traditional calculators requires entering:
- Lot size ($V$)
- Contract size ($C$)
- Leverage or margin percentage
- Currency exchange rate between the quote currency and account base currency

This approach is error-prone when trading exotic pairs, commodities (Gold, Silver, Oil), or indices. ExaGuard eliminates external lookups by evaluating the **empirical signed sensitivity factor** ($k_{\text{signed}}$).

For any active leveraged position:

$$\text{PnL} = \begin{cases} 
V \cdot C \cdot \frac{\text{Tick Value}}{\text{Tick Size}} \cdot (P_{\text{current}} - P_{\text{open}}), & \text{for BUY} \\
V \cdot C \cdot \frac{\text{Tick Value}}{\text{Tick Size}} \cdot (P_{\text{open}} - P_{\text{current}}), & \text{for SELL}
\end{cases}$$

Let the position constant be:
$$\alpha = V \cdot C \cdot \frac{\text{Tick Value}}{\text{Tick Size}} > 0$$

Then:
- For **BUY**: $\text{PnL} = +\alpha \cdot (P_{\text{current}} - P_{\text{open}})$
- For **SELL**: $\text{PnL} = -\alpha \cdot (P_{\text{current}} - P_{\text{open}})$

We define the signed sensitivity constant $k_{\text{signed}}$:
$$k_{\text{signed}} = \frac{\text{PnL}}{P_{\text{current}} - P_{\text{open}}}$$

Where:
- $k_{\text{signed}} = +\alpha > 0$ for a Long/BUY trade.
- $k_{\text{signed}} = -\alpha < 0$ for a Short/SELL trade.

---

### 1.3 Exact Solution for Arbitrary Margin Level Targets

Let $T$ be the target Margin Level percentage (e.g., $T = 100$ or $T = 30$).
The target equity required to satisfy $T$ is:

$$\text{Target Equity}(T) = \left( \frac{T}{100} \right) \cdot \text{Used Margin}$$

The required change in account equity is:
$$\Delta \text{Equity} = \text{Target Equity}(T) - \text{Current Equity}$$
$$\Delta \text{Equity} = \left( \frac{T}{100} \cdot \text{Used Margin} \right) - \text{Current Equity}$$

Because account equity changes strictly by the change in position PnL:
$$\Delta \text{Equity} = \Delta \text{PnL} = k_{\text{signed}} \cdot (P_{\text{target}} - P_{\text{current}})$$

Equating both expressions:
$$k_{\text{signed}} \cdot (P_{\text{target}} - P_{\text{current}}) = \left( \frac{T}{100} \cdot \text{Used Margin} \right) - \text{Current Equity}$$

Dividing both sides by $k_{\text{signed}}$:
$$P_{\text{target}} - P_{\text{current}} = \frac{\left( \frac{T}{100} \cdot \text{Used Margin} \right) - \text{Current Equity}}{k_{\text{signed}}}$$

$$\boxed{P_{\text{target}}(T) = P_{\text{current}} + \frac{\left( \frac{T}{100} \cdot \text{Used Margin} \right) - \text{Current Equity}}{k_{\text{signed}}}}$$

---

### 1.4 Algebraic Proof for Long (BUY) and Short (SELL) Positions

#### Case 1: Long (BUY) Position
- Suppose a trader bought 1.00 lot of EURUSD at $1.0800$. The price drops to $1.0750$.
- $P_{\text{current}} - P_{\text{open}} = 1.0750 - 1.0800 = -0.0050$.
- Floating PnL is negative (e.g., $-\$500.00$).
- $k_{\text{signed}} = \frac{-500}{-0.0050} = +100,000 > 0$.
- Since account equity is falling toward Stop Out, $\text{Target Equity} < \text{Current Equity}$, making the numerator negative.
- Dividing a negative numerator by $k_{\text{signed}} > 0$ yields a negative $\Delta P$.
- Consequently:
  $$P_{30\%} = P_{\text{current}} + (\text{negative value}) < P_{\text{current}}$$
- **Result**: The liquidation price correctly lies **below** the current market price.

#### Case 2: Short (SELL) Position
- Suppose a trader sold 1.00 lot of Gold (XAUUSD) at $2000.00$. The price rises to $2010.00$.
- $P_{\text{current}} - P_{\text{open}} = 2010.00 - 2000.00 = +10.00$.
- Floating PnL is negative (e.g., $-\$1000.00$).
- $k_{\text{signed}} = \frac{-1000}{+10} = -100 < 0$.
- The numerator is negative ($\text{Target Equity} - \text{Current Equity} < 0$).
- Dividing a negative numerator by $k_{\text{signed}} < 0$ yields a positive $\Delta P$.
- Consequently:
  $$P_{30\%} = P_{\text{current}} + (\text{positive value}) > P_{\text{current}}$$
- **Result**: The liquidation price correctly lies **above** the current market price.

---

### 1.5 Multi-Position Portfolio Extension

When an account holds $M$ active positions on the same instrument:
$$k_{\text{net}} = \sum_{i=1}^M k_{\text{signed}, i}$$

If the portfolio is net long ($k_{\text{net}} > 0$), price drops decrease equity; if net short ($k_{\text{net}} < 0$), price gains decrease equity. If hedged ($k_{\text{net}} \approx 0$), equity is invariant to price moves, and liquidation cannot occur via price movement alone (barring financing swaps or widening spreads).

---

## 2. Dual-Engine OCR & Spatial Clustering Architecture

```
                      ┌────────────────────────────┐
                      │    Uploaded Screenshot     │
                      └─────────────┬──────────────┘
                                    │
                                    ▼
                      ┌────────────────────────────┐
                      │ Memory-Safe Downsampling   │
                      │ (Max Dim: 1024px, Log2 Sc) │
                      └─────────────┬──────────────┘
                                    │
                  ┌─────────────────┴─────────────────┐
                  │                                   │
                  ▼                                   ▼
        [Online Path (Primary)]             [Offline Path (Fallback)]
  ┌───────────────────────────────┐   ┌──────────────────────────────────┐
  │  Gemini Flash Multimodal REST │   │ Google ML Kit Text Recognition   │
  │  - Zero-temp deterministic    │   │ - TextBlock / Line / Element     │
  │  - JSON Schema enforcement    │   │ - 2D Spatial Center Clustering   │
  │  - Multi-position parsing     │   │ - Regex Currency Number Sanitizer│
  └───────────────┬───────────────┘   └────────────────┬─────────────────┘
                  │                                    │
                  └─────────────────┬──────────────────┘
                                    │
                                    ▼
                      ┌────────────────────────────┐
                      │ Unified Trading Model      │
                      │ (Balance, Equity, Margin,  │
                      │  MarginLevel%, Positions)  │
                      └────────────────────────────┘
```

### 2.1 Spatial Clustering Algorithm for Tabular Screenshots

Mobile screenshots from MetaTrader or trading apps display label-value pairs side-by-side (e.g., `Balance:` on the left at $X=50$, and `1 925.31` on the right at $X=420$). Naive OCR often groups vertically rather than horizontally.

ExaGuard implements an on-device **vertical proximity clustering algorithm**:

```kotlin
// Algorithm: 2D Bounding-Box Line Reconstruction
val sortedElements = elementsList.sortedWith(compareBy({ it.top }, { it.left }))

for (element in sortedElements) {
    var placed = false
    for (lineGroup in groupedLinesList) {
        val rep = lineGroup.first()
        val avgHeight = ((element.bottom - element.top) + (rep.bottom - rep.top)) / 2.0
        val centerDistance = abs((element.top + element.bottom)/2 - (rep.top + rep.bottom)/2)
        
        // Associate elements if vertical center deviation is <= 70% of element height
        if (centerDistance < avgHeight * 0.7) {
            lineGroup.add(element)
            placed = true
            break
        }
    }
    if (!placed) groupedLinesList.add(mutableListOf(element))
}

// Order elements horizontally left-to-right to reconstruct tabular sentences
val reconstructedLines = groupedLinesList.map { lineGroup ->
    lineGroup.sortedBy { it.left }.joinToString(" ") { it.text }
}
```

---

## 3. Software Architecture & Data Flow

### 3.1 Unidirectional Data Flow (UDF)

```
[User Action] ──► [MarginViewModel] ──► [State Mutation / Flow] ──► [Compose Recomposition]
   - Slider Drag       - Recalculates       - allCalculations           - Live Breakpoint Card
   - Take Photo        - Triggers OCR       - simulatedPriceValue       - Dynamic Warning Banner
   - Add Position      - Room Database      - simulatedEquityValue      - History Item Card
```

### 3.2 Database Schema (`margin_calculations`)

| Column Name | SQLite Data Type | Description |
|---|---|---|
| `id` | `INTEGER PRIMARY KEY AUTOINCREMENT` | Unique calculation ID |
| `timestamp` | `INTEGER` | Unix epoch millisecond timestamp |
| `symbol` | `TEXT` | Instrument ticker (e.g., `EURUSD`, `XAUUSD`) |
| `positionType` | `TEXT` | `BUY` or `SELL` |
| `openPrice` | `REAL` | Position execution price |
| `currentPrice` | `REAL` | Market price at scan time |
| `positionPnl` | `REAL` | Floating profit/loss |
| `equity` | `REAL` | Account equity at scan time |
| `margin` | `REAL` | Used margin required by broker |
| `price100` | `REAL` | Computed price for 100% Margin Call |
| `price30` | `REAL` | Computed price for 30% Stop Out |
| `imageUri` | `TEXT` (Nullable) | Internal app storage path of saved screenshot |
| `extractedMarginLevel` | `REAL` (Nullable) | Broker's reported Margin Level % for cross-validation |
| `positionsJson` | `TEXT` (Nullable) | Moshi-serialized list of multi-position tickets |

---

## 4. Edge Case Handling & Numerical Safeguards

1. **Zero Used Margin ($M \le 0$)**: If an account has zero used margin (e.g., no open trades), margin level is mathematically undefined ($\infty$). The engine safely returns $0.0$ and displays an informative prompt.
2. **Zero Price Movement ($P_{\text{current}} = P_{\text{open}}$)**: When price has not moved since opening, $k_{\text{signed}}$ cannot be calculated from price difference. The engine falls back gracefully to $P_{\text{current}}$.
3. **Negative Price Prevention**: For short positions where theoretical stop-out could calculate below zero in degenerate configurations, results are clamped to $\ge 0.0$.
4. **Number Parsing Sanitization**: Handles European number formats, non-standard minus signs (en-dash `–`, em-dash `—`), spaces inside numbers (e.g., `1 925.31`), and currency symbols (`$`, `€`, `£`, `¥`).

---

## 5. Resume / Technical Portfolio Bullet Points

Here are production-grade resume descriptions highlighting the engineering involved in this application:

- **Android / Jetpack Compose**: Built a reactive financial trading risk management app in 100% Kotlin utilizing Jetpack Compose Material 3, custom Canvas/Sliders, and Unidirectional Data Flow (UDF).
- **Computer Vision & AI**: Architected a dual-stage OCR ingestion pipeline integrating Google Gemini Multimodal Flash REST API with on-device Google ML Kit Vision and a custom 2D geometric clustering algorithm to extract tabular financial metrics from MetaTrader screenshots.
- **Financial Engineering**: Derived and implemented closed-form linear pricing formulas ($k_{\text{signed}}$ price velocity) to calculate exact 100% margin call and 30% liquidation stop-out price levels without requiring broker-specific leverage lookups.
- **Local Persistence**: Engineered an offline-first storage architecture utilizing Android Room 2.7, Coroutine Flows, and Moshi JSON adapters for multi-position portfolio serialization.
