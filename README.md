# ExaGuard — CFD Margin Level & Risk Calculator
### Android Application for Real-Time Forex & CFD Margin Protection, Liquidation Breakpoints & OCR Parsing

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![ML Kit & Gemini](https://img.shields.io/badge/AI%20OCR-Gemini%20Flash%20%2B%20ML%20Kit-FF6F00.svg)](https://ai.google.dev/)
[![Room Database](https://img.shields.io/badge/Storage-Room%202.7-brightgreen.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-Proprietary%20%2F%20MIT-lightgrey.svg)](LICENSE)

---

## 📌 Executive Summary

**ExaGuard Margin Level Calculator** is an enterprise-grade Android financial utility designed for Contracts for Difference (CFD) and Forex traders. It solves one of the most critical challenges in leveraged trading: **calculating exact liquidation and margin call price levels before they happen**.

By ingesting MetaTrader 4 (MT4), MetaTrader 5 (MT5), or cTrader trade screenshots—or through direct parameter input—ExaGuard dynamically determines the precise asset price where an account will trigger:
1. **100% Margin Call** (broker warning threshold prohibiting new positions)
2. **30% Stop Out** (forced automatic liquidation threshold)

The app features an interactive risk simulator slider, multi-position portfolio aggregation, offline-first Room database history, and an AI-driven dual-engine OCR pipeline combining **Google Gemini Flash Multimodal Vision** with an offline **Google ML Kit** clustering fallback.

---

## 📐 Mathematical Formulation & Financial Basis

The core mathematical innovation of ExaGuard is its **Empirical Price Sensitivity Derivation ($k_{\text{signed}}$)**. Rather than requiring users to manually lookup contract sizes, leverage ratios, and quote currency exchange conversions, ExaGuard derives the exact monetary price velocity directly from live position state.

### 1. Fundamental Account Relations

In CFD and Forex trading accounts:
$$\text{Equity} = \text{Balance} + \sum_{i=1}^{n} \text{Floating PnL}_i$$

$$\text{Margin Level \%} = \left( \frac{\text{Equity}}{\text{Used Margin}} \right) \times 100\%$$

$$\text{Free Margin} = \text{Equity} - \text{Used Margin}$$

### 2. Derivation of the Signed Sensitivity Factor ($k_{\text{signed}}$)

For any active leveraged position:
- **BUY (Long)**: $\text{PnL} = V \cdot C \cdot (P_{\text{current}} - P_{\text{open}})$
- **SELL (Short)**: $\text{PnL} = V \cdot C \cdot (P_{\text{open}} - P_{\text{current}}) = - V \cdot C \cdot (P_{\text{current}} - P_{\text{open}})$

Where:
- $V$ = Lot volume (e.g., $0.01, 1.00$)
- $C$ = Contract size in base units (e.g., $100,000$ for FX, $100$ for Gold/XAUUSD, $1$ for Crypto)

Defining the signed monetary sensitivity factor per unit price change ($k_{\text{signed}}$):
$$k_{\text{signed}} = \frac{\text{Floating PnL}}{P_{\text{current}} - P_{\text{open}}}$$

- For a **BUY** position: $k_{\text{signed}} > 0$ (Price increase $\implies$ positive PnL).
- For a **SELL** position: $k_{\text{signed}} < 0$ (Price increase $\implies$ negative PnL).

When price moves by $\Delta P = P_{\text{target}} - P_{\text{current}}$, the change in equity is:
$$\Delta \text{Equity} = \Delta \text{PnL} = k_{\text{signed}} \cdot (P_{\text{target}} - P_{\text{current}})$$

### 3. Exact Solution for Target Margin Level Breakpoint Prices

To find the asset price $P_{\text{target}}$ that causes Margin Level to reach a specific target percentage $T$ (such as $100\%$ or $30\%$):

$$\text{Target Equity} = \left(\frac{T}{100}\right) \cdot \text{Used Margin}$$

$$\Delta \text{Equity} = \text{Target Equity} - \text{Current Equity} = \left(\frac{T}{100} \cdot \text{Margin}\right) - \text{Equity}$$

Setting $\Delta \text{Equity} = k_{\text{signed}} \cdot (P_{\text{target}} - P_{\text{current}})$ and solving for $P_{\text{target}}$:

$$\boxed{P_{\text{target}} = P_{\text{current}} + \frac{\left(\frac{T}{100} \cdot \text{Used Margin}\right) - \text{Current Equity}}{k_{\text{signed}}}}$$

#### Margin Call ($100\%$ Threshold):
$$P_{100\%} = P_{\text{current}} + \frac{\text{Used Margin} - \text{Current Equity}}{k_{\text{signed}}}$$

#### Stop Out / Liquidation ($30\%$ Threshold):
$$P_{30\%} = P_{\text{current}} + \frac{(0.30 \cdot \text{Used Margin}) - \text{Current Equity}}{k_{\text{signed}}}$$

### 4. Interactive Simulation Model

When adjusting the simulated price slider ($P_{\text{sim}}$) or account equity injection slider ($E_{\text{sim}}$):
$$\text{PnL}_{\text{sim}} = \text{PnL}_{\text{current}} + k_{\text{signed}} \cdot (P_{\text{sim}} - P_{\text{current}})$$
$$\text{Equity}_{\text{sim}} = E_{\text{sim}} + k_{\text{signed}} \cdot (P_{\text{sim}} - P_{\text{current}})$$
$$\text{Margin Level}_{\text{sim}} \% = \left( \frac{\text{Equity}_{\text{sim}}}{\text{Used Margin}} \right) \times 100\%$$

---

## 🏛️ System Architecture & Design Document

ExaGuard follows **Modern Android Architecture (MVVM + Clean Architecture)** principles with a unidirectional data flow (UDF).

```
┌─────────────────────────────────────────────────────────────────┐
│                       Presentation Layer                        │
│                                                                 │
│  ┌───────────────────────┐           ┌──────────────────────┐   │
│  │   Dashboard Screen    │           │  Input & Simulation  │   │
│  │  - Header Metrics     │           │  - Screenshot Picker │   │
│  │  - Breakpoint Cards   │           │  - 2D Sliders        │   │
│  │  - History List       │           │  - Multi-Pos Editor  │   │
│  └───────────▲───────────┘           └──────────▲───────────┘   │
│              │                                  │               │
│              └─────────────────┬────────────────┘               │
│                                │ State / Events                 │
│                                ▼                                │
│                     ┌─────────────────────┐                     │
│                     │   MarginViewModel   │                     │
│                     │  - StateFlow UDF    │                     │
│                     │  - Formula Engine   │                     │
│                     │  - Bitmap Scaling   │                     │
│                     └──────────┬──────────┘                     │
└────────────────────────────────┼────────────────────────────────┘
                                 │
┌────────────────────────────────┴────────────────────────────────┐
│                          Domain & Data                          │
│                                                                 │
│  ┌───────────────────────────┐      ┌────────────────────────┐  │
│  │     MarginRepository      │      │      GeminiClient      │  │
│  │  - Local SQLite / Room    │      │  - Dual-Layer OCR      │  │
│  │  - Offline Persistence    │      │  - Gemini Flash Vision │  │
│  │  - Moshi JSON Adapters    │      │  - ML Kit 2D Cluster   │  │
│  └─────────────┬─────────────┘      └────────────────────────┘  │
│                │                                                │
│                ▼                                                │
│  ┌───────────────────────────┐                                  │
│  │   Room AppDatabase (DAO)  │                                  │
│  │   Table: margin_calcs     │                                  │
│  └───────────────────────────┘                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Key Architectural Highlights:
1. **Unidirectional Data Flow (UDF)**: UI state is exposed via Kotlin Coroutines `StateFlow` and Compose `mutableStateOf`, ensuring atomic recompositions without race conditions.
2. **Dual-Layer OCR Pipeline**:
   - **Tier 1 (Cloud Vision)**: Calls Gemini Multimodal Flash with an optimized, temperature-zero system instruction enforcing pure JSON extraction.
   - **Tier 2 (On-Device Fallback)**: If offline or API fails, Google ML Kit Vision extracts text blocks, passes them through a custom **horizontal bounding-box clustering algorithm**, and extracts trading figures via specialized regular expressions.
3. **Optimized Bitmap Ingestion**: Images are pre-scaled in memory using logarithmic dimension calculation to prevent `OutOfMemoryError` on 4K/high-density mobile screenshots while minimizing Gemini API payload latency.
4. **Moshi Serialization**: Full support for multi-position portfolios serialized as JSON arrays inside Room SQLite entities.

---

## 💻 Technology Stack

| Layer | Technologies / Libraries |
|---|---|
| **Language** | Kotlin 2.1.0 (100% Kotlin codebase) |
| **UI Framework** | Jetpack Compose (Material Design 3, Edge-to-Edge) |
| **Architecture** | MVVM, Clean Architecture, Unidirectional Data Flow (UDF) |
| **Asynchronous** | Kotlin Coroutines, StateFlow, Flow |
| **Local Database** | Android Jetpack Room 2.7 with KSP (Kotlin Symbol Processing) |
| **Serialization** | Moshi (Kotlin Reflection & Polymorphic Adapters) |
| **Image Loading** | Coil Compose (v3.1.0) with custom bitmap memory decoders |
| **Artificial Intelligence** | Gemini Flash Multimodal API (REST) |
| **On-Device Vision** | Google ML Kit Text Recognition with 2D spatial clustering |
| **Camera & Media** | Android Activity Result Contracts (`PickVisualMedia`, `TakePicturePreview`) |
| **Testing** | JUnit 4, Robolectric, Roborazzi UI screenshot verification |

---

## 🚀 Getting Started & Build Instructions

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+) or newer
- **JDK**: Java 17 or Java 21
- **Android SDK**: Min SDK 24 (Android 7.0), Target SDK 36 (Android 15)

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/margin-level-calculator.git
   cd margin-level-calculator
   ```
2. Set up Gemini API key (optional for online AI extraction; offline ML Kit works without a key):
   Create a `.env` file in the root directory:
   ```properties
   GEMINI_API_KEY="your_actual_gemini_api_key_here"
   ```
3. Build the debug APK via Gradle:
   ```bash
   gradle :app:assembleDebug
   ```
4. Run local JVM unit and Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 🛡️ License & Trademarks

Developed by **Prasanti Tech & ExaGuard**.  
All trademarks, including MetaTrader 4, MetaTrader 5, and cTrader, belong to their respective owners.
