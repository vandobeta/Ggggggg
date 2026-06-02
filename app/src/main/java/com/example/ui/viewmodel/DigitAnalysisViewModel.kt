package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CompleteDataPacket
import com.example.data.DerivWebSocketManager
import com.example.data.LivePredictionModel
import com.example.data.MarketScanResult
import com.example.data.ReminderNotificationHelper
import com.example.data.db.AppDatabase
import com.example.data.db.AppSettings
import com.example.data.db.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class DigitAnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val wsManager = DerivWebSocketManager()
    
    // Database and Repository references
    private val db = AppDatabase.getInstance(application)
    private val repository = SettingsRepository(db.appSettingsDao())

    // Active selected index
    private val _selectedSymbol = MutableStateFlow("1HZ100V")
    val selectedSymbol: StateFlow<String> = _selectedSymbol.asStateFlow()

    // Sorted rankings of all symbols
    private val _marketRankings = MutableStateFlow<List<MarketScanResult>>(emptyList())
    val marketRankings: StateFlow<List<MarketScanResult>> = _marketRankings.asStateFlow()

    // Complete packet for selected index
    private val _selectedPacket = MutableStateFlow<CompleteDataPacket?>(null)
    val selectedPacket: StateFlow<CompleteDataPacket?> = _selectedPacket.asStateFlow()

    // Configuration Settings
    private val _userSettings = MutableStateFlow(AppSettings())
    val userSettings: StateFlow<AppSettings> = _userSettings.asStateFlow()

    // Counts how many high confidence triggers we had
    private val _triggeredSignalsToday = MutableStateFlow(0)
    val triggeredSignalsToday: StateFlow<Int> = _triggeredSignalsToday.asStateFlow()

    // --- AUTOMATED SIGNAL NOTIFIER STATE ---
    private val _isAutomatedNotifierRunning = MutableStateFlow(false)
    val isAutomatedNotifierRunning: StateFlow<Boolean> = _isAutomatedNotifierRunning.asStateFlow()

    private val _selectedNotifierContract = MutableStateFlow<String?>("MATCHES")
    val selectedNotifierContract: StateFlow<String?> = _selectedNotifierContract.asStateFlow()

    // --- VIRTUAL BACKTEST / WHAT-IF SIMULATOR ---
    private val _isBacktestActive = MutableStateFlow(false)
    val isBacktestActive: StateFlow<Boolean> = _isBacktestActive.asStateFlow()

    private val _backtestBetsCount = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val backtestBetsCount: StateFlow<Map<Int, Int>> = _backtestBetsCount.asStateFlow()

    private val _backtestWinsCount = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val backtestWinsCount: StateFlow<Map<Int, Int>> = _backtestWinsCount.asStateFlow()

    private val _backtestTransactions = MutableStateFlow<List<BacktestTx>>(emptyList())
    val backtestTransactions: StateFlow<List<BacktestTx>> = _backtestTransactions.asStateFlow()

    // --- PERSISTENT TRADE PRACTICE LOGS ---
    private val _practiceBets = MutableStateFlow<List<com.example.data.db.PracticeBet>>(emptyList())
    val practiceBets: StateFlow<List<com.example.data.db.PracticeBet>> = _practiceBets.asStateFlow()
    
    // --- REALTIME DYNAMIC 15-SEC SIGNALS SCREEN ---
    private val _signalCountdown = MutableStateFlow(30)
    val signalCountdown: StateFlow<Int> = _signalCountdown.asStateFlow()

    private val _activeSignal = MutableStateFlow<LiveTradeSignal?>(null)
    val activeSignal: StateFlow<LiveTradeSignal?> = _activeSignal.asStateFlow()

    private var signalsJob: Job? = null

    // --- PERSISTENT TRIGGERED SIGNALS HISTORY FLOW ---
    private val _signalHistory = MutableStateFlow<List<com.example.data.db.SignalHistory>>(emptyList())
    val signalHistory: StateFlow<List<com.example.data.db.SignalHistory>> = _signalHistory.asStateFlow()

    private val pendingSignals = java.util.concurrent.CopyOnWriteArrayList<com.example.data.db.SignalHistory>()
    
    private var lastMorningNotificationDay = -1
    private var lastSignalNotificationTime = 0L

    // Unified helper for triggering actual live notifications with 15s cooldown and onboard protection
    private fun sendPersonalizedSignal(
        context: android.content.Context,
        title: String,
        content: String,
        signalDescription: String? = null
    ): Boolean {
        val settings = _userSettings.value
        if (settings.isFirstLaunch) {
            android.util.Log.d("DigitAnalysisViewModel", "Suppressing notification: First-launch onboarding is in progress.")
            return false
        }

        val currentTime = System.currentTimeMillis()
        val cooldownMillis = (settings.signalNotificationCooldownSecs * 1000L).coerceAtLeast(15000L)
        if (currentTime - lastSignalNotificationTime < cooldownMillis) {
            android.util.Log.d("DigitAnalysisViewModel", "Suppressing notification: Under custom $cooldownMillis ms cooldown safety limit.")
            return false
        }

        lastSignalNotificationTime = currentTime
        ReminderNotificationHelper.showReminderNotification(
            context = context,
            title = title,
            content = content,
            traderName = settings.traderName,
            signalDescription = signalDescription
        )
        return true
    }

    // Thread-safe state for tracking pending bets on ticks
    private var pendingBetSymbol: String? = null
    private var pendingBetDigit: Int = -1
    private var pendingBetContractType: String = ""
    private var pendingBetTargetDigit: Int = -1
    private var pendingBetDisplayName: String = ""
    private var pendingBetTicksObserved: Int = 0

    val connectionState: StateFlow<String> = wsManager.connectionState
    val pingState: StateFlow<Long> = wsManager.pingState

    private var updateJob: Job? = null
    private var alarmJob: Job? = null
    private val context = application.applicationContext

    // Thread-safe caches for ranking smoothing and crossover detection
    private val smoothedScores = java.util.concurrent.ConcurrentHashMap<String, Float>()
    private val previousEvenDominance = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    // --- HIGH FREQUENCY PROFILE STATE ARCHITECTURE ---
    private val _unifiedTickState = MutableStateFlow<com.example.data.UnifiedTickState?>(null)
    val unifiedTickState: StateFlow<com.example.data.UnifiedTickState?> = _unifiedTickState.asStateFlow()

    private val tickProcessors = java.util.concurrent.ConcurrentHashMap<String, com.example.data.HighFrequencyTickProcessor>()
    private val previousExecutionSafe = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    fun getProcessorFor(symbol: String): com.example.data.HighFrequencyTickProcessor {
        return tickProcessors.getOrPut(symbol) { com.example.data.HighFrequencyTickProcessor() }
    }

    init {
        wsManager.connect()
        observeSettings()
        startObservationLoop()
        startPeriodicAlarmLoop()
        observePracticeBets()
        startSignalsLoop()
    }

    private var lastObservedRiskProfile: String? = null

    private fun observeSettings() {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                if (settings != null) {
                    val oldRiskProfile = lastObservedRiskProfile
                    _userSettings.value = settings
                    lastObservedRiskProfile = settings.riskProfile
                    recalculateAllStates()
                    if (oldRiskProfile != null && oldRiskProfile != settings.riskProfile) {
                        generateFreshSignal()
                        startSignalsLoop()
                    }
                } else {
                    // Initialize default settings entity
                    repository.saveSettings(AppSettings())
                }
            }
        }
    }

    private suspend fun verifyAndStoreTickTimestamp(symbol: String, digit: Int) {
        val currentTime = System.currentTimeMillis()
        try {
            val latestStoredTick = db.tickOccurrenceDao().getLatestTick()
            if (latestStoredTick != null) {
                val timeDifference = kotlin.math.abs(currentTime - latestStoredTick.timestamp)
                if (timeDifference > 3000L) {
                    android.util.Log.w("DigitAnalysisViewModel", "CRITICAL GAP: Tick timestamp gap ${timeDifference}ms exceeds 3 seconds policy! Wiping tick history ONLY to preserve user setup configuration...")
                    db.tickOccurrenceDao().clearTicks()
                }
            }
            db.tickOccurrenceDao().insertTick(
                com.example.data.db.TickOccurrence(
                    symbolCode = symbol,
                    digit = digit,
                    timestamp = currentTime
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startObservationLoop() {
        updateJob = viewModelScope.launch(Dispatchers.Default) {
            wsManager.tickUpdateFlow.collect { update ->
                if (update != null) {
                    val symbol = update.first
                    val digit = update.second
                    verifyAndStoreTickTimestamp(symbol, digit)
                    processBacktestOnNewTick(symbol, digit)
                    processLiveSignalsOnNewTick(symbol, digit)

                    // Real-time State Distribution: Pass calculated UnifiedTickState to single UI StateFlow container
                    val rawPrice = wsManager.getLastPriceFor(symbol)
                    val settings = _userSettings.value
                    val contractChoice = if (settings.customContract.startsWith("OVER")) "OVER" else "UNDER"

                    val processor = getProcessorFor(symbol)
                    val unifiedState = processor.processIncomingMarketTick(rawPrice, contractChoice)

                    if (symbol == _selectedSymbol.value) {
                        _unifiedTickState.value = unifiedState

                        // Haptic Engine Interlocking: Based on selected screen track toggle for "Risky" or "Safer"
                        val profile = settings.riskProfile
                        val activeTrack = if (profile == "RISKY") unifiedState.riskyProfile else unifiedState.saferProfile

                        val previouslySafe = previousExecutionSafe[symbol] ?: false
                        val currentlySafe = activeTrack.isSafeToExecute

                        if (currentlySafe && !previouslySafe) {
                            // If isSafeToExecute == true, it fires a double pulse
                            triggerDoubleVibration()
                        } else if (!currentlySafe && previouslySafe) {
                            // If it drops to false, it instantly forces a heavy cooldown lock / warning
                            triggerConflictWarningVibration()
                        }
                        previousExecutionSafe[symbol] = currentlySafe
                    }
                }
                recalculateAllStates()
            }
        }
    }

    private fun startPeriodicAlarmLoop() {
        alarmJob?.cancel()
        alarmJob = viewModelScope.launch {
            while (true) {
                // Check if alarms are enabled OR our specialized automated signal notifier is running
                val settings = _userSettings.value
                val notifierActive = _isAutomatedNotifierRunning.value
                val notifierContract = _selectedNotifierContract.value

                if ((settings.alarmsEnabled || notifierActive) && settings.customContract == "ALL") {
                    val list = _marketRankings.value
                    val topSignal = list.firstOrNull()

                    if (topSignal != null) {
                        // Check if candidate recommended strategy is consistent with chosen contract type
                        val isCorrectContract = when (notifierContract) {
                            "OVER" -> topSignal.recommendedContract.contains("OVER")
                            "UNDER" -> topSignal.recommendedContract.contains("UNDER")
                            "DIFFERS" -> topSignal.recommendedContract.contains("DIFFERS")
                            "MATCHES" -> topSignal.recommendedContract.contains("MATCHES")
                            else -> true
                        }

                        if (!notifierActive || isCorrectContract) {
                            val textTitle = if (notifierActive) {
                                "🟢 Notifier Active: $notifierContract"
                            } else {
                                "Algo-Radar Market Alarm Signal"
                            }

                            val textDesc = if (topSignal.totalEdgeScore >= settings.signalStrengthMinimum) {
                                "🔥 ${topSignal.displayName} edge exceeded: ${String.format("%.1f", topSignal.totalEdgeScore)}. Strategy: ${topSignal.recommendedContract}"
                            } else {
                                "📊 Scan update: ${topSignal.displayName} strategy: ${topSignal.recommendedContract}"
                            }

                            if (_triggeredSignalsToday.value < settings.signalsPerDayLimit) {
                                val didSend = sendPersonalizedSignal(
                                    context = context,
                                    title = textTitle,
                                    content = textDesc,
                                    signalDescription = "${topSignal.displayName} - ${topSignal.recommendedContract}"
                                )
                                if (didSend) {
                                    _triggeredSignalsToday.value++
                                }
                            }
                        }
                    }
                }
                // Delay based on user interval
                val delayTime = (settings.alarmIntervalMinutes.toLong() * 60 * 1000).coerceAtLeast(15000)
                delay(delayTime)
            }
        }
    }

    fun fireTestAlarm() {
        viewModelScope.launch {
            val settings = _userSettings.value
            val list = _marketRankings.value
            val topSignal = list.firstOrNull()
            val textTitle = "🔔 Test Reminder Alarm Triggered"
            val textDesc = if (topSignal != null) {
                "Selected: ${topSignal.displayName} | Edge Score: ${String.format("%.1f", topSignal.totalEdgeScore)} | Confidence: ${String.format("%.1f%%", topSignal.confidence)}"
            } else {
                "Scanning Live Derivatives WebSocket target buffers..."
            }
            ReminderNotificationHelper.showReminderNotification(
                context = context, 
                title = textTitle, 
                content = textDesc,
                traderName = settings.traderName,
                signalDescription = if (topSignal != null) {
                    "${topSignal.displayName} Test Alert"
                } else {
                    "Demo Volatility Match Alert"
                }
            )
            
            // Adjust count
            _triggeredSignalsToday.value++
        }
    }

    fun updateSettingsInDb(newSettings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
        }
    }

    fun resetDailySignalsCounter() {
        _triggeredSignalsToday.value = 0
    }

    fun selectSymbol(symbol: String) {
        _selectedSymbol.value = symbol
        recalculateAllStates()
        generateFreshSignal()
        startSignalsLoop()
    }

    fun reconnect() {
        viewModelScope.launch {
            wsManager.disconnect()
            delay(500)
            wsManager.connect()
        }
    }

    private fun recalculateAllStates() {
        val allSymbols = DerivWebSocketManager.VOLATILITY_SYMBOLS
        val results = mutableListOf<MarketScanResult>()

        var selectedSymbolResult: CompleteDataPacket? = null
        val currentSelected = _selectedSymbol.value
        val settings = _userSettings.value

        for ((symbol, displayName) in allSymbols) {
            val history = wsManager.getHistoryFor(symbol)
            if (history.isEmpty()) {
                results.add(
                    MarketScanResult(
                        symbol = symbol,
                        displayName = displayName,
                        totalEdgeScore = 0f,
                        macroEvenPct = 50f,
                        microEvenVelocity = 50f,
                        primePredictionDigit = 0,
                        primeDigitPct = 10f,
                        recommendedContract = "Awaiting streams...",
                        confidence = 0f
                    )
                )
                if (symbol == currentSelected) {
                    selectedSymbolResult = createEmptyPacket(symbol, displayName)
                }
                continue
            }

            // Perform statistical calculations using dynamically adjusted settings lookback
            val n = history.size.toFloat()
            val macroLookbackLimit = settings.ticksToCompare.coerceIn(10, 1000)
            val microLookbackLimit = settings.microLookback.coerceIn(2, 20)

            val macroLookback = history.takeLast(macroLookbackLimit)
            val microLookback = history.takeLast(microLookbackLimit)

            // Digit distributions
            val counts = IntArray(10)
            macroLookback.forEach { counts[it]++ }

            // Macro even pct
            val evenCountCount = macroLookback.count { it % 2 == 0 }
            val macroEvenPct = if (macroLookback.isNotEmpty()) {
                (evenCountCount.toFloat() / macroLookback.size.toFloat()) * 100f
            } else 50f

            // Parity Crossover Detection Rule (ODD vs EVEN transitions)
            val currentlyEven = macroEvenPct > 50f
            previousEvenDominance[symbol] = currentlyEven

            // Micro velocity
            val microEvenCount = microLookback.count { it % 2 == 0 }
            val microEvenVelocity = if (microLookback.isNotEmpty()) {
                (microEvenCount.toFloat() / microLookback.size.toFloat()) * 100f
            } else 50f

            // Anomaly severity based on absolute coldest digit
            var primeDigit = 0
            var minDigitCount = Int.MAX_VALUE
            for (i in 0..9) {
                if (counts[i] < minDigitCount) {
                    minDigitCount = counts[i]
                    primeDigit = i
                }
            }
            val primeDigitPct = if (macroLookback.isNotEmpty()) {
                (minDigitCount.toFloat() / macroLookback.size.toFloat()) * 100f
            } else 10f

            // Strategic Scoring Formula: E = 0.3 * M + 0.5 * V + 0.2 * A
            val M = abs(macroEvenPct - 50f) * 4f
            val V = if (macroEvenPct < 50f) microEvenVelocity else (100f - microEvenVelocity)
            val A = (10f - primeDigitPct).coerceAtLeast(0f) * 10f
            val rawTotalEdgeScore = (0.3f * M) + (0.5f * V) + (0.2f * A)

            // Apply Exponential Moving Average (EMA) smoothing to sort out ranking noise
            val intensity = settings.smoothingIntensity.coerceIn(0f, 0.9f)
            val alpha = 1.0f - intensity
            val previousSmoothed = smoothedScores[symbol] ?: rawTotalEdgeScore
            val smoothedScore = (alpha * rawTotalEdgeScore) + ((1.0f - alpha) * previousSmoothed)
            smoothedScores[symbol] = smoothedScore
            val totalEdgeScore = smoothedScore

            // Quadrant Accumulation
            val macroSize = if (macroLookback.isNotEmpty()) macroLookback.size.toFloat() else 100f
            val countsFloat = counts.map { (it.toFloat() / macroSize) * 100f }
            val leWeight = countsFloat[0] + countsFloat[2] + countsFloat[4]
            val loWeight = countsFloat[1] + countsFloat[3]
            val heWeight = countsFloat[6] + countsFloat[8]
            val hoWeight = countsFloat[5] + countsFloat[7] + countsFloat[9]

            val quadWeights = mapOf(
                "LE" to leWeight,
                "LO" to loWeight,
                "HE" to heWeight,
                "HO" to hoWeight
            )

            // Predictions Engine (Confidence & dynamic drought configurations)
            val predictions = calculatePredictions(
                countsFloat, 
                macroEvenPct, 
                microEvenVelocity, 
                leWeight, 
                loWeight, 
                heWeight, 
                hoWeight,
                settings.droughtThreshold
            )

            val rank1 = predictions.firstOrNull()
            val finalConfidence = rank1?.confidence ?: 0f

            // Formulate recommended contract based strictly on riskProfile (OVERS, UNDERS, DIFFERS)
            val profile = settings.riskProfile
            val recommendedContract = when {
                primeDigit <= 3 -> {
                    if (profile == "RISKY") {
                        if (primeDigit % 2 == 0) "UNDER 1" else "DIFFERS 9"
                    } else {
                        if (primeDigit % 2 == 0) "UNDER 4" else "DIFFERS 8"
                    }
                }
                primeDigit >= 6 -> {
                    if (profile == "RISKY") {
                        if (primeDigit % 2 == 0) "OVER 8" else "DIFFERS 0"
                    } else {
                        if (primeDigit % 2 == 0) "OVER 5" else "DIFFERS 1"
                    }
                }
                else -> { // 4, 5
                    if (profile == "RISKY") {
                        if (primeDigit % 2 == 0) "UNDER 3" else "OVER 6"
                    } else {
                        if (primeDigit % 2 == 0) "UNDER 6" else "OVER 3"
                    }
                }
            }

            val result = MarketScanResult(
                symbol = symbol,
                displayName = displayName,
                totalEdgeScore = totalEdgeScore,
                macroEvenPct = macroEvenPct,
                microEvenVelocity = microEvenVelocity,
                primePredictionDigit = primeDigit,
                primeDigitPct = primeDigitPct,
                recommendedContract = recommendedContract,
                confidence = finalConfidence
            )
            results.add(result)

            if (symbol == currentSelected) {
                val noiseScore = if (microEvenVelocity in 21f..79f) 85f else 15f
                val stabilityScore = (100f - abs((leWeight + heWeight) - 50f) * 2f).coerceIn(0f, 100f)

                selectedSymbolResult = CompleteDataPacket(
                    symbol = symbol,
                    displayName = displayName,
                    livePing = pingState.value,
                    quadWeights = quadWeights,
                    digitBreakdowns = counts,
                    momentumScore = V,
                    noiseScore = noiseScore,
                    stabilityScore = stabilityScore,
                    predictionsList = predictions,
                    tickHistory = history,
                    lastTickValue = wsManager.getLastPriceFor(symbol),
                    isStableConnection = !wsManager.isSimulating()
                )
            }
        }

        _marketRankings.value = results.sortedByDescending { it.totalEdgeScore }
        _selectedPacket.value = selectedSymbolResult
    }

    private fun calculatePredictions(
        digitPcts: List<Float>,
        macroEvenPct: Float,
        microEvenVelocity: Float,
        leWeight: Float,
        loWeight: Float,
        heWeight: Float,
        hoWeight: Float,
        droughtThreshold: Float
    ): List<LivePredictionModel> {
        val list = mutableListOf<LivePredictionModel>()
        val evenIsDominating = macroEvenPct > 50f
        
        for (digit in 0..9) {
            val countPct = digitPcts[digit]
            val quadrant = when (digit) {
                0, 2, 4 -> "LOWER EVEN"
                1, 3 -> "LOWER ODD"
                6, 8 -> "HIGHER EVEN"
                else -> "HIGHER ODD"
            }

            // Using AppSettings.droughtThreshold dynamically
            val droughtFactor = (droughtThreshold - countPct).coerceAtLeast(0f) * 6f

            val isEven = (digit % 2 == 0)
            val parityImbalance = abs(macroEvenPct - 50f)
            val parityBonus = if (isEven != evenIsDominating) {
                (parityImbalance / 50f) * 22f
            } else {
                -5f
            }

            var blendInBonus = 0f
            if (evenIsDominating && quadrant == "LOWER EVEN") {
                blendInBonus = 12f
            } else if (!evenIsDominating && quadrant == "LOWER ODD") {
                blendInBonus = 12f
            }

            val microMovesOpposite = if (evenIsDominating) (microEvenVelocity < 50f) else (microEvenVelocity > 50f)
            val momentumBonus = if (microMovesOpposite && (isEven != evenIsDominating)) {
                15f
            } else {
                0f
            }

            val finalConfidence = (50f + droughtFactor + parityBonus + blendInBonus + momentumBonus).coerceIn(5f, 99f)

            list.add(
                LivePredictionModel(
                    digit = digit,
                    quadrant = quadrant,
                    confidence = finalConfidence,
                    occurrencePct = countPct
                )
            )
        }

        return list.sortedByDescending { it.confidence }
    }

    private fun createEmptyPacket(symbol: String, displayName: String): CompleteDataPacket {
        return CompleteDataPacket(
            symbol = symbol,
            displayName = displayName,
            livePing = pingState.value,
            quadWeights = mapOf("LE" to 0f, "LO" to 0f, "HE" to 0f, "HO" to 0f),
            digitBreakdowns = IntArray(10),
            momentumScore = 0f,
            noiseScore = 0f,
            stabilityScore = 0f,
            predictionsList = emptyList(),
            tickHistory = emptyList(),
            lastTickValue = 0.0,
            isStableConnection = false
        )
    }

    // --- AUTOMATED SIGNAL NOTIFIER ACTIONS ---
    fun toggleAutomatedNotifier(running: Boolean, contractType: String? = null) {
        _isAutomatedNotifierRunning.value = running
        if (contractType != null) {
            _selectedNotifierContract.value = contractType
        }
    }

    // --- BACKTESTING SIMULATOR ACTIONS ---
    fun toggleBacktest(active: Boolean) {
        _isBacktestActive.value = active
        if (!active) {
            pendingBetSymbol = null
            pendingBetContractType = ""
        }
    }

    fun resetBacktestStats() {
        _backtestBetsCount.value = emptyMap()
        _backtestWinsCount.value = emptyMap()
        _backtestTransactions.value = emptyList()
        pendingBetSymbol = null
        pendingBetContractType = ""
    }

    fun processBacktestOnNewTick(symbol: String, incomingDigit: Int) {
        if (!_isBacktestActive.value) return

        val currentPendingSymbol = pendingBetSymbol
        val currentPendingContractType = pendingBetContractType
        val currentPendingTargetDigit = pendingBetTargetDigit

        if (currentPendingSymbol == symbol && currentPendingContractType.isNotEmpty()) {
            pendingBetTicksObserved++
            if (pendingBetTicksObserved >= _userSettings.value.virtualTradeCloseTicks.coerceAtLeast(1)) {
                val isWin = when (currentPendingContractType) {
                    "MATCHES" -> incomingDigit == currentPendingTargetDigit
                    "DIFFERS" -> incomingDigit != currentPendingTargetDigit
                    "OVER" -> incomingDigit > currentPendingTargetDigit
                    "UNDER" -> incomingDigit < currentPendingTargetDigit
                    else -> false
                }

                val resultStr = if (isWin) {
                    android.util.Log.d("FairVirtualWinHook", "🟢 [VIRTUAL WIN] Placed virtual bet on $currentPendingContractType $currentPendingTargetDigit. Real incoming digit: $incomingDigit (closed after ${_userSettings.value.virtualTradeCloseTicks} ticks)")
                    "WIN"
                } else {
                    android.util.Log.d("FairVirtualWinHook", "🔴 [VIRTUAL LOSS] Placed virtual bet on $currentPendingContractType $currentPendingTargetDigit. Real incoming digit: $incomingDigit (closed after ${_userSettings.value.virtualTradeCloseTicks} ticks)")
                    "LOSS"
                }

                // Update Counts
                val oldBetsMap = _backtestBetsCount.value.toMutableMap()
                val oldWinsMap = _backtestWinsCount.value.toMutableMap()

                oldBetsMap[currentPendingTargetDigit] = (oldBetsMap[currentPendingTargetDigit] ?: 0) + 1
                if (isWin) {
                    oldWinsMap[currentPendingTargetDigit] = (oldWinsMap[currentPendingTargetDigit] ?: 0) + 1
                }
                _backtestBetsCount.value = oldBetsMap
                _backtestWinsCount.value = oldWinsMap

                // Record transaction
                val tx = BacktestTx(
                    timestamp = System.currentTimeMillis(),
                    digit = currentPendingTargetDigit,
                    contractType = currentPendingContractType,
                    result = resultStr,
                    entryDigit = incomingDigit,
                    targetDigit = currentPendingTargetDigit,
                    symbolDisp = pendingBetDisplayName
                )
                val oldList = _backtestTransactions.value.toMutableList()
                oldList.add(0, tx)
                if (oldList.size > 50) {
                    oldList.removeAt(oldList.size - 1)
                }
                _backtestTransactions.value = oldList

                // Clear pending
                pendingBetSymbol = null
                pendingBetContractType = ""
            }
        }

        // Place next bet on active symbol
        val packet = _selectedPacket.value
        if (pendingBetSymbol == null && packet != null && packet.symbol == symbol && packet.predictionsList.isNotEmpty()) {
            val prime = packet.predictionsList.first()
            val userContractPref = _selectedNotifierContract.value ?: "MATCHES"

            pendingBetSymbol = symbol
            pendingBetDigit = prime.digit
            pendingBetContractType = userContractPref
            pendingBetTargetDigit = if (userContractPref == "OVER") {
                if (prime.digit >= 6) 6 else 5
            } else if (userContractPref == "UNDER") {
                if (prime.digit <= 4) 4 else 5
            } else {
                prime.digit
            }
            pendingBetDisplayName = packet.displayName
            pendingBetTicksObserved = 0
        }
    }

    private fun observePracticeBets() {
        viewModelScope.launch {
            db.practiceBetDao().getAllBets().collect { bets ->
                _practiceBets.value = bets
            }
        }
        viewModelScope.launch {
            db.signalHistoryDao().getAllSignalsFlow().collect { historyList ->
                _signalHistory.value = historyList
            }
        }
        viewModelScope.launch {
            try {
                val pending = db.signalHistoryDao().getPendingSignals()
                pendingSignals.addAll(pending)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            delay(2000)
            checkAndSendMorningNotification()
        }
    }

    private fun checkAndSendMorningNotification() {
        val settings = _userSettings.value
        if (settings.isFirstLaunch) return
        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        if (currentDay != lastMorningNotificationDay) {
            lastMorningNotificationDay = currentDay
            triggerMorningEncouragement()
        }
    }

    fun triggerMorningEncouragement() {
        viewModelScope.launch {
            val settings = _userSettings.value
            if (settings.customContract != "ALL") {
                android.util.Log.d("DigitAnalysisViewModel", "Suppressing morning support notification: Custom user settings take precedence.")
                return@launch
            }
            val name = if (settings.traderName.isNotBlank()) settings.traderName else "Trader"
            val encouragements = listOf(
                "Good morning $name! Discipline is the bridge between goals and accomplishment. Have a systematic trading day! 🌅",
                "Good morning $name! The goal of a successful trader is to make the best trades. Money is secondary. 📈",
                "Good morning $name! Focus on the process, not the outcome. Let your algorithm guide you to consistency. 🚀",
                "Good morning $name! Losses are just feedback. Every master was once a beginner. Stay disciplined! 💪",
                "Good morning $name! Patience is key. Wait for your setups, stick to the plan. 💎",
                "Good morning $name! Mindset is everything. Keep your emotions cool and your calculations sharp! 🧠",
                "Good morning $name! Rise and shine. High confidence entries are waiting for those who wait. 🎯",
                "Good morning $name! A trading plan is only as good as your execution. Stick to the system! 📋"
            )
            val selected = encouragements.random()
            ReminderNotificationHelper.showReminderNotification(
                context = context,
                title = "🌅 Morning Motivation Support",
                content = selected,
                traderName = settings.traderName
            )
        }
    }

    fun clearPracticeBets() {
        viewModelScope.launch {
            db.practiceBetDao().clearAllBets()
        }
    }

    fun clearSignalHistory() {
        viewModelScope.launch {
            db.signalHistoryDao().clearHistory()
            pendingSignals.clear()
        }
    }

    private fun processLiveSignalsOnNewTick(symbol: String, incomingDigit: Int) {
        pendingSignals.forEach { signal ->
            if (signal.symbolCode == symbol) {
                signal.ticksObserved++
                if (signal.ticksObserved >= signal.targetTicks) {
                    val barrierNum = signal.barrierValue
                    val won = when (signal.contractType) {
                        "UNDER" -> incomingDigit < barrierNum
                        "OVER" -> incomingDigit > barrierNum
                        "DIFFERS" -> incomingDigit != barrierNum
                        else -> false
                    }
                    signal.isWin = won
                    signal.exitDigit = incomingDigit
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            db.signalHistoryDao().updateSignal(signal)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    pendingSignals.remove(signal)
                }
            }
        }
    }

    fun triggerSingleVibration() {
        try {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createOneShot(150L, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(150L)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("DigitAnalysisViewModel", "Single vibration failed: ${t.message}")
        }
    }

    fun triggerDoubleVibration() {
        try {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createWaveform(longArrayOf(0L, 100L, 100L, 100L), -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0L, 100L, 100L, 100L), -1)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("DigitAnalysisViewModel", "Double vibration failed: ${t.message}")
        }
    }

    fun triggerConflictWarningVibration() {
        try {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val effect = android.os.VibrationEffect.createWaveform(longArrayOf(0L, 40L, 40L, 40L, 40L, 40L), -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0L, 40L, 40L, 40L, 40L, 40L), -1)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("DigitAnalysisViewModel", "Conflict warning vibration failed: ${t.message}")
        }
    }

    fun generateFreshSignal() {
        val resultList = _marketRankings.value
        if (resultList.isEmpty()) return

        // 1. Determine which market symbol to analyze. 
        val currentSym = _selectedSymbol.value
        val targetResult = resultList.firstOrNull { it.symbol == currentSym } ?: resultList.first()
        val symbolCode = targetResult.symbol
        val displayName = targetResult.displayName

        // 2. Fetch history
        val history = wsManager.getHistoryFor(symbolCode)
        if (history.isEmpty()) return

        // Take top 3 predictors representing candidates
        val activePacket = _selectedPacket.value
        val candidates = if (activePacket != null && activePacket.symbol == symbolCode && activePacket.predictionsList.isNotEmpty()) {
            activePacket.predictionsList.take(3).map { it.digit }
        } else {
            val counts = IntArray(10)
            history.forEach { counts[it]++ }
            val sortedWithIndices = counts.mapIndexed { idx, freq -> idx to freq }.sortedBy { it.second }
            sortedWithIndices.take(3).map { it.first }
        }

        if (candidates.isEmpty()) return

        val maxDigit = candidates.maxOrNull() ?: 0
        val minDigit = candidates.minOrNull() ?: 0

        // Determine spatial Zone
        val zone = when {
            maxDigit <= 3 -> "LOWER"
            minDigit >= 6 -> "HIGHER"
            else -> "MID_SCATTERED"
        }

        val settings = _userSettings.value
        val profile = settings.riskProfile

        // Custom config check
        var contractType = "UNDER"
        var barrier = "5"
        var estPayout = "~100%"
        var message = "Target general drift with wide stats cushion protection safety net."
        var probability = 75f

        val customContractSetting = settings.customContract
        if (customContractSetting != "ALL") {
            // Check triggers first
            val topCandidate = candidates.firstOrNull() ?: 0
            val isLowerOdds = (topCandidate % 2 != 0 && topCandidate < 5)
            val isLowerEvens = (topCandidate % 2 == 0 && topCandidate < 5)
            val isHigherOdds = (topCandidate % 2 != 0 && topCandidate >= 5)
            val isHigherEvens = (topCandidate % 2 == 0 && topCandidate >= 5)

            val isCustomTriggerMatched = (isLowerOdds && settings.triggerLowerOdds) ||
                                         (isLowerEvens && settings.triggerLowerEvens) ||
                                         (isHigherOdds && settings.triggerHigherOdds) ||
                                         (isHigherEvens && settings.triggerHigherEvens)

            if (!isCustomTriggerMatched) {
                android.util.Log.d("DigitAnalysisViewModel", "Custom trigger not matched for topCandidate $topCandidate. Skipping custom signal registration.")
                return
            }

            val parts = customContractSetting.split(" ")
            contractType = parts.getOrNull(0) ?: "OVER"
            barrier = parts.getOrNull(1) ?: "3"

            probability = when (contractType) {
                "MATCHES" -> 10f
                "DIFFERS" -> 90f
                "OVER" -> {
                    val bVal = barrier.toIntOrNull() ?: 3
                    (9 - bVal) * 10f
                }
                "UNDER" -> {
                    val bVal = barrier.toIntOrNull() ?: 5
                    bVal * 10f
                }
                else -> 50f
            }

            estPayout = when (contractType) {
                "MATCHES" -> "~900%"
                "DIFFERS" -> "~9.8%"
                "OVER" -> when (barrier.toIntOrNull() ?: 3) {
                    0 -> "~10%"
                    1 -> "~15%"
                    2 -> "~25%"
                    3 -> "~40%"
                    4 -> "~65%"
                    5 -> "~150%"
                    6 -> "~230%"
                    7 -> "~450%"
                    8 -> "~900%"
                    else -> "~100%"
                }
                "UNDER" -> when (barrier.toIntOrNull() ?: 5) {
                    1 -> "~970%"
                    2 -> "~460%"
                    3 -> "~240%"
                    4 -> "~150%"
                    5 -> "~100%"
                    6 -> "~40%"
                    7 -> "~25%"
                    8 -> "~15%"
                    9 -> "~10%"
                    else -> "~100%"
                }
                else -> "~100%"
            }

            val matchedTriggerName = when {
                isLowerOdds -> "LOWER ODDS [Odd < 5]"
                isLowerEvens -> "LOWER EVENS [Even < 5]"
                isHigherOdds -> "HIGHER ODDS [Odd >= 5]"
                else -> "HIGHER EVENS [Even >= 5]"
            }
            message = "Matched Custom Trigger: $matchedTriggerName. Executing customized contract model."
        } else {
            // Standard original zone-based options (OVER, UNDER, DIFFERS)
            val randomChoice = kotlin.random.Random.nextFloat() // To distribute OVER, UNDER, and DIFFERS

            when (zone) {
                "LOWER" -> {
                    if (profile == "RISKY") {
                        if (randomChoice < 0.7f) {
                            contractType = "UNDER"
                            barrier = "1"
                            estPayout = "~970%"
                            message = "🎯 Sniper Mode: Squeezed boundaries. Target extreme bottom edge on digit."
                            probability = 42f
                        } else {
                            contractType = "DIFFERS"
                            barrier = "9"
                            estPayout = "~9.8%"
                            message = "📡 Tactical Option: DIFFERS 9 contract targets cold top edge digits."
                            probability = 91f
                        }
                    } else {
                        if (randomChoice < 0.7f) {
                            contractType = "UNDER"
                            barrier = "4"
                            estPayout = "~100%"
                            message = "🟢 Safety Net: Broad padding. Recommends Under 5 or Under 4 play."
                            probability = 84f
                        } else {
                            contractType = "DIFFERS"
                            barrier = "8"
                            estPayout = "~9.8%"
                            message = "📡 Tactical Option: DIFFERS 8 contract offers maximum cushion safety."
                            probability = 92f
                        }
                    }
                }
                "HIGHER" -> {
                    if (profile == "RISKY") {
                        if (randomChoice < 0.7f) {
                            contractType = "OVER"
                            barrier = "8"
                            estPayout = "~900%"
                            message = "🎯 Sniper Mode: Strike extreme high peaks. Only wins if digit 9 is hit."
                            probability = 40f
                        } else {
                            contractType = "DIFFERS"
                            barrier = "0"
                            estPayout = "~9.8%"
                            message = "📡 Tactical Option: DIFFERS 0 contract targets extreme bottom edge."
                            probability = 91f
                        }
                    } else {
                        if (randomChoice < 0.7f) {
                            contractType = "OVER"
                            barrier = "5"
                            estPayout = "~150%"
                            message = "🟢 Safety Net: Broad margin. Collect comfortable high-probability premiums."
                            probability = 78f
                        } else {
                            contractType = "DIFFERS"
                            barrier = "1"
                            estPayout = "~9.8%"
                            message = "📡 Tactical Option: DIFFERS 1 contract ignores lower segment fluctuations."
                            probability = 92f
                        }
                    }
                }
                "MID_SCATTERED" -> {
                    if (profile == "RISKY") {
                        if (randomChoice < 0.40f) {
                            contractType = "UNDER"
                            barrier = "3"
                            estPayout = "~150%"
                            message = "🎯 Sniper Mode: Squeezed mid segments. High risk payout edge."
                            probability = 52f
                        } else if (randomChoice < 0.80f) {
                            contractType = "OVER"
                            barrier = "6"
                            estPayout = "~230%"
                            message = "🎯 Sniper Mode: Squeezed mid segments. High risk payout edge."
                            probability = 48f
                        } else {
                            contractType = "DIFFERS"
                            barrier = "5"
                            estPayout = "~9.8%"
                            message = "🎯 Sniper Mode: DIFFERS 5 contract bypasses volatile centroid digits."
                            probability = 90f
                        }
                    } else {
                        if (randomChoice < 0.40f) {
                            contractType = "UNDER"
                            barrier = "6"
                            estPayout = "~40%"
                            message = "🟢 Safety Net: Macro cushions inside scattered clumping trends of digits."
                            probability = 86f
                        } else if (randomChoice < 0.80f) {
                            contractType = "OVER"
                            barrier = "3"
                            estPayout = "~40%"
                            message = "🟢 Safety Net: Macro cushions inside scattered clumping trends of digits."
                            probability = 86f
                        } else {
                            contractType = "DIFFERS"
                            barrier = "4"
                            estPayout = "~9.8%"
                            message = "🟢 Safety Net: DIFFERS 4 contract filters out central noise with massive margin."
                            probability = 91f
                        }
                    }
                }
            }
        }

        val signalId = "SIG-" + System.currentTimeMillis().toString().takeLast(5)
        val freshSignal = LiveTradeSignal(
            id = signalId,
            timestamp = System.currentTimeMillis(),
            symbol = symbolCode,
            displayName = displayName,
            riskProfile = profile,
            contractType = contractType,
            barrier = barrier,
            payoutPct = estPayout,
            candidates = candidates,
            message = message,
            zone = if (zone == "LOWER") "CHILL ZONE" else if (zone == "HIGHER") "SKY HIGH" else "CHOP SHOP",
            probabilityEst = probability
        )
        _activeSignal.value = freshSignal

        val behavior = settings.alertBehavior
        val showNotif = (behavior == "NOTIF_ONLY" || behavior == "VIB_AND_NOTIF")
        val triggerVib = (behavior == "VIB_ONLY" || behavior == "VIB_AND_NOTIF")

        if (showNotif && settings.alarmsEnabled) {
            sendPersonalizedSignal(
                context = context,
                title = "📡 Tactical Signal: ${freshSignal.displayName}",
                content = "Order Recommendation: BUY ${freshSignal.contractType} ${freshSignal.barrier} [${freshSignal.payoutPct} Payout] - ${freshSignal.message}",
                signalDescription = "${freshSignal.displayName} - ${freshSignal.contractType} ${freshSignal.barrier}"
            )
        }

        // Save to signal_history database as pending
        val freshDbSignal = com.example.data.db.SignalHistory(
            timestamp = System.currentTimeMillis(),
            signalId = signalId,
            symbolCode = symbolCode,
            displayName = displayName,
            riskProfile = profile,
            contractType = contractType,
            barrierValue = barrier.toIntOrNull() ?: 5,
            payoutPct = estPayout,
            message = message,
            winDigits = candidates.joinToString(","),
            targetTicks = settings.virtualTradeCloseTicks
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbId = db.signalHistoryDao().insertSignal(freshDbSignal)
                pendingSignals.add(freshDbSignal.copy(id = dbId))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (triggerVib) {
            triggerSingleVibration()
        }
    }

    private fun startSignalsLoop() {
        signalsJob?.cancel()
        signalsJob = viewModelScope.launch(Dispatchers.Default) {
            delay(1200)
            generateFreshSignal()
            while (true) {
                for (sec in 30 downTo 1) {
                    _signalCountdown.value = sec
                    delay(1000)
                }
                generateFreshSignal()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
        alarmJob?.cancel()
        signalsJob?.cancel()
        wsManager.disconnect()
    }
}

data class LiveTradeSignal(
    val id: String,
    val timestamp: Long,
    val symbol: String,
    val displayName: String,
    val riskProfile: String,
    val contractType: String,
    val barrier: String,
    val payoutPct: String,
    val candidates: List<Int>,
    val message: String,
    val zone: String,
    val probabilityEst: Float
)

data class BacktestTx(
    val timestamp: Long,
    val digit: Int,
    val contractType: String,
    val result: String,
    val entryDigit: Int,
    val targetDigit: Int,
    val symbolDisp: String
)
