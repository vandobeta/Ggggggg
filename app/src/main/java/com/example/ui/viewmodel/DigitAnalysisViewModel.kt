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

    // --- AUTO TRADER ENGINE STATES ---
    private val _autoSessionProfit = MutableStateFlow(0.0)
    val autoSessionProfit: StateFlow<Double> = _autoSessionProfit.asStateFlow()

    private val _maxSessionProfit = MutableStateFlow(0.0)
    val maxSessionProfit: StateFlow<Double> = _maxSessionProfit.asStateFlow()

    private val _targetProfitReached = MutableStateFlow(false)
    val targetProfitReached: StateFlow<Boolean> = _targetProfitReached.asStateFlow()
    
    private val _stopLossHit = MutableStateFlow(false)
    val stopLossHit: StateFlow<Boolean> = _stopLossHit.asStateFlow()

    private val crossoverTicksRemaining = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val _crossoverDetected = MutableStateFlow<Boolean>(false)
    val crossoverDetected: StateFlow<Boolean> = _crossoverDetected.asStateFlow()

    private val _marketChoppyBlocked = MutableStateFlow(false)
    val marketChoppyBlocked: StateFlow<Boolean> = _marketChoppyBlocked.asStateFlow()

    private val _entryTriggerAwaiting = MutableStateFlow(false)
    val entryTriggerAwaiting: StateFlow<Boolean> = _entryTriggerAwaiting.asStateFlow()

    private val _dualVectorState = MutableStateFlow<DualVectorState?>(null)
    val dualVectorState: StateFlow<DualVectorState?> = _dualVectorState.asStateFlow()

    private var hasAttemptedStartupAuth = false

    fun resetAutoTraderSession() {
        _autoSessionProfit.value = 0.0
        _maxSessionProfit.value = 0.0
        _targetProfitReached.value = false
        _stopLossHit.value = false
    }

    fun resetDemoBalance(onCompleted: (Boolean, String?) -> Unit) {
        val settings = _userSettings.value
        val token = settings.derivToken
        val accountId = wsManager.authorizedUserId.value
        if (token.isNotEmpty() && !accountId.isNullOrEmpty() && settings.isDemoAccount) {
            wsManager.resetDemoBalance(token, accountId) { success, errMsg ->
                if (success) {
                    viewModelScope.launch {
                        val current = _userSettings.value
                        val updated = current.copy(
                            derivWalletBalance = 10000.0,
                            demoWalletBalance = 10000.0
                        )
                        repository.saveSettings(updated)
                        _userSettings.value = updated
                    }
                }
                onCompleted(success, errMsg)
            }
        } else {
            // Local reset only if no token/demo account authorized
            viewModelScope.launch {
                val current = _userSettings.value
                val updated = current.copy(
                    derivWalletBalance = 1000.0,
                    demoWalletBalance = 10000.0,
                    realWalletBalance = 100.0
                )
                repository.saveSettings(updated)
                _userSettings.value = updated
            }
            onCompleted(true, "Reset locally.")
        }
    }

    private var signalsJob: Job? = null

    // --- PERSISTENT TRIGGERED SIGNALS HISTORY FLOW ---
    private val _signalHistory = MutableStateFlow<List<com.example.data.db.SignalHistory>>(emptyList())
    val signalHistory: StateFlow<List<com.example.data.db.SignalHistory>> = _signalHistory.asStateFlow()

    // --- PERSISTENT TRADE HISTORY WORKFLOW FLOWS ---
    private val _tradeHistory = MutableStateFlow<List<com.example.data.db.TradeHistory>>(emptyList())
    val tradeHistory: StateFlow<List<com.example.data.db.TradeHistory>> = _tradeHistory.asStateFlow()

    val activePendingTrades = java.util.Collections.synchronizedList(mutableListOf<com.example.data.db.TradeHistory>())
    val tradeTicksRemaining = java.util.concurrent.ConcurrentHashMap<Long, Int>()

    private val _navErrorMessage = MutableStateFlow<String?>(null)
    val navErrorMessage: StateFlow<String?> = _navErrorMessage.asStateFlow()

    // Expose authorized states and live token messages to UI screens
    val authorizedScopes: StateFlow<List<String>> = wsManager.authorizedScopes
    val authErrorState: StateFlow<String?> = wsManager.authErrorState
    val authorizedBalance: StateFlow<Double?> = wsManager.authorizedBalance
    val authorizedTraderName: StateFlow<String?> = wsManager.authorizedTraderName
    val authorizedEmail: StateFlow<String?> = wsManager.authorizedEmail
    val authorizedCountry: StateFlow<String?> = wsManager.authorizedCountry
    val authorizedCurrency: StateFlow<String?> = wsManager.authorizedCurrency
    val authorizedUserId: StateFlow<String?> = wsManager.authorizedUserId

    val tradeFeedback: StateFlow<com.example.data.DerivWebSocketManager.TradeFeedback?> = wsManager.tradeFeedbackFlow

    fun dismissTradeFeedback() {
        wsManager.dismissTradeFeedback()
    }

    private val _tokenValidationMessage = MutableStateFlow<String?>(null)
    val tokenValidationMessage: StateFlow<String?> = _tokenValidationMessage.asStateFlow()

    data class TokenValidationState(
        val status: String = "IDLE", // "IDLE", "LOADING", "CHOOSE_ACCOUNT", "SUCCESS", "ERROR"
        val message: String = "",
        val accounts: List<com.example.data.DerivWebSocketManager.DerivApiAccount> = emptyList(),
        val token: String = ""
    )
    private val _tokenValidationState = MutableStateFlow<TokenValidationState?>(null)
    val tokenValidationState: StateFlow<TokenValidationState?> = _tokenValidationState.asStateFlow()

    fun dismissErrorMessage() {
        _navErrorMessage.value = null
    }

    fun setSystemErrorMessage(msg: String) {
        _navErrorMessage.value = msg
    }

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
    private var lastProcessedBacktestSignalId: String? = null

    val connectionState: StateFlow<String> = wsManager.connectionState
    val pingState: StateFlow<Long> = wsManager.pingState
    val tickUpdateFlow: StateFlow<Pair<String, Int>?> = wsManager.tickUpdateFlow

    val liveLogs: StateFlow<List<com.example.data.DerivWebSocketManager.WsLog>> = wsManager.liveLogs
    val activeContracts: StateFlow<List<com.example.data.DerivWebSocketManager.WsContract>> = wsManager.activeContracts
    val realTradeHistory: StateFlow<List<com.example.data.DerivWebSocketManager.WsContract>> = wsManager.realTradeHistory

    fun forceReconnectWebSocket() {
        wsManager.disconnect()
        wsManager.connect()
    }

    fun getHistoryFor(symbol: String): List<Int> {
        return wsManager.getHistoryFor(symbol)
    }

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
        // Enforce strict cache management on app start/crash cleanup: wipe stale ticks
        viewModelScope.launch {
            try {
                android.util.Log.d("DigitAnalysisViewModel", "App open/crash recovery: clearing all stale tick occurrences from SQLite...")
                db.tickOccurrenceDao().clearTicks()
                wsManager.clearTickCaches()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Handle network jitter and websocket reconnect state changes dynamically to prevent calculations pollution
        viewModelScope.launch {
            wsManager.connectionState.collect { state ->
                if (state == "SERVER OFFLINE" || state == "DISCONNECTED" || state == "CONNECTING...") {
                    try {
                        android.util.Log.w("DigitAnalysisViewModel", "Network jitter or websocket disconnected ($state)! Clearing database ticks, local caches, and resetting tick calculations...")
                        db.tickOccurrenceDao().clearTicks()
                        wsManager.clearTickCaches()
                        _unifiedTickState.value = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Synchronously correlate live WS finalized trades with SQLite TradeHistory status update flow
        viewModelScope.launch {
            wsManager.realTradeHistory.collect { contracts ->
                if (contracts.isNotEmpty()) {
                    contracts.forEach { contract ->
                        val finishedStatus = contract.status.trim().uppercase()
                        if (finishedStatus == "WON" || finishedStatus == "LOST" || finishedStatus == "WON_CONTRACT" || finishedStatus == "LOST_CONTRACT") {
                            val mainStatus = if (finishedStatus.contains("WON")) "WIN" else "LOSS"
                            synchronized(activePendingTrades) {
                                val matchIndex = activePendingTrades.indexOfFirst { 
                                    it.symbolCode == contract.symbol && it.status == "PENDING" 
                                }
                                if (matchIndex != -1) {
                                    val pending = activePendingTrades[matchIndex]
                                    val resolvedTrade = pending.copy(
                                        exitPrice = contract.bidPrice,
                                        exitDigit = contract.exitDigit,
                                        profitLoss = contract.profit,
                                        status = mainStatus
                                    )
                                    activePendingTrades.removeAt(matchIndex)
                                    tradeTicksRemaining.remove(pending.id)
                                    // Save the resolution directly in ROOM SQLite
                                    viewModelScope.launch(Dispatchers.IO) {
                                        try {
                                            db.tradeHistoryDao().updateTrade(resolvedTrade)
                                            android.util.Log.d("TradeCorrelationListener", "Interlinked update: Contract ID ${contract.contractId} on ${contract.symbol} solved as $mainStatus. Profit: $${contract.profit}")
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        wsManager.connect()
        observeSettings()
        startObservationLoop()
        startPeriodicAlarmLoop()
        observePracticeBets()
        observeTradeHistory()
        startSignalsLoop()

        // High-frequency signal self-destruction monitor (shadow trade prevention)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val active = _activeSignal.value
                if (active != null) {
                    val age = System.currentTimeMillis() - active.timestamp
                    if (age > 15000L) { // 15 seconds expiry metric
                        _activeSignal.value = null
                        android.util.Log.d("DigitAnalysisViewModel", "Failsafe: Auto co-pilot signal self-destruction triggered. Active signal aged ${age}ms discarded.")
                    }
                }
            }
        }

        // Sync real-time balance from secure WebSocket directly to local DB
        viewModelScope.launch {
            wsManager.authorizedBalance.collect { balance ->
                if (balance != null) {
                    val current = _userSettings.value
                    val isSocketVirtual = wsManager.authorizedIsVirtual.value ?: current.isDemoAccount
                    if (isSocketVirtual) {
                        if (current.demoWalletBalance != balance || current.derivWalletBalance != balance) {
                            val updated = current.copy(demoWalletBalance = balance, derivWalletBalance = balance)
                            repository.saveSettings(updated)
                            _userSettings.value = updated
                        }
                    } else {
                        if (current.realWalletBalance != balance || current.derivWalletBalance != balance) {
                            val updated = current.copy(realWalletBalance = balance, derivWalletBalance = balance)
                            repository.saveSettings(updated)
                            _userSettings.value = updated
                        }
                    }
                }
            }
        }

        // Sync validated isDemoAccount mode from secure WebSocket directly to local DB
        viewModelScope.launch {
            wsManager.authorizedIsVirtual.collect { isVirtual ->
                if (isVirtual != null) {
                    val current = _userSettings.value
                    if (isVirtual == wsManager.preferDemo) {
                        if (current.isDemoAccount != isVirtual) {
                            val updated = current.copy(
                                isDemoAccount = isVirtual,
                                derivWalletBalance = if (isVirtual) current.demoWalletBalance else current.realWalletBalance
                            )
                            repository.saveSettings(updated)
                            _userSettings.value = updated
                        }
                    }
                }
            }
        }

        // Sync validated trader name from secure WebSocket directly to local DB
        viewModelScope.launch {
            wsManager.authorizedTraderName.collect { name ->
                if (!name.isNullOrBlank()) {
                    val current = _userSettings.value
                    if (current.traderName != name) {
                        repository.saveSettings(current.copy(traderName = name))
                    }
                }
            }
        }

        // Sync latest websocket logs to local DB settings under explicit trade_logs field
        viewModelScope.launch {
            wsManager.liveLogs.collect { logs ->
                if (logs.isNotEmpty()) {
                    val latestLogs = logs.takeLast(15).joinToString("\n") { log ->
                        val date = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(log.timestamp))
                        "[$date] [${log.type}] ${log.message}"
                    }
                    val current = _userSettings.value
                    if (current.tradeLogs != latestLogs) {
                        val updated = current.copy(tradeLogs = latestLogs)
                        repository.saveSettings(updated)
                        _userSettings.value = updated
                    }
                }
            }
        }

        // Sync validated currency from secure WebSocket directly to local DB
        viewModelScope.launch {
            wsManager.authorizedCurrency.collect { cur ->
                if (!cur.isNullOrBlank()) {
                    val current = _userSettings.value
                    if (current.currency != cur) {
                        repository.saveSettings(current.copy(currency = cur))
                    }
                }
            }
        }

        // Recover unresolved pending trades from database in the background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val unresolved = db.tradeHistoryDao().getPendingResultTrades()
                synchronized(activePendingTrades) {
                    activePendingTrades.addAll(unresolved)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var lastObservedRiskProfile: String? = null

    private var lastObservedSessionsPerDay: Int? = null

    private fun observeSettings() {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                if (settings != null) {
                    wsManager.preferDemo = settings.isDemoAccount
                    wsManager.activeAppId = settings.derivAppId
                    val oldRiskProfile = lastObservedRiskProfile
                    val oldSessionsCount = lastObservedSessionsPerDay
                    
                    _userSettings.value = settings
                    lastObservedRiskProfile = settings.riskProfile
                    lastObservedSessionsPerDay = settings.sessionsPerDay
                    
                    recalculateAllStates()
                    
                    // Securely auto-authorize saved token on app start/reconnect
                    if (!hasAttemptedStartupAuth && settings.derivToken.isNotEmpty()) {
                        hasAttemptedStartupAuth = true
                        android.util.Log.d("DigitAnalysisViewModel", "🚀 Auto-authorizing on app startup using saved secure token...")
                        wsManager.sendAuthorizeRequest(settings.derivToken)
                    }

                    if (oldRiskProfile != null && oldRiskProfile != settings.riskProfile) {
                        generateFreshSignal()
                        startSignalsLoop()
                    }
                    
                    if (oldSessionsCount == null || oldSessionsCount != settings.sessionsPerDay) {
                        com.example.data.AlarmSchedulerHelper.scheduleSessionAlarms(
                            getApplication(),
                            settings.sessionsPerDay
                        )
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
                    processTradeHistoryOnNewTick(symbol, digit)

                    // Real-time State Distribution: Pass calculated UnifiedTickState to single UI StateFlow container
                    val rawPrice = wsManager.getLastPriceFor(symbol)
                    val settings = _userSettings.value
                    val contractChoice = if (settings.customContract.startsWith("OVER")) "OVER" else "UNDER"

                    val isOneSecond = symbol.startsWith("1HZ")
                    val processor = getProcessorFor(symbol)
                    val unifiedState = processor.processIncomingMarketTick(rawPrice, contractChoice, isOneSecond, cushionSpacing = settings.cushionSpacing)

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
        val oldSettings = _userSettings.value
        
        // Intercept enabling of autoTraderEnabled to check scope and balance
        if (newSettings.autoTraderEnabled && !oldSettings.autoTraderEnabled) {
            val token = newSettings.derivToken
            if (token.isEmpty()) {
                _navErrorMessage.value = "Co-pilot Error: A secure API token is required to initialize automated trading! Update in setup or settings."
                viewModelScope.launch {
                    val corrected = newSettings.copy(autoTraderEnabled = false)
                    repository.saveSettings(corrected)
                    _userSettings.value = corrected
                }
                return
            }

            validateTokenAndInitializeEngine(token, newSettings.isDemoAccount) { success, message ->
                if (!success) {
                    _navErrorMessage.value = "Security Gate: $message"
                    viewModelScope.launch {
                        val corrected = newSettings.copy(autoTraderEnabled = false)
                        repository.saveSettings(corrected)
                        _userSettings.value = corrected
                    }
                } else {
                    _navErrorMessage.value = null // clear prior errors
                    
                    // Force authorize on socket so real-time orders can execute
                    wsManager.sendAuthorizeRequest(token)
                    
                    viewModelScope.launch {
                        val currentDetected = _userSettings.value
                        val mergedSettings = newSettings.copy(
                            isDemoAccount = currentDetected.isDemoAccount,
                            demoWalletBalance = currentDetected.demoWalletBalance,
                            realWalletBalance = currentDetected.realWalletBalance,
                            derivWalletBalance = currentDetected.derivWalletBalance
                        )
                        repository.saveSettings(mergedSettings)
                        _userSettings.value = mergedSettings
                    }
                }
            }
        } else {
            // Standard update
            viewModelScope.launch {
                repository.saveSettings(newSettings)
                _userSettings.value = newSettings
                
                // If real-time token is edited/updated, trigger a fresh secure auth request to keep states fully synced
                if (newSettings.derivToken != oldSettings.derivToken && newSettings.derivToken.isNotEmpty()) {
                    wsManager.sendAuthorizeRequest(newSettings.derivToken)
                } else if (newSettings.isDemoAccount != oldSettings.isDemoAccount || newSettings.currentAccountType != oldSettings.currentAccountType) {
                    if (newSettings.derivToken.isNotEmpty()) {
                        wsManager.preferDemo = newSettings.isDemoAccount
                        wsManager.clearAuthStates()
                        wsManager.disconnect()
                        wsManager.sendAuthorizeRequest(newSettings.derivToken)
                    }
                }
            }
        }
    }

    fun validateTokenAndInitializeEngine(token: String, isDemo: Boolean, onCompleted: (Boolean, String) -> Unit) {
        val cleanToken = token.replace("\n", "").replace("\r", "").replace(" ", "").trim()
        if (cleanToken.isEmpty()) {
            onCompleted(false, "Deriv security token is empty.")
            return
        }

        _tokenValidationState.value = TokenValidationState(status = "LOADING", message = "Querying live options accounts...")
        wsManager.fetchAccountsForToken(cleanToken) { accounts, errorMsg ->
            if (accounts != null && accounts.isNotEmpty()) {
                _tokenValidationState.value = TokenValidationState(
                    status = "CHOOSE_ACCOUNT",
                    accounts = accounts,
                    token = cleanToken
                )
                onCompleted(true, "Successfully retrieved ${accounts.size} options accounts.")
            } else {
                val errorText = errorMsg ?: "No suitable Options accounts found."
                _tokenValidationState.value = TokenValidationState(
                    status = "ERROR",
                    message = errorText
                )
                onCompleted(false, errorText)
            }
        }
    }

    fun chooseAccountAndInitialize(token: String, account: com.example.data.DerivWebSocketManager.DerivApiAccount, onCompleted: (Boolean, String) -> Unit = { _, _ -> }) {
        _tokenValidationState.value = TokenValidationState(status = "LOADING", message = "Establishing authorized channel to ${account.accountId}...")
        viewModelScope.launch {
            val isVirtual = account.accountType == "demo"
            wsManager.preferDemo = isVirtual
            
            // Connect and validate WebSocket authorization first
            wsManager.sendAuthorizeRequest(token)
            
            var validated = false
            var responseMessage = "Token authorization check timed out. Please check your connection."
            var delayCount = 0

            while (delayCount < 35) {
                delay(500)
                val connectionState = wsManager.connectionState.value
                val scopes = wsManager.authorizedScopes.value
                val balance = wsManager.authorizedBalance.value
                val errorMsg = wsManager.authErrorState.value

                if (connectionState == "AUTHORIZED") {
                    val hasRead = scopes.any { it.equals("read", ignoreCase = true) }
                    val hasTrade = scopes.any { it.equals("trade", ignoreCase = true) }

                    if (!hasRead || !hasTrade) {
                        validated = false
                        responseMessage = "Scope validation failed! Read and trade permissions are required."
                    } else {
                        validated = true
                        val finalBalance = balance ?: account.balance
                        responseMessage = "Success! Loaded account ${account.accountId} with balance of $${finalBalance}."
                        
                        try {
                            val current = _userSettings.value
                            val updated = current.copy(
                                derivToken = token,
                                isDemoAccount = isVirtual,
                                currentAccountType = if (isVirtual) "demo" else "real",
                                demoWalletBalance = if (isVirtual) finalBalance else current.demoWalletBalance,
                                realWalletBalance = if (!isVirtual) finalBalance else current.realWalletBalance,
                                derivWalletBalance = finalBalance,
                                currency = account.currency,
                                traderName = account.name
                            )
                            repository.saveSettings(updated)
                            _userSettings.value = updated
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    break
                } else if (connectionState == "AUTH_FAILED" || errorMsg != null) {
                    validated = false
                    responseMessage = errorMsg ?: "Deriv authorization credentials rejected."
                    break
                }
                delayCount++
            }
            
            if (validated) {
                _tokenValidationState.value = TokenValidationState(status = "SUCCESS", message = responseMessage)
            } else {
                _tokenValidationState.value = TokenValidationState(status = "ERROR", message = responseMessage)
            }
            onCompleted(validated, responseMessage)
        }
    }

    fun dismissTokenValidationState() {
        _tokenValidationState.value = null
    }

    fun clearAuthStates() {
        wsManager.clearAuthStates()
    }

    /**
     * Diagnostic token function that directly tests the provided PAT against the Deriv authorize API call
     * and returns the exact API response or raw error JSON.
     */
    fun validateToken(token: String, onCompleted: (Boolean, String?) -> Unit) {
        val cleanToken = token.replace("\n", "").replace("\r", "").replace(" ", "").trim()
        wsManager.validateToken(cleanToken, onCompleted)
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
        val settings = _userSettings.value
        val packetsPrepared = mutableMapOf<String, CompleteDataPacket>()

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
                packetsPrepared[symbol] = createEmptyPacket(symbol, displayName)
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
            val previousEven = previousEvenDominance[symbol]
            val crossoverHappened = previousEven != null && previousEven != currentlyEven
            previousEvenDominance[symbol] = currentlyEven

            if (crossoverHappened) {
                crossoverTicksRemaining[symbol] = 8
                if (symbol == _selectedSymbol.value) {
                    _crossoverDetected.value = true
                    triggerDoubleVibration() // Entry vibration for crossover
                }
            } else {
                val remaining = crossoverTicksRemaining[symbol] ?: 0
                if (remaining > 0) {
                    crossoverTicksRemaining[symbol] = remaining - 1
                } else if (symbol == _selectedSymbol.value) {
                    _crossoverDetected.value = false
                }
            }

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

            // Formulate recommended contract
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

            val noiseScore = if (microEvenVelocity in 21f..79f) 85f else 15f
            val stabilityScore = (100f - abs((leWeight + heWeight) - 50f) * 2f).coerceIn(0f, 100f)

            packetsPrepared[symbol] = CompleteDataPacket(
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
                isStableConnection = wsManager.connectionState.value == "CONNECTED" || wsManager.connectionState.value == "AUTHORIZED"
            )
        }

        val rankedResults = results.sortedByDescending { it.totalEdgeScore }
        _marketRankings.value = rankedResults

        // Automatically select the highest-scoring volatility symbol
        val topRankingSymbol = rankedResults.firstOrNull()?.symbol ?: _selectedSymbol.value
        _selectedSymbol.value = topRankingSymbol

        _selectedPacket.value = packetsPrepared[topRankingSymbol]

        val currentSym = _selectedSymbol.value
        val history = wsManager.getHistoryFor(currentSym)
        if (history.isNotEmpty()) {
            val historyLimit = history.takeLast(30)
            val size = historyLimit.size
            if (size >= 10) {
                val halfSize = size / 2
                val recentHalf = historyLimit.takeLast(halfSize)
                val priorHalf = historyLimit.dropLast(halfSize).takeLast(halfSize)

                val recentEvenCount = recentHalf.count { it % 2 == 0 }
                val priorEvenCount = priorHalf.count { it % 2 == 0 }
                val recentEvenPct = (recentEvenCount.toFloat() / halfSize.toFloat()) * 100f
                val priorEvenPct = (priorEvenCount.toFloat() / priorHalf.size.toFloat()) * 100f
                val evenDelta = recentEvenPct - priorEvenPct

                val recentOddCount = recentHalf.count { it % 2 != 0 }
                val priorOddCount = priorHalf.count { it % 2 != 0 }
                val recentOddPct = (recentOddCount.toFloat() / halfSize.toFloat()) * 100f
                val priorOddPct = (priorOddCount.toFloat() / priorHalf.size.toFloat()) * 100f
                val oddDelta = recentOddPct - priorOddPct

                var evenWeightedSum = 0f
                var oddWeightedSum = 0f
                var totalWeightsSum = 0f
                
                for (i in 0 until size) {
                    val weight = (i + 1).toFloat()
                    totalWeightsSum += weight
                    if (historyLimit[i] % 2 == 0) {
                        evenWeightedSum += weight
                    } else {
                        oddWeightedSum += weight
                    }
                }
                
                val evenMomentum = if (totalWeightsSum > 0) (evenWeightedSum / totalWeightsSum) * 100f else 50f
                val oddMomentum = if (totalWeightsSum > 0) (oddWeightedSum / totalWeightsSum) * 100f else 50f

                val evenPercentage = (historyLimit.count { it % 2 == 0 }.toFloat() / size.toFloat()) * 100f
                val oddPercentage = (historyLimit.count { it % 2 != 0 }.toFloat() / size.toFloat()) * 100f

                val evenDominates = evenPercentage > 52f && evenMomentum > oddMomentum
                val oddDominates = oddPercentage > 52f && oddMomentum > evenMomentum
                val dominantSide = when {
                    evenDominates -> "EVENS"
                    oddDominates -> "ODDS"
                    else -> "NONE"
                }

                val evenTrigger = evenDelta > 0f && evenMomentum > oddMomentum && oddDelta < 0f
                val oddTrigger = oddDelta > 0f && oddMomentum > evenMomentum && evenDelta < 0f
                
                val triggerPassed = evenTrigger || oddTrigger
                val triggerDir = when {
                    evenTrigger -> "EVEN"
                    oddTrigger -> "ODD"
                    else -> "NONE"
                }

                _dualVectorState.value = DualVectorState(
                    evenVector = ParityVector(
                        percentage = evenPercentage,
                        delta = evenDelta,
                        momentum = evenMomentum,
                        isDominating = evenDominates
                    ),
                    oddVector = ParityVector(
                        percentage = oddPercentage,
                        delta = oddDelta,
                        momentum = oddMomentum,
                        isDominating = oddDominates
                    ),
                    dominantSide = dominantSide,
                    entryTriggerPassed = triggerPassed,
                    triggerDirection = triggerDir
                )
            } else {
                _dualVectorState.value = null
            }
        } else {
            _dualVectorState.value = null
        }
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

            // High occurrence percentage (countPct) contributes POSITIVELY to confidence!
            val occurrenceBonus = countPct * 3.5f

            // Quadrant weight contributes POSITIVELY to confidence!
            val quadrantWeight = when (digit) {
                0, 2, 4 -> leWeight
                1, 3 -> loWeight
                6, 8 -> heWeight
                else -> hoWeight
            }
            val quadBonus = quadrantWeight * 0.7f

            val isEven = (digit % 2 == 0)
            val parityImbalance = abs(macroEvenPct - 50f)
            val parityBonus = if (isEven == evenIsDominating) {
                (parityImbalance / 50f) * 15f
            } else {
                -5f
            }

            // Momentum bonus: matching micro velocity (micro trend)
            val microEvenDominating = microEvenVelocity > 50f
            val momentumBonus = if (isEven == microEvenDominating) {
                10f
            } else {
                0f
            }

            val finalConfidence = (35f + occurrenceBonus + quadBonus + parityBonus + momentumBonus).coerceIn(5f, 99f)

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

        // Place next bet on active symbol (only 1 backtest bet allowed per 30-second signal)
        val packet = _selectedPacket.value
        val activeSignalId = _activeSignal.value?.id
        if (pendingBetSymbol == null && activeSignalId != null && activeSignalId != lastProcessedBacktestSignalId && packet != null && packet.symbol == symbol && packet.predictionsList.isNotEmpty()) {
            val prime = packet.predictionsList.first()
            val userContractPref = _selectedNotifierContract.value ?: "MATCHES"

            lastProcessedBacktestSignalId = activeSignalId
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
                signal.observedTicks = if (signal.observedTicks.isEmpty()) {
                    incomingDigit.toString()
                } else {
                    "${signal.observedTicks},$incomingDigit"
                }
                
                if (signal.ticksObserved >= signal.targetTicks) {
                    val barrierNum = signal.barrierValue
                    val won = when (signal.contractType) {
                        "UNDER" -> incomingDigit < barrierNum
                        "OVER" -> incomingDigit > barrierNum
                        "DIFFERS" -> incomingDigit != barrierNum
                        "EVEN", "DIGITEVEN" -> incomingDigit % 2 == 0
                        "ODD", "DIGITODD" -> incomingDigit % 2 != 0
                        else -> false
                    }
                    signal.isWin = won
                    signal.exitDigit = incomingDigit

                    // Process Auto Trader Results
                    val settings = _userSettings.value
                    if (settings.autoTraderEnabled) {
                        val actualStake = if (settings.autoTraderCompoundingStake) {
                            (settings.derivWalletBalance * 0.01).coerceAtLeast(1.0)
                        } else {
                            settings.autoTraderStake
                        }

                        val cleanPayoutStr = signal.payoutPct.replace("~", "").replace("%", "")
                        val payoutPercent = cleanPayoutStr.toDoubleOrNull() ?: 100.0
                        val payoutMultiplier = payoutPercent / 100.0

                        val tradeProfit = if (won) (actualStake * payoutMultiplier) else (-actualStake)
                        val newSessionProfit = _autoSessionProfit.value + tradeProfit
                        val newBalance = settings.derivWalletBalance + tradeProfit

                        _autoSessionProfit.value = newSessionProfit
                        _maxSessionProfit.value = maxOf(_maxSessionProfit.value, newSessionProfit)

                        var isTPReached = false
                        var isSLHit = false

                        if (newSessionProfit >= settings.autoTraderTakeProfit) {
                            isTPReached = true
                            _targetProfitReached.value = true
                        }

                        if (settings.autoTraderTrailingStopLoss) {
                            if (newSessionProfit < _maxSessionProfit.value - settings.autoTraderStopLoss) {
                                isSLHit = true
                                _stopLossHit.value = true
                            }
                        } else {
                            if (newSessionProfit <= -settings.autoTraderStopLoss) {
                                isSLHit = true
                                _stopLossHit.value = true
                            }
                        }

                        val shouldDisableAuto = isTPReached || isSLHit
                        val updatedSettings = settings.copy(
                            derivWalletBalance = newBalance,
                            autoTraderEnabled = if (shouldDisableAuto) false else settings.autoTraderEnabled
                        )

                        viewModelScope.launch {
                            try {
                                repository.saveSettings(updatedSettings)
                                _userSettings.value = updatedSettings
                                if (won) {
                                    triggerDoubleVibration()
                                } else {
                                    triggerConflictWarningVibration()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            db.signalHistoryDao().updateSignal(signal)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    pendingSignals.remove(signal)
                } else {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            db.signalHistoryDao().updateSignal(signal)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun shouldSkipFeedback(): Boolean {
        try {
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
            if (audioManager != null) {
                val mode = audioManager.ringerMode
                return mode == android.media.AudioManager.RINGER_MODE_SILENT || mode == android.media.AudioManager.RINGER_MODE_VIBRATE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun triggerSingleVibration() {
        if (shouldSkipFeedback()) return
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
        if (shouldSkipFeedback()) return
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
        if (shouldSkipFeedback()) return
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

        val activePacket = _selectedPacket.value
        val stability = if (activePacket != null && activePacket.symbol == symbolCode) activePacket.stabilityScore else 50f
        val isChoppy = stability < 40f
        _marketChoppyBlocked.value = isChoppy
        
        if (isChoppy) {
            _activeSignal.value = null
            _entryTriggerAwaiting.value = false
            android.util.Log.d("DigitAnalysisViewModel", "Safety Filter: Market is choppy ($stability%). Skipping signal.")
            return
        }

        // Parity gate checker: Only trigger a signal if Odd vs Even percentage gap is > 20%
        val totalTicks = history.size
        val evenCount = history.count { it % 2 == 0 }
        val oddCount = totalTicks - evenCount
        val evenPct = (evenCount.toFloat() / totalTicks.coerceAtLeast(1)) * 100f
        val oddPct = (oddCount.toFloat() / totalTicks.coerceAtLeast(1)) * 100f
        val parityDiff = kotlin.math.abs(evenPct - oddPct)
        
        if (parityDiff <= 20f) {
            _activeSignal.value = null
            _entryTriggerAwaiting.value = false
            android.util.Log.d("DigitAnalysisViewModel", "Parity Gap Filter: Odd to Even difference is too narrow (${String.format("%.1f%%", parityDiff)} <= 20%). Skipping signal.")
            return
        }

        val vecState = _dualVectorState.value
        val hasEntryTrigger = vecState != null && vecState.entryTriggerPassed
        
        _entryTriggerAwaiting.value = !hasEntryTrigger
        if (!hasEntryTrigger) {
            _activeSignal.value = null // Discard active signal immediately on entry failure
            android.util.Log.d("DigitAnalysisViewModel", "Entry Trigger Filter: Dual vector condition not met. Awaiting convergence...")
            return
        }

        // Take top 3 predictors representing candidates
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
            // Standard system qualification rules (Low, Mid, High Divisions)
            val anchor = candidates.getOrNull(0) ?: 0
            val maxDigit = candidates.maxOrNull() ?: 0
            val minDigit = candidates.minOrNull() ?: 0
            val span = maxDigit - minDigit

            when {
                // 📉 Lows Division: {0, 1, 2, 3} -> Evaluates DIGITUNDER
                anchor in 0..3 -> {
                    if (span <= 4) {
                        val proposedBarrier = maxDigit + 2
                        if (proposedBarrier == 0) {
                            // Catch UNDER 0: redirect to DIGITDIFF and pick untouched cold digit
                            contractType = "DIFFERS"
                            val diffBarrier = getDigitDiffBarrier(candidates, anchor)
                            barrier = diffBarrier.toString()
                            estPayout = "~9.8%"
                            message = "🔄 Interceptor Catch UNDER 0: Ignored invalid boundary. Redirected to DIGITDIFF secure anchor separation at $barrier."
                            probability = 91f
                        } else {
                            contractType = "UNDER"
                            barrier = proposedBarrier.coerceIn(0, 9).toString()
                            estPayout = when (proposedBarrier) {
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
                            message = "📉 Digit Under Qualified: Primary anchor $anchor in Lows division with safe cushion padding above maximum candidate $maxDigit."
                            probability = proposedBarrier * 10f
                        }
                    } else {
                        // High-Span Sniper Pivot: exceeds safe threshold
                        contractType = "DIFFERS"
                        val diffBarrier = getDigitDiffBarrier(candidates, anchor)
                        barrier = diffBarrier.toString()
                        estPayout = "~9.8%"
                        message = "🎯 High-Span Sniper Pivot: Span $span exceeds SAFE threshold. Shifted to secure DIGITDIFF contract at $barrier."
                        probability = 91f
                    }
                }
                
                // 📈 Highs Division: {7, 8, 9} -> Evaluates DIGITOVER
                anchor in 7..9 -> {
                    if (span <= 4) {
                        val proposedBarrier = minDigit - 2
                        if (proposedBarrier >= 9) {
                            // Catch OVER 9: redirect to DIGITDIFF and pick untouched cold digit
                            contractType = "DIFFERS"
                            val diffBarrier = getDigitDiffBarrier(candidates, anchor)
                            barrier = diffBarrier.toString()
                            estPayout = "~9.8%"
                            message = "🔄 Interceptor Catch OVER 9: Ignored invalid boundary. Redirected to DIGITDIFF secure anchor separation at $barrier."
                            probability = 91f
                        } else {
                            contractType = "OVER"
                            barrier = proposedBarrier.coerceIn(0, 9).toString()
                            estPayout = when (proposedBarrier) {
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
                            message = "📈 Digit Over Qualified: Primary anchor $anchor in Highs division with safe cushion padding below minimum candidate $minDigit."
                            probability = (9 - proposedBarrier) * 10f
                        }
                    } else {
                        // High-Span Sniper Pivot: exceeds safe threshold
                        contractType = "DIFFERS"
                        val diffBarrier = getDigitDiffBarrier(candidates, anchor)
                        barrier = diffBarrier.toString()
                        estPayout = "~9.8%"
                        message = "🎯 High-Span Sniper Pivot: Span $span exceeds SAFE threshold. Shifted to secure DIGITDIFF contract at $barrier."
                        probability = 91f
                    }
                }

                // ↔️ Mids Division: {4, 5, 6} -> Governed exclusively by center channel parity-hunting
                else -> {
                    val switchingToEven = targetResult.microEvenVelocity > 50f
                    val oddsAreMany = targetResult.macroEvenPct < 50f

                    if (switchingToEven && oddsAreMany) {
                        // Switching to even, but odds dominate: snap-back to ODD
                        contractType = "ODD"
                        barrier = "0"
                        estPayout = "~95%"
                        message = "↔️ Spring Elasticity Matrix Parity Lock: Market switching to Even, but Odds dominate. Elastic snap-back executes ODD contract."
                        probability = 50f
                    } else if (!switchingToEven && !oddsAreMany) {
                        // Switching to odd, but evens dominate: snap-back to EVEN
                        contractType = "EVEN"
                        barrier = "0"
                        estPayout = "~95%"
                        message = "↔️ Spring Elasticity Matrix Parity Lock: Market switching to Odd, but Evens dominate. Elastic snap-back executes EVEN contract."
                        probability = 50f
                    } else {
                        // Normal shifting momentum
                        if (switchingToEven) {
                            contractType = "EVEN"
                            barrier = "0"
                            estPayout = "~95%"
                            message = "↔️ Spring Elasticity Matrix: Pure localized shift momentum locks EVEN parity hunter contract."
                            probability = 50f
                        } else {
                            contractType = "ODD"
                            barrier = "0"
                            estPayout = "~95%"
                            message = "↔️ Spring Elasticity Matrix: Pure localized shift momentum locks ODD parity hunter contract."
                            probability = 50f
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
            probabilityEst = probability,
            crossoverActive = (crossoverTicksRemaining[symbolCode] ?: 0) > 0
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

        // Deploy Auto Trader actual buy orders to live/demo Deriv system if enabled
        if (settings.autoTraderEnabled) {
            val computedStake = if (settings.autoTraderCompoundingStake) {
                ((if (settings.isDemoAccount) settings.demoWalletBalance else settings.realWalletBalance) * 0.01).coerceAtLeast(1.0)
            } else {
                settings.autoTraderStake
            }

            val rawPrice = wsManager.getLastPriceFor(symbolCode) ?: 1.0
            val priceStr = rawPrice.toString()
            val entryDigitVal = priceStr.substring(priceStr.length - 1).toIntOrNull() ?: 0

            val automatedPendingTrade = com.example.data.db.TradeHistory(
                timestamp = System.currentTimeMillis(),
                symbolCode = symbolCode,
                displayName = displayName,
                contractType = contractType,
                barrierValue = barrier.toIntOrNull() ?: 5,
                tradeType = "AUTOMATED",
                accountType = if (settings.isDemoAccount) "DEMO" else "REAL",
                stake = computedStake,
                entryPrice = rawPrice,
                entryDigit = entryDigitVal,
                status = "PENDING"
            )

            viewModelScope.launch {
                try {
                    val dbId = kotlinx.coroutines.withContext(Dispatchers.IO) {
                        db.tradeHistoryDao().insertTrade(automatedPendingTrade)
                    }
                    synchronized(activePendingTrades) {
                        activePendingTrades.add(automatedPendingTrade.copy(id = dbId))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (settings.derivToken.isNotEmpty()) {
                wsManager.sendBuyRequest(
                    symbol = symbolCode,
                    contractType = contractType,
                    barrier = barrier,
                    stake = computedStake,
                    durationTicks = settings.virtualTradeCloseTicks
                )
            }
        }

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

    private fun observeTradeHistory() {
        viewModelScope.launch {
            db.tradeHistoryDao().getAllTradesFlow().collect { trades ->
                _tradeHistory.value = trades
            }
        }
    }

    fun clearTradeHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.tradeHistoryDao().clearAllTrades()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleAccountType() {
        viewModelScope.launch {
            try {
                val currentSettings = _userSettings.value
                val nextDemoMode = !currentSettings.isDemoAccount
                val updatedSettings = currentSettings.copy(
                    isDemoAccount = nextDemoMode,
                    currentAccountType = if (nextDemoMode) "demo" else "real",
                    derivWalletBalance = if (nextDemoMode) currentSettings.demoWalletBalance else currentSettings.realWalletBalance
                )
                
                // Explicitly set preferred mode first to prevent race condition
                wsManager.preferDemo = nextDemoMode
                
                repository.saveSettings(updatedSettings)
                _userSettings.value = updatedSettings
                
                // Trigger dynamic swap on secure WebSocket
                if (updatedSettings.derivToken.isNotEmpty()) {
                    wsManager.sendAuthorizeRequest(updatedSettings.derivToken)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun executeManualTrade(
        symbolCode: String,
        displayName: String,
        contractType: String,
        barrier: Int,
        stake: Double
    ) {
        viewModelScope.launch {
            try {
                val settings = _userSettings.value
                
                // Balance protection check
                val currentBalance = if (settings.isDemoAccount) settings.demoWalletBalance else settings.realWalletBalance
                if (currentBalance < stake) {
                    _navErrorMessage.value = "Insufficient funds! Balance: $${String.format("%.2f", currentBalance)} | Stake: $${String.format("%.2f", stake)}"
                    triggerConflictWarningVibration()
                    return@launch
                }

                val currentPrice = wsManager.getLastPriceFor(symbolCode)
                val finalEntryPrice = if (currentPrice > 0.0) currentPrice else 1.0
                val priceStr = finalEntryPrice.toString()
                val entryDigitVal = priceStr.substring(priceStr.length - 1).toIntOrNull() ?: 0

                val freshManualTrade = com.example.data.db.TradeHistory(
                    timestamp = System.currentTimeMillis(),
                    symbolCode = symbolCode,
                    displayName = displayName,
                    contractType = contractType,
                    barrierValue = barrier,
                    tradeType = "MANUAL",
                    accountType = if (settings.isDemoAccount) "DEMO" else "REAL",
                    stake = stake,
                    entryPrice = finalEntryPrice,
                    entryDigit = entryDigitVal,
                    status = "PENDING"
                )

                val dbId = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    db.tradeHistoryDao().insertTrade(freshManualTrade)
                }

                synchronized(activePendingTrades) {
                    activePendingTrades.add(freshManualTrade.copy(id = dbId))
                }

                if (settings.derivToken.isNotEmpty()) {
                    wsManager.sendBuyRequest(
                        symbol = symbolCode,
                        contractType = contractType,
                        barrier = barrier.toString(),
                        stake = stake,
                        durationTicks = settings.virtualTradeCloseTicks
                    )
                }
                
                triggerDoubleVibration()
            } catch (e: Exception) {
                _navErrorMessage.value = "Manual trade execution failed: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    private fun getUnderPayoutFor(barrier: Int): String {
        return when (barrier) {
            1 -> "~970%"
            2 -> "~400%"
            3 -> "~233%"
            4 -> "~150%"
            5 -> "~100%"
            6 -> "~66%"
            7 -> "~42%"
            8 -> "~25%"
            9 -> "~11%"
            else -> "~100%"
        }
    }

    private fun getOverPayoutFor(barrier: Int): String {
        return when (barrier) {
            0 -> "~11%"
            1 -> "~25%"
            2 -> "~42%"
            3 -> "~66%"
            4 -> "~100%"
            5 -> "~150%"
            6 -> "~233%"
            7 -> "~400%"
            8 -> "~970%"
            else -> "~100%"
        }
    }

    fun processTradeHistoryOnNewTick(symbol: String, incomingDigit: Int) {
        synchronized(activePendingTrades) {
            val iterator = activePendingTrades.iterator()
            while (iterator.hasNext()) {
                val trade = iterator.next()
                if (trade.symbolCode == symbol) {
                    val ticksPassed = (tradeTicksRemaining[trade.id] ?: 0) + 1
                    tradeTicksRemaining[trade.id] = ticksPassed
                    
                    val settings = _userSettings.value
                    val targetTicks = settings.virtualTradeCloseTicks.coerceAtLeast(1)
                    if (ticksPassed >= targetTicks) {
                        // Gather clean exit prices and exit digits
                        val rawPrice = wsManager.getLastPriceFor(symbol)
                        val finalExitPrice = if (rawPrice > 0.0) rawPrice else (trade.entryPrice + 0.1)
                        val cleanPriceStr = finalExitPrice.toString()
                        val exitDigitVal = cleanPriceStr.substring(cleanPriceStr.length - 1).toIntOrNull() ?: incomingDigit
                        
                        // Compute outcome won state
                        val won = when (trade.contractType) {
                            "UNDER" -> exitDigitVal < trade.barrierValue
                            "OVER" -> exitDigitVal > trade.barrierValue
                            "DIFFERS" -> exitDigitVal != trade.barrierValue
                            "EVEN", "DIGITEVEN" -> exitDigitVal % 2 == 0
                            "ODD", "DIGITODD" -> exitDigitVal % 2 != 0
                            "MATCHES" -> exitDigitVal == trade.barrierValue
                            else -> false
                        }
                        
                        // Calculate payout pct & profitLoss
                        val payoutPctStr = if (trade.contractType == "UNDER") {
                            getUnderPayoutFor(trade.barrierValue)
                        } else if (trade.contractType == "OVER") {
                            getOverPayoutFor(trade.barrierValue)
                        } else if (trade.contractType == "DIFFERS") {
                            "~11.0%"
                        } else {
                            "~100.0%"
                        }
                        
                        val cleanPayoutNum = payoutPctStr.replace("~", "").replace("%", "").toDoubleOrNull() ?: 100.0
                        val payoutMultiplier = cleanPayoutNum / 100.0
                        val profitLossVal = if (won) (trade.stake * payoutMultiplier) else (-trade.stake)
                        
                        val resolvedTrade = trade.copy(
                            exitPrice = finalExitPrice,
                            exitDigit = exitDigitVal,
                            profitLoss = profitLossVal,
                            status = if (won) "WIN" else "LOSS"
                        )
                        
                        // Persist resolved trade
                        viewModelScope.launch(Dispatchers.IO) {
                            try {
                                db.tradeHistoryDao().updateTrade(resolvedTrade)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        // Synchronize settings balances based on demo or real mode!
                        val updatedSettings = if (resolvedTrade.accountType == "DEMO") {
                            val newDemoBal = settings.demoWalletBalance + profitLossVal
                            settings.copy(
                                demoWalletBalance = newDemoBal,
                                derivWalletBalance = if (settings.isDemoAccount) newDemoBal else settings.derivWalletBalance
                            )
                        } else {
                            val newRealBal = settings.realWalletBalance + profitLossVal
                            settings.copy(
                                realWalletBalance = newRealBal,
                                derivWalletBalance = if (!settings.isDemoAccount) newRealBal else settings.derivWalletBalance
                            )
                        }
                        
                        viewModelScope.launch {
                            try {
                                repository.saveSettings(updatedSettings)
                                _userSettings.value = updatedSettings
                                if (won) {
                                    triggerDoubleVibration()
                                } else {
                                    triggerConflictWarningVibration()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        
                        iterator.remove()
                        tradeTicksRemaining.remove(trade.id)
                    }
                }
            }
        }
    }

    private fun getDigitDiffBarrier(candidates: List<Int>, anchor: Int): Int {
        var bestDigit = -1
        var maxDistance = -1
        for (digit in 0..9) {
            if (digit !in candidates) {
                val dist = Math.abs(digit - anchor)
                if (dist > maxDistance) {
                    maxDistance = dist
                    bestDigit = digit
                }
            }
        }
        return if (bestDigit != -1) bestDigit else 9
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
    val probabilityEst: Float,
    val crossoverActive: Boolean = false
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

data class ParityVector(
    val percentage: Float,
    val delta: Float,
    val momentum: Float,
    val isDominating: Boolean
)

data class DualVectorState(
    val evenVector: ParityVector,
    val oddVector: ParityVector,
    val dominantSide: String,
    val entryTriggerPassed: Boolean,
    val triggerDirection: String
)
