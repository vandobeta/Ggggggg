# Deriv Digit Analysis & Tactical Radar Tool - Development Plan

This document outlines the architecture, mathematical specifications, UI designs, and implementation roadmap for the Deriv Algorithmic Digit Analysis application. 

The application is engineered as a professional, sub-second latency, high-frequency style trading companion. It processes multiple Deriv WebSocket streams concurrently, ranks markets using a weighted composite formula, categorizes digit flows across statistical grids, and identifies high-probability entry targets for Even/Odd and Matches/Differs contracts.

---

## 1. Core Mathematical Specifications

The application uses an objective, multi-layered scoring matrix to scan and identify trend reversions based on the **Law of Large Numbers**. When statistical distributions significantly deviate from the 10% expected frequency (per digit) or 50% frequency (Even/Odd), a correction or trend mean-reversion is highly probable.

### The Unified Edge Score Formula ($E$)
For every active Volatility Index, a score from $0$ to $100$ is calculated:
$$E = w_1 \cdot M + w_2 \cdot V + w_3 \cdot A$$

Where weight parameters are balanced to favor aggressive micro-momentum convergence:
*   $w_1 = 0.3$ (Macro Imbalance Weight)
*   $w_2 = 0.5$ (Micro Momentum Weight)
*   $w_3 = 0.2$ (Digit Anomaly Weight)

### Layer 1: Macro Bias ($M$)
Measures structural equilibrium of the last 100 ticks.
$$M = |P_{\text{Even, 100}} - 50| \times 4$$
*   *Range:* $0$ to $100$.
*   *Purpose:* A larger deviation represents a higher macro-imbalance, preparing the spring for mean-reversion.

### Layer 2: Micro Momentum Convergence Speed ($V$)
Measures if the immediate 5-tick market velocity is moving in opposition to the macro bias to trigger a corrective kick-in.
$$V = \begin{cases} 
P_{\text{Even, 5}} & \text{if } P_{\text{Even, 100}} < 50 \\ 
100 - P_{\text{Even, 5}} & \text{if } P_{\text{Even, 100}} > 50 
\end{cases}$$
*   *Range:* $0$ to $100$.
*   *Purpose:* Captures the short-term burst showing that a reversion has initiated.

### Layer 3: Digit Anomaly Severity ($A$)
Tracks individual digit percentages ($0-9$). Measures how starved the absolute coldest digit ($D_{\text{min}}$) is relative to its theoretical 10% baseline.
$$A = (10 - P_{D_{\text{min}}, 100}) \times 10$$
*   *Range:* Coerced to $0$ to $100$.
*   *Purpose:* Pinpoints the target with the highest statistical drought (maximum variance stretch).

---

## 2. Deriv API WebSocket & Integration Strategy

### WebSocket Connection Specs
*   **Production Endpoint:** `wss://ws.derivws.com/websockets/v3?app_id=1089` (or configurable app ID from users).
*   **Dual-Stream Framework:**
    1.  **Direct Mode (Real Deriv API):** When connected, it registers a subscription frame JSON.
    2.  **Simulation Mode (Robust Fallback):** For offline use, mock accounts, and reliable local validation. Provides high-fidelity simulated index feeds matching live tick behavior with adjustable latency.

### Multi-Stream Subscriptions
To track multiple indices simultaneously, the app sends a subscription request for active volatility symbols:
```json
{
  "ticks": ["R_10", "R_25", "R_50", "R_75", "R_100", "1HZ10V", "1HZ25V", "1HZ50V", "1HZ100V"],
  "subscribe": 1
}
```
*Note: Deriv's API allows subscribing to multiple streams. When ticks arrive, we parse the incoming JSON message containing the active symbol and the last tick price/integer portion, extracting the final fractional digit.*

### Processing Pipeline
1.  **Ingestion:** Real-time stream receives tick payload.
2.  **Tick Extraction:** Extract the absolute last digit of the tick value (e.g., if price is `743.82`, digit is `2`).
3.  **Buffer Management:** Maintain a sliding queue of exactly 100 ticks per symbol.
4.  **Recalculation:** Trigger recalculations of the Composite Edge Score ($E$) for that symbol immediately upon tick receipt.
5.  **Re-Ranking:** Bubble the maximum edge score symbol to the top of the queue.

---

## 3. UI/UX & Native Screen Wireframes

We will implement a dual-pane responsive layout using Jetpack Compose, styled according to the **"Industrial Cyberpunk Dark"** theme (high contrast, ultra-black backgrounds, neon indicators, dynamic widgets, and zero generic AI appearance).

### Screen 1: Dashboard Radar & Tactical Matrix
*   **Header Bar:** Real-time stream monitor, connection state, simulated delay gauge, and subscription indicators.
*   **4-Quadrant Digit Matrix Cluster:**
    *   **LOWER ODD** (Digits 1, 3)
    *   **LOWER EVEN** (Digits 0, 2, 4)
    *   **HIGHER ODD** (Digits 5, 7, 9)
    *   **HIGHER EVEN** (Digits 6, 8)
    *   Dynamic colors: highlight bloated quadrants (statistical surplus) in hot coral/crimson, and starved quadrants (droughts) in high-visibility neon cyber green.
*   **Industrial Progress Gauges:** 
    *   *Momentum Velocity Engine* Progress Bar (Blue Neon)
    *   *Bridge Noise Transition Filter* Progress Bar (Red Neon)
    *   *Market Variance Stability Index* Progress Bar (Amber Neon)
*   **Active Volatility Selector/Grid:** Scrollable, dynamically ranked row/list showing all scanned symbols sorted from highest edge score ($E$) down to lowest. Selecting a symbol immediately binds its detailed analysis to the view.

### Screen 2: Prediction Engine & Playbook Console
*   **Prime Candidate Corridor:** 
    *   Displays the sorted drought ranks of digits.
    *   **Rank 1 (The Prime Prediction)**: Drawn with a prominent glowing golden layout, showing the target digit, the mathematical quadrant details and its overall confidence percentage ($0$ to $100\%$).
*   **Interactive Ticks Bar Chart:**
    *   Calculates and renders custom horizontal visual drought lines.
    *   Instantly targets the shortest bar on that screen, mapping boundaries (**OVER/UNDER** thresholds and **Matches/Differs** options).
*   **Live Algorithmic Recommender:**
    *   Formulates a structured execution contract advice (e.g., "BUY ODD / UNDER 8 CONTRACT").
    *   **Simulated Practice Executions:** Includes virtual trade simulators to let users tap "Mock Fire Order" and view trade outcome feedback based on the very next incoming tick from the selected market index.

---

## 4. MVVM Architecture & Kotlin Source Structure

The app will follow clean MVVM guidelines:

```
com.example
├── MainActivity.kt                      # Layout Scaffold, Navigation Controller
├── data
│   ├── DerivWebSocketManager.kt         # Client managing connections & simulated ticks
│   └── Models.kt                        # Unified data definitions
├── ui
│   ├── theme
│   │   ├── Theme.kt                     # Deep black theme, Material 3 configurations
│   │   ├── Color.kt                     # Cyberpunk palette (emerald, crimson, amber, cyan)
│   │   └── Type.kt                      # Monospace values & high-legibility trading typography
│   ├── viewmodel
│   │   └── DigitAnalysisViewModel.kt    # Tactical mathematical formulas & active streams
│   └── screens
│       ├── DashboardScreen.kt           # Screen 1: The Tactical Multi-Market Radar Grid
│       └── PredictionsScreen.kt         # Screen 2: Numerical Drought Targets & Practicing Console
```

---

## 5. Development Phase Plan & Milestones

1.  **Phase 1: Project Alignment & Platform Sync**
    *   Set `applicationId` to `com.aistudio.derivradar.tgfbyu`
    *   Rename the launcher application label to "Deriv Radar" in resource strings.
    *   Sync project names in `metadata.json`.
2.  **Phase 2: Mathematical Engine & Data Models**
    *   Implement unified models (`MarketScanResult`, `LivePredictionModel`, `CompleteDataPacket`).
    *   Write the core formula engine calculations with exact weights ($0.3, 0.5, 0.2$).
3.  **Phase 3: WebSocket Transport Layer**
    *   Implement `DerivWebSocketManager` with real OkHttp WebSocket subscription streams + dual mode simulator.
4.  **Phase 4: MVVM Core ViewModel Integration**
    *   Initialize `DigitAnalysisViewModel` maintaining concurrent sliding counters for all volatility streams.
5.  **Phase 5: High-Polish Jetpack Compose UI & Dynamic Gauges**
    *   Build Screen 1: Standard M3 scaffolding, 4-Quadrant live matrix layout, customizable micro/macro mechanical gauges panel.
    *   Build Screen 2: Digit analysis visual chart, golden rank card, interactive recommend triggers, and simulation practice ledger.
6.  **Phase 6: Multi-Screen Navigation & Final Polish**
    *   Integrate safe navigation between screens, swipeable layout options, clean accessibilities, and visual feedback ripples.
    *   Compile the app via `compile_applet` and perform rigorous validations.

---
**Consent Statement:**
Ready to create the workspace core files according to these design specifications. Please acknowledge if we can begin implementation.
