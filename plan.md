# 📝 Deriv High-Fidelity Integration Milestones Tracker

This document outlines structural goals, real accomplishments, and pending directions for the authentic Deriv API connection suite and analytical platform.

---

## 🧭 Active Milestones

### ✅ Milestone 1: Plumbing Secure State & Terminal Flows
- Expose direct indicators from Deriv API standard specifications.
- Configure secure, production-ready live websocket connection to `wss://ws.binaryws.com/websockets/v3?app_id=1089` representing direct, zero-simulation market feeds.
- Maintain accurate connection state metrics: Connection States, Ping RTT latency in milliseconds, and WebSocket transaction streams.
- Feed logs directly into a high-contrast console terminal using styled `WsLog` frames (`INFO`, `ERROR`, `OUTBOUND`, `INBOUND`).
- Persist live-authorized details including `fullname`, `email`, `currency`, and `scopes` list.

### ✅ Milestone 2: Direct Database & Dynamic Balance Synchronization
- Automatically collect WebSocket-level active balances (`authorizedBalance`) and client details (`authorizedTraderName`).
- Sync results directly to local SQLite via Room's persistence repository (`AppSettings`) so both Demo and Real account balance cards reflect changes live across other dashboards.

### ✅ Milestone 3: Live Active Contract Tracking Streams
- Hook custom handlers into the `proposal_open_contract` channel for purchases, buying quotes, and actual results tracking.
- Collect live bid price fluctuations and compute actual wins/losses instantly without emulation fallback blocks.

### ✅ Milestone 4: Immersive Real-Time Trader Terminal (DERIV LIVE)
- Build a dedicated, full-screen dashboard consisting of:
  1. Connection Diagnostics
  2. Verifiable Client Profile Data
  3. Interactive Active Trade Contract trackers
  4. Monospace Terminal System console log with color states
  5. Decoded history outputs (Wins, Losses, Exits, Profits)
- Embed full-screen system flags at activity launch.

### ✅ Milestone 5: Dual Demo/Real Auto-Configurations & Token Auto-Detection
- Read the official `is_virtual` attribute from successful `authorize` WebSocket payloads to auto-detect the token's real account type.
- Automatically configure and transition the application's global state (`isDemoAccount`) to match the authorized token type rather than erroring out due to toggle mismatch.
- Support real-time synchronization between the authorized socket account mode and local persistence databases seamlessly.

---

## ❓ Follow-up Verification Questions
*To keep implementation strictly aligned with expectations without any assumptions:*
1. Under the new auto-detect token flow, would you like a specific visual toast or warning modal to inform the user if their token changed the app's mode (e.g., "Note: Switched to Real Account Mode based on token")?
2. Do you need a dedicated webhook receiver or background alert system when the application is completely minimized, or is the dual-mode Picture-in-Picture logic and the active dashboard sufficient?
3. Should the historical outcomes filter by specific underlying assets (e.g., Volatility 10 Index only) or is the global chronological list of completes preferred?
4. What is the preferred minimum threshold latency warning value? (Our default is set to color-code yellow above `150ms` and green below it).
