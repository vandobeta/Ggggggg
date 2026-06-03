# Project Plan & Milestone Tracker

This project is a high-frequency algorithmic derivatives indicator and automated co-pilot executor for trading parity digit contracts (UNDER, OVER, DIFFERS, EVEN, ODD) using WebSocket parity streams.

## Done / Completed Milestones

### 1. Unified Global Header & Account Type Switching
*   **Demo & Real Account Segregation**: Integrated independent demo vs real wallet balances and persisted preferences inside local SQLite Room configuration database.
*   **Header Interface**: Integrated a cohesive, modern Material 3 global navigation bar header inside `MainActivity.kt` with instant toggle capability to transition seamlessly between test and live sandboxes.
*   **Error Banner**: Embedded a high-visibility, dismissible error and websocket connection status alert banner directly coupled to ViewModel states.

### 2. Room Trade History Persistence Log Engine
*   **Trade Database Entities**: Designed `TradeHistory` Room scheme in `TradeHistory.kt` with complete transaction metadata including timestamps, stakes, barrier thresholds, contract specifications, entry digits, exit digits, status (WIN, LOSS, PENDING), and net performance payout margins.
*   **Observer Pipelines**: Bound StateFlow collectors in `DigitAnalysisViewModel` to retrieve transactional ledger streams in real-time, handling instant clearing/purging operations cleanly.

### 3. Interactive RADAR Manual Trade Execution Deck
*   **Manual Executor Module**: Designed a dedicated manual action area inside `SignalsScreen.kt` featuring real-time input fields bound to custom preset stakes (`$1.00`, `$5.00`, `$10.00`, `$25.00`, `$50.00`).
*   **Dynamic Triggers**: Linked manual submission buttons dynamically to execute contracts using raw Deriv token REST pathways or local simulator fallback pipelines instantly.

### 4. Transaction History Dashboard Tab
*   **Sub-Tab Selector**: Expanded the tab structures inside `SignalsScreen.kt` from 2 options to 3: **RADAR DECK** (Live analysis), **TRADES** (Live performance logs), and **SIGNALS** (Historical recommendation stats).
*   **Net Performance Aggregations**: Calculated and displayed actual session win percentage metrics, cumulative Profit/Loss markers, and active transaction listings with colored card highlights.

### 5. Algorithmic Debug Separations
*   **Developer Consolidation**: Moved diagnostic facilities and demo alarm simulations inside `SettingsScreen.kt` behind a secure, collapsible developer drawer, preserving clean aesthetics for retail configurations.

### 6. Mathematically Locked Out-Of-Span Weight Engine
*   **QuantitativeContractCompiler**: Introduced mathematically locked, weight-aware out-of-span routing.
*   **Weight & Position Optimization**: Candidate matrix prioritized with `[Primary Anchor (Index 0), Noise Guard 1, Noise Guard 2]`. Configured Index 0 carry supreme predictable gravity when Even/Odd ratios stretch.
*   **Stability Gates (S101)**:
    *   *Dead Zone* (<40%): Terminate/suspend all outputs.
    *   *Active Zone* (40%-85%): Authorize Over/Under and Even/Odd modes.
    *   *Sniper Zone* (>85%): Unlocks high-conviction Differs signals.
    *   *Parity Kill-Switch*: Active under Span 3-4 and Stability < 60% to safely abort pending parity orders.
    *   *Absolute Blacklist*: Span 9 triggers a hard lockout.

### 7. Structured Setup Wizard & Security Disclaimers
*   **4-Step Wizard**: Upgraded `FirstLaunchSetupScreen.kt` into a highly-directed 4-step wizard structure.
*   **Deriv Secure Token Capture**: Designed a dedicated Token Integration step capturing keys with high-visibility, exclusive disclaimers highlighting local encrypted storage, self-custody details, minimum permission profiles (Read & Trade scopes), and volatility cautions.
*   **Foolproof Startup Retention**: Reinforced onboarding state tracking with dual-layer storage (Room SQLite + SharedPreferences). Guarantees that completed setups permanently skip the setup screen on launch.

## Future Plans & Milestones
*   Real-time WebSocket connection listener optimizations and raw tick throughput enhancements.
