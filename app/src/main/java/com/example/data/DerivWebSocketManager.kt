package com.example.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class DerivWebSocketManager {

    companion object {
        private const val TAG = "DerivWebSocketManager"
        private const val WS_URL = "wss://ws.binaryws.com/websockets/v3?app_id=1089"
        
        val VOLATILITY_SYMBOLS = listOf(
            "1HZ10V" to "Volatility 10 (1S)",
            "1HZ25V" to "Volatility 25 (1S)",
            "1HZ50V" to "Volatility 50 (1S)",
            "1HZ100V" to "Volatility 100 (1S)",
            "R_10" to "Volatility 10",
            "R_25" to "Volatility 25",
            "R_50" to "Volatility 50",
            "R_75" to "Volatility 75",
            "R_100" to "Volatility 100"
        )
    }

    private val client = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Thread-safe history storage for each symbol
    private val histories = ConcurrentHashMap<String, MutableList<Int>>()
    private val lastPrices = ConcurrentHashMap<String, Double>()

    // States
    private val _connectionState = MutableStateFlow("DISCONNECTED")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _pingState = MutableStateFlow(0L)
    val pingState: StateFlow<Long> = _pingState.asStateFlow()

    private val _tickUpdateFlow = MutableStateFlow<Pair<String, Int>?>(null)
    val tickUpdateFlow: StateFlow<Pair<String, Int>?> = _tickUpdateFlow.asStateFlow()

    private val _authorizedBalance = MutableStateFlow<Double?>(null)
    val authorizedBalance: StateFlow<Double?> = _authorizedBalance.asStateFlow()

    private val _authorizedTraderName = MutableStateFlow<String?>(null)
    val authorizedTraderName: StateFlow<String?> = _authorizedTraderName.asStateFlow()

    private val _authorizedEmail = MutableStateFlow<String?>(null)
    val authorizedEmail: StateFlow<String?> = _authorizedEmail.asStateFlow()

    private val _authorizedCountry = MutableStateFlow<String?>(null)
    val authorizedCountry: StateFlow<String?> = _authorizedCountry.asStateFlow()

    private val _authorizedCurrency = MutableStateFlow<String?>(null)
    val authorizedCurrency: StateFlow<String?> = _authorizedCurrency.asStateFlow()

    private val _authorizedUserId = MutableStateFlow<String?>(null)
    val authorizedUserId: StateFlow<String?> = _authorizedUserId.asStateFlow()

    private val _authorizedScopes = MutableStateFlow<List<String>>(emptyList())
    val authorizedScopes: StateFlow<List<String>> = _authorizedScopes.asStateFlow()

    private val _authErrorState = MutableStateFlow<String?>(null)
    val authErrorState: StateFlow<String?> = _authErrorState.asStateFlow()

    // --- DERIV METRICS & LIVE TRADING DATA ---
    data class WsLog(val id: Long, val timestamp: Long, val message: String, val type: String) // "INFO", "ERROR", "OUTBOUND", "INBOUND"
    data class WsContract(
        val contractId: Long,
        val buyPrice: Double,
        val contractType: String,
        val symbol: String,
        val status: String = "OPEN", // "OPEN", "WON", "LOST"
        val profit: Double = 0.0,
        val bidPrice: Double = 0.0,
        val exitDigit: Int? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _liveLogs = MutableStateFlow<List<WsLog>>(emptyList())
    val liveLogs: StateFlow<List<WsLog>> = _liveLogs.asStateFlow()

    private val _activeContracts = MutableStateFlow<List<WsContract>>(emptyList())
    val activeContracts: StateFlow<List<WsContract>> = _activeContracts.asStateFlow()

    private val _realTradeHistory = MutableStateFlow<List<WsContract>>(emptyList())
    val realTradeHistory: StateFlow<List<WsContract>> = _realTradeHistory.asStateFlow()

    private var logIdCounter = 0L

    fun addLog(msg: String, type: String = "INFO") {
        val currentLogs = _liveLogs.value.toMutableList()
        currentLogs.add(0, WsLog(logIdCounter++, System.currentTimeMillis(), msg, type))
        if (currentLogs.size > 150) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _liveLogs.value = currentLogs
    }

    private var pingSendTime = 0L
    private var pingRunnable: Runnable? = null

    init {
        // Initialize history lists
        for ((symbol, _) in VOLATILITY_SYMBOLS) {
            histories[symbol] = mutableListOf()
        }
    }

    fun connect() {
        if (webSocket != null) return

        _connectionState.value = "CONNECTING..."
        Log.d(TAG, "Connecting to Deriv WebSocket...")
        addLog("Initiating connection to Deriv secure websockets: $WS_URL", "INFO")

        val request = Request.Builder()
            .url(WS_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = "CONNECTED"
                Log.d(TAG, "Deriv WebSocket Open!")
                addLog("Secure WebSocket Connection Established successfully!", "INFO")
                
                // Subscribe to all volatility streams
                subscribeToAllStreams(webSocket)
                addLog("Subscribed to Volatility Streams: ${VOLATILITY_SYMBOLS.map { it.first }.joinToString()}", "INFO")
                
                // Start ping loop
                startPingLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                _connectionState.value = "SERVER OFFLINE"
                this@DerivWebSocketManager.webSocket = null
                addLog("WebSocket socket failure: ${t.message}. Reconnect to a working network.", "ERROR")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "DISCONNECTING"
                addLog("WebSocket closing: $reason (code: $code)", "INFO")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "DISCONNECTED"
                this@DerivWebSocketManager.webSocket = null
                addLog("WebSocket channel disconnected cleanly.", "INFO")
            }
        })
    }

    fun disconnect() {
        stopPingLoop()
        webSocket?.close(1000, "App exit")
        webSocket = null
        _connectionState.value = "DISCONNECTED"
        addLog("Application-triggered WebSocket shutdown.", "INFO")
    }

    private fun subscribeToAllStreams(ws: WebSocket) {
        for ((symbol, _) in VOLATILITY_SYMBOLS) {
            val subJson = JSONObject().apply {
                put("ticks", symbol)
                put("subscribe", 1)
            }
            ws.send(subJson.toString())
        }
    }

    private fun startPingLoop() {
        stopPingLoop()
        pingRunnable = object : Runnable {
            override fun run() {
                val ws = webSocket
                if (ws != null && _connectionState.value == "CONNECTED") {
                    pingSendTime = System.currentTimeMillis()
                    val pingJson = JSONObject().apply {
                        put("ping", 1)
                    }
                    ws.send(pingJson.toString())
                    mainHandler.postDelayed(this, 3000)
                }
            }
        }
        mainHandler.post(pingRunnable!!)
    }

    private fun stopPingLoop() {
        pingRunnable?.let { mainHandler.removeCallbacks(it) }
        pingRunnable = null
    }

    private fun parseMessage(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val msgType = json.optString("msg_type")

            if (msgType == "ping") {
                val rtt = System.currentTimeMillis() - pingSendTime
                _pingState.value = rtt
            } else if (json.has("error")) {
                val errorObj = json.optJSONObject("error")
                val errMsg = errorObj?.optString("message") ?: "Unknown API Error"
                val errCode = errorObj?.optString("code") ?: "UNKNOWN"
                addLog("API Error [$errCode]: $errMsg", "ERROR")
                
                if (msgType == "authorize") {
                    _connectionState.value = "AUTH_FAILED"
                    _authErrorState.value = errMsg
                    _authorizedBalance.value = null
                    _authorizedScopes.value = emptyList()
                }
            } else if (msgType == "authorize") {
                val authObj = json.optJSONObject("authorize")
                if (authObj != null) {
                    val balance = authObj.optDouble("balance")
                    val fullname = authObj.optString("fullname", "Deriv Trader")
                    val email = authObj.optString("email", "deriv.trader@deriv.com")
                    val country = authObj.optString("country", "US")
                    val currency = authObj.optString("currency", "USD")
                    val userId = authObj.optLong("user_id", 888123L).toString()
                    
                    val scopesArray = authObj.optJSONArray("scopes")
                    val scopesList = mutableListOf<String>()
                    if (scopesArray != null) {
                        for (i in 0 until scopesArray.length()) {
                            scopesList.add(scopesArray.optString(i))
                        }
                    }
                    if (!balance.isNaN()) {
                        _authorizedBalance.value = balance
                    }
                    _authorizedTraderName.value = fullname
                    _authorizedEmail.value = email
                    _authorizedCountry.value = country
                    _authorizedCurrency.value = currency
                    _authorizedUserId.value = userId
                    _authorizedScopes.value = scopesList
                    _authErrorState.value = null
                    _connectionState.value = "AUTHORIZED"
                    
                    addLog("Authorized context: $fullname ($email) | Balance: $$balance $currency | Scopes: $scopesList", "INBOUND")
                }
            } else if (msgType == "buy") {
                val buyObj = json.optJSONObject("buy")
                if (buyObj != null) {
                    val contractId = buyObj.optLong("contract_id", -1L)
                    val buyPrice = buyObj.optDouble("buy_price", 0.0)
                    val symbol = buyObj.optString("underlying", "R_10")
                    val contractType = buyObj.optString("contract_type", "UNKNOWN")
                    val balanceAfter = buyObj.optDouble("balance_after")
                    if (!balanceAfter.isNaN()) {
                        _authorizedBalance.value = balanceAfter
                    }
                    if (contractId != -1L) {
                        val contract = WsContract(
                            contractId = contractId,
                            buyPrice = buyPrice,
                            contractType = contractType,
                            symbol = symbol,
                            status = "OPEN"
                        )
                        addActiveContract(contract)
                        addLog("Contract purchased: ID $contractId on $symbol ($contractType). Stake: $$buyPrice.", "INBOUND")
                        subscribeToContractResult(contractId)
                    }
                }
            } else if (msgType == "proposal_open_contract") {
                val pocObj = json.optJSONObject("proposal_open_contract")
                if (pocObj != null) {
                    val contractId = pocObj.optLong("contract_id")
                    val isSold = pocObj.optInt("is_sold", 0) == 1
                    val underlying = pocObj.optString("underlying", "")
                    val contractType = pocObj.optString("contract_type", "")
                    val buyPrice = pocObj.optDouble("buy_price", 0.0)
                    val bidPrice = pocObj.optDouble("bid_price", 0.0)
                    val profit = pocObj.optDouble("profit", 0.0)
                    val status = pocObj.optString("status", "open")
                    val exitTick = pocObj.optInt("exit_tick", -1)
                    val exitDigit = if (exitTick != -1) (exitTick % 10) else null

                    updateActiveContract(
                        contractId = contractId,
                        bidPrice = bidPrice,
                        status = status,
                        profit = profit,
                        exitDigit = exitDigit,
                        isSold = isSold,
                        underlying = underlying,
                        contractType = contractType,
                        buyPrice = buyPrice
                    )
                }
            } else if (msgType == "tick") {
                val tickObj = json.optJSONObject("tick")
                if (tickObj != null) {
                    val symbol = tickObj.optString("symbol")
                    val quote = tickObj.optDouble("quote")
                    val pipSize = tickObj.optInt("pip_size", 3)
                    
                    if (symbol.isNotEmpty() && !quote.isNaN()) {
                        lastPrices[symbol] = quote
                        val digit = extractLastDigit(quote, pipSize)
                        appendDigitToHistory(symbol, digit)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}")
            addLog("Error parsing WebSocket JSON frame: ${e.message}", "ERROR")
        }
    }

    private fun addActiveContract(contract: WsContract) {
        val list = _activeContracts.value.toMutableList()
        list.removeAll { it.contractId == contract.contractId }
        list.add(contract)
        _activeContracts.value = list
    }

    private fun updateActiveContract(
        contractId: Long,
        bidPrice: Double,
        status: String,
        profit: Double,
        exitDigit: Int?,
        isSold: Boolean,
        underlying: String,
        contractType: String,
        buyPrice: Double
    ) {
        if (isSold) {
            val activeList = _activeContracts.value.toMutableList()
            activeList.removeAll { it.contractId == contractId }
            _activeContracts.value = activeList

            val finalContract = WsContract(
                contractId = contractId,
                buyPrice = buyPrice,
                contractType = contractType,
                symbol = underlying,
                status = status.uppercase(),
                profit = profit,
                bidPrice = bidPrice,
                exitDigit = exitDigit
            )
            val histList = _realTradeHistory.value.toMutableList()
            histList.removeAll { it.contractId == contractId }
            histList.add(0, finalContract)
            if (histList.size > 100) histList.removeAt(histList.size - 1)
            _realTradeHistory.value = histList
            addLog("Contract ID $contractId finished: ${status.uppercase()}. Profit/Loss: $${String.format("%.2f", profit)} (Exit digit: $exitDigit)", "INFO")
        } else {
            val activeList = _activeContracts.value.toMutableList()
            val index = activeList.indexOfFirst { it.contractId == contractId }
            if (index != -1) {
                val existing = activeList[index]
                activeList[index] = existing.copy(bidPrice = bidPrice, profit = profit)
                _activeContracts.value = activeList
            } else {
                activeList.add(WsContract(
                    contractId = contractId,
                    buyPrice = buyPrice,
                    contractType = contractType,
                    symbol = underlying,
                    status = "OPEN",
                    profit = profit,
                    bidPrice = bidPrice
                ))
                _activeContracts.value = activeList
            }
        }
    }

    private fun subscribeToContractResult(contractId: Long) {
        val ws = webSocket
        if (ws != null) {
            try {
                val json = JSONObject().apply {
                    put("proposal_open_contract", 1)
                    put("contract_id", contractId)
                    put("subscribe", 1)
                }
                ws.send(json.toString())
                addLog("Sent active open contract tracking request for: $contractId", "OUTBOUND")
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to contract result: ${e.message}")
            }
        }
    }

    fun sendAuthorizeRequest(token: String) {
        addLog("Sending Authorization request token: ${if (token.length > 5) token.take(5) + "..." else token}", "OUTBOUND")
        val ws = webSocket
        if (ws != null) {
            try {
                val json = JSONObject().apply {
                    put("authorize", token)
                }
                ws.send(json.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending authorize request: ${e.message}")
                addLog("Error transmitting authorizing packet: ${e.message}", "ERROR")
            }
        }
    }

    fun sendBuyRequest(symbol: String, contractType: String, barrier: String, stake: Double, durationTicks: Int = 2) {
        addLog("Transmitting Buy parameter proposal contract: $contractType on $symbol (barrier: $barrier, stake: $$stake, duration: $durationTicks t)", "OUTBOUND")
        val ws = webSocket
        if (ws != null) {
            try {
                val json = JSONObject().apply {
                    put("buy", 1)
                    put("price", stake)
                    val params = JSONObject().apply {
                        put("amount", stake)
                        put("basis", "stake")
                        put("contract_type", contractType)
                        put("currency", "USD")
                        put("duration", durationTicks)
                        put("duration_unit", "t")
                        put("symbol", symbol)
                        if (contractType == "OVER" || contractType == "UNDER") {
                            put("barrier", if (contractType == "OVER") "+$barrier" else "-$barrier")
                        } else if (contractType == "DIFFERS") {
                            put("barrier", barrier)
                        }
                    }
                    put("parameters", params)
                }
                ws.send(json.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending buy request: ${e.message}")
                addLog("Error transmitting buy contract details: ${e.message}", "ERROR")
            }
        }
    }

    fun appendDigitToHistory(symbol: String, digit: Int) {
        val history = histories[symbol] ?: mutableListOf()
        synchronized(history) {
            history.add(digit)
            if (history.size > 1000) {
                history.removeAt(0)
            }
        }
        _tickUpdateFlow.value = symbol to digit
    }

    fun getHistoryFor(symbol: String): List<Int> {
        val history = histories[symbol] ?: return emptyList()
        return synchronized(history) {
            history.toList()
        }
    }

    fun getLastPriceFor(symbol: String): Double {
        return lastPrices[symbol] ?: 100.0
    }

    fun isSimulating(): Boolean {
        return false
    }

    /**
     * Highly robust double-to-last-digit extractor. Formats specifically for pip_size to guarantee accurate
     * representation of fractional tick data as received in financial streams.
     */
    private fun extractLastDigit(quote: Double, pipSize: Int): Int {
        val decimals = if (pipSize in 0..8) pipSize else 3
        val formatted = String.format(Locale.US, "%.${decimals}f", quote)
        val clean = formatted.trim()
        val lastChar = clean.lastOrNull { it.isDigit() }
        return lastChar?.digitToInt() ?: (quote * 100).toInt() % 10
    }
}
