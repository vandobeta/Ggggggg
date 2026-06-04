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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Real Deriv API WebSocket Manager
 * Uses official Deriv.com Options Trading API for real money trading
 * 
 * API Documentation: https://developers.deriv.com/docs/intro/api-overview
 * REST Base: https://api.derivws.com
 * WebSocket: wss://api.derivws.com/trading/v1/options/ws/demo or /ws/real
 */
class DerivWebSocketManager {

    companion object {
        private const val TAG = "DerivWebSocketManager"
        
        // REAL Deriv API endpoints (Options Trading API)
        private const val REST_BASE_URL = "https://api.derivws.com"
        private const val WEBSOCKET_PUBLIC_URL = "wss://api.derivws.com/trading/v1/options/ws/public"
        private const val WEBSOCKET_DEMO_URL = "wss://api.derivws.com/trading/v1/options/ws/demo"
        private const val WEBSOCKET_REAL_URL = "wss://api.derivws.com/trading/v1/options/ws/real"
        
        // App ID - Register your own app at https://app.deriv.com/
        private const val DEFAULT_APP_ID = "1089"
        
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
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Thread-safe history storage for each symbol
    private val histories = ConcurrentHashMap<String, MutableList<Int>>()
    private val lastPrices = ConcurrentHashMap<String, Double>()

    // Authentication state
    private var authToken: String = ""
    private var appId: String = DEFAULT_APP_ID
    private var currentAccountId: String = ""
    private var isDemoAccount: Boolean = true

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

    private var pingSendTime = 0L
    private var pingRunnable: Runnable? = null
    
    // NO SIMULATION - Real API only
    private var isConnectedToRealApi = false

    init {
        for ((symbol, _) in VOLATILITY_SYMBOLS) {
            histories[symbol] = mutableListOf()
        }
    }

    /**
     * Configure authentication before connecting
     */
    fun setAuthentication(token: String, accountId: String, appId: String = DEFAULT_APP_ID, isDemo: Boolean = true) {
        this.authToken = token
        this.currentAccountId = accountId
        this.appId = appId
        this.isDemoAccount = isDemo
    }

    fun connect() {
        if (webSocket != null) return

        _connectionState.value = "CONNECTING..."
        Log.d(TAG, "Connecting to Deriv API WebSocket...")

        // Use authenticated WebSocket URL if we have a token, otherwise use public endpoint
        val wsUrl = if (authToken.isNotEmpty() && currentAccountId.isNotEmpty()) {
            if (isDemoAccount) {
                "$WEBSOCKET_DEMO_URL?app_id=$appId&token=$authToken"
            } else {
                "$WEBSOCKET_REAL_URL?app_id=$appId&token=$authToken"
            }
        } else {
            WEBSOCKET_PUBLIC_URL
        }

        Log.d(TAG, "WebSocket URL: ${wsUrl.substring(0, 50)}...")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = "CONNECTED"
                isConnectedToRealApi = true
                Log.d(TAG, "Deriv API WebSocket Connected!")
                
                if (authToken.isNotEmpty()) {
                    _connectionState.value = "AUTHORIZING..."
                }
                
                subscribeToAllStreams(webSocket)
                startPingLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failed: ${t.message}")
                _connectionState.value = "CONNECTION FAILED"
                isConnectedToRealApi = false
                this@DerivWebSocketManager.webSocket = null
                _authErrorState.value = "Failed to connect: ${t.message}"
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "DISCONNECTING"
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "DISCONNECTED"
                isConnectedToRealApi = false
                this@DerivWebSocketManager.webSocket = null
            }
        })
    }

    fun disconnect() {
        stopPingLoop()
        webSocket?.close(1000, "App exit")
        webSocket = null
        _connectionState.value = "DISCONNECTED"
        isConnectedToRealApi = false
    }

    private fun subscribeToAllStreams(ws: WebSocket) {
        for ((symbol, _) in VOLATILITY_SYMBOLS) {
            val subJson = JSONObject().apply {
                put("ticks", symbol)
                put("subscribe", 1)
            }
            ws.send(subJson.toString())
            Log.d(TAG, "Subscribed to: $symbol")
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
                _authErrorState.value = errMsg
                _connectionState.value = "AUTH_FAILED"
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
                }
            } else if (msgType == "buy") {
                val buyObj = json.optJSONObject("buy")
                if (buyObj != null) {
                    val balanceAfter = buyObj.optDouble("balance_after")
                    if (!balanceAfter.isNaN()) {
                        _authorizedBalance.value = balanceAfter
                    }
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
        }
    }

    fun sendAuthorizeRequest(token: String) {
        val ws = webSocket
        if (ws != null) {
            try {
                val json = JSONObject().apply {
                    put("authorize", token)
                }
                ws.send(json.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending authorize request: ${e.message}")
            }
        }
    }

    fun sendBuyRequest(symbol: String, contractType: String, barrier: String, stake: Double, durationTicks: Int = 2) {
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
                Log.d(TAG, "BUY REQUEST: $contractType $symbol barrier:$barrier stake:$$stake")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending buy request: ${e.message}")
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

    fun isConnectedToRealApi(): Boolean {
        return isConnectedToRealApi
    }
    
    /**
     * @deprecated Use isConnectedToRealApi() instead. Kept for backward compatibility.
     */
    fun isSimulating(): Boolean {
        return !isConnectedToRealApi
    }

    private fun extractLastDigit(quote: Double, pipSize: Int): Int {
        val decimals = if (pipSize in 0..8) pipSize else 3
        val formatted = String.format(Locale.US, "%.${decimals}f", quote)
        val clean = formatted.trim()
        val lastChar = clean.lastOrNull { it.isDigit() }
        return lastChar?.digitToInt() ?: (quote * 100).toInt() % 10
    }
}