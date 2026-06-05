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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
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

    var activeAppId: String = "1089"

    fun getClassicWsUrl(): String {
        return "wss://ws.binaryws.com/websockets/v3?app_id=$activeAppId"
    }

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

    private val _authorizedIsVirtual = MutableStateFlow<Boolean?>(null)
    val authorizedIsVirtual: StateFlow<Boolean?> = _authorizedIsVirtual.asStateFlow()

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
    var preferDemo: Boolean = true
    private var pendingAuthorizeToken: String? = null

    // Helper data class representing a robust options trading account retrieved from REST API
    data class DerivApiAccount(
        val accountId: String,
        val accountType: String, // "demo" or "real"
        val balance: Double,
        val currency: String,
        val status: String,
        val name: String,
        val email: String
    )

    private fun parseAccounts(responseBody: String): List<DerivApiAccount> {
        val accountsList = mutableListOf<DerivApiAccount>()
        try {
            val json = JSONObject(responseBody)
            val dataArray = json.optJSONArray("data")
            if (dataArray != null) {
                for (i in 0 until dataArray.length()) {
                    val accObj = dataArray.optJSONObject(i) ?: continue
                    val accountId = accObj.optString("account_id")
                    val accountType = accObj.optString("account_type")
                    val balance = accObj.optDouble("balance", 0.0)
                    val currency = accObj.optString("currency", "USD")
                    val status = accObj.optString("status", "active")
                    val name = accObj.optString("name", "Deriv Trader")
                    val email = accObj.optString("email", "")
                    accountsList.add(
                        DerivApiAccount(
                            accountId = accountId,
                            accountType = accountType,
                            balance = balance,
                            currency = currency,
                            status = status,
                            name = name,
                            email = email
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing options accounts JSON: ${e.message}")
        }
        return accountsList
    }

    private fun parseRestError(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = JSONObject(errorBody)
            val errorsArray = json.optJSONArray("errors")
            if (errorsArray != null && errorsArray.length() > 0) {
                val firstError = errorsArray.optJSONObject(0)
                firstError?.optString("message")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getOtpWebsocketUrl(token: String, accountId: String): String? {
        val mediaTypeJson = "application/json; charset=utf-8".toMediaTypeOrNull()
        val emptyBody = "{}".toRequestBody(mediaTypeJson)
        val request = Request.Builder()
            .url("https://api.derivws.com/trading/v1/options/accounts/$accountId/otp")
            .addHeader("Deriv-App-ID", activeAppId)
            .addHeader("Authorization", "Bearer $token")
            .post(emptyBody)
            .build()
            
        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val dataObj = json.optJSONObject("data")
                dataObj?.optString("url")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun connectWebSocketNewUrl(wsUrl: String, account: DerivApiAccount) {
        synchronized(this) {
            webSocket?.close(1000, "App reconnect to authorized link")
            webSocket = null
        }
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = "AUTHORIZED"
                addLog("Connected to Options trading WebSocket for ${account.accountId} (${account.accountType.uppercase()})", "INFO")
                
                // Populate profile states so UI/DB update live
                _authorizedBalance.value = account.balance
                _authorizedTraderName.value = account.name
                _authorizedEmail.value = account.email
                _authorizedIsVirtual.value = (account.accountType == "demo")
                _authorizedCountry.value = "US"
                _authorizedCurrency.value = account.currency
                _authorizedUserId.value = account.accountId
                _authorizedScopes.value = listOf("read", "trade")
                _authErrorState.value = null
                
                // Subscribe to public volatility streams
                subscribeToAllStreams(webSocket)
                addLog("Subscribed to Volatility Streams: ${VOLATILITY_SYMBOLS.map { it.first }.joinToString()}", "INFO")
                
                // Subscribe to live balance updates
                subscribeToBalance(webSocket)
                
                // Start ping loop
                startPingLoop()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                parseMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "New API WebSocket failure: ${t.message}")
                addLog("WebSocket failure: ${t.message}. Reconnect to a working network.", "ERROR")
                _connectionState.value = "SERVER OFFLINE"
                this@DerivWebSocketManager.webSocket = null
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                addLog("WebSocket closing: $reason (code: $code)", "INFO")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "DISCONNECTED"
                this@DerivWebSocketManager.webSocket = null
                addLog("WebSocket channel disconnected cleanly.", "INFO")
            }
        })
    }

    private fun subscribeToBalance(ws: WebSocket) {
        try {
            val json = JSONObject().apply {
                put("balance", 1)
                put("subscribe", 1)
            }
            ws.send(json.toString())
            addLog("Sent balance updates subscription", "OUTBOUND")
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to balance: ${e.message}")
        }
    }

    private fun connectLegacy(token: String) {
        val dynamicWsUrl = getClassicWsUrl()
        addLog("Initializing legacy WebSocket fallback connection to: $dynamicWsUrl", "INFO")
        synchronized(this) {
            webSocket?.close(1000, "Reset to legacy")
            webSocket = null
        }
        
        val request = Request.Builder()
            .url(dynamicWsUrl)
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = "CONNECTED"
                Log.d(TAG, "Legacy Deriv WebSocket Open!")
                addLog("Secure WebSocket Connection Established successfully (Legacy Fallback)", "INFO")
                
                // Subscribe to streams
                subscribeToAllStreams(webSocket)
                addLog("Subscribed to Volatility Streams: ${VOLATILITY_SYMBOLS.map { it.first }.joinToString()}", "INFO")
                
                // Authorize on socket
                try {
                    val json = JSONObject().apply {
                        put("authorize", token)
                    }
                    webSocket.send(json.toString())
                    addLog("Transmitting legacy authorizing packet...", "OUTBOUND")
                } catch (e: Exception) {
                    Log.e(TAG, "Legacy authorize send error: ${e.message}")
                }
                
                // Start ping loop
                startPingLoop()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                parseMessage(text)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Legacy failure: ${t.message}")
                _connectionState.value = "SERVER OFFLINE"
                this@DerivWebSocketManager.webSocket = null
                addLog("Legacy WebSocket failure: ${t.message}", "ERROR")
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                addLog("Legacy WebSocket closing: $reason (code: $code)", "INFO")
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "DISCONNECTED"
                this@DerivWebSocketManager.webSocket = null
                addLog("Legacy WebSocket channel disconnected cleanly.", "INFO")
            }
        })
    }

    private fun authenticateAndConnectNewApi(token: String, isDemoDesired: Boolean) {
        _connectionState.value = "CONNECTING..."
        Log.d(TAG, "Connecting using new Option REST endpoints and OTP WebSocket flow...")
        addLog("Querying active options accounts from REST endpoint...", "INFO")
        
        val request = Request.Builder()
            .url("https://api.derivws.com/trading/v1/options/accounts")
            .addHeader("Deriv-App-ID", activeAppId)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
            
        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                val errMsg = parseRestError(errorBody) ?: "HTTP Fetch Accounts error code ${response.code}"
                addLog("REST accounts retrieval failed: $errMsg", "ERROR")
                
                // Try legacy fallback
                addLog("Falling back to legacy WebSocket authorization flow...", "INFO")
                connectLegacy(token)
                return
            }
            
            val responseBody = response.body?.string() ?: ""
            val accounts = parseAccounts(responseBody)
            if (accounts.isEmpty()) {
                _connectionState.value = "AUTH_FAILED"
                _authErrorState.value = "No Options Accounts found for this token."
                addLog("REST authorization failed: No options accounts listed on token.", "ERROR")
                return
            }
            
            // Try preferred mode, if it fails or doesn't exist try alternate!
            val preferredType = if (isDemoDesired) "demo" else "real"
            val alternateType = if (isDemoDesired) "real" else "demo"
            
            val preferredAccount = accounts.find { it.accountType == preferredType }
            val alternateAccount = accounts.find { it.accountType == alternateType }
            
            var accountToUse = preferredAccount
            var usedAlternate = false
            
            if (accountToUse == null) {
                accountToUse = alternateAccount
                usedAlternate = true
                addLog("No $preferredType Options Account available on token. Real/Demo Switch: attempting $alternateType account setup...", "INFO")
            }
            
            if (accountToUse == null) {
                _connectionState.value = "AUTH_FAILED"
                _authErrorState.value = "No suitable Demo or Real Options accounts found."
                addLog("REST authorization failed: Neither demo nor real options accounts found.", "ERROR")
                return
            }
            
            // Call OTP endpoint
            var wsUrl = getOtpWebsocketUrl(token, accountToUse.accountId)
            
            if (wsUrl == null && !usedAlternate && alternateAccount != null) {
                addLog("Secure OTP acquisition failed for preferred account type. Real/Demo Switch: trying fallback alternate $alternateType account...", "INFO")
                accountToUse = alternateAccount
                usedAlternate = true
                wsUrl = getOtpWebsocketUrl(token, accountToUse.accountId)
            }
            
            if (wsUrl == null) {
                _connectionState.value = "AUTH_FAILED"
                _authErrorState.value = "Failed to generate OTP secure trade URL."
                addLog("REST authorization failed: unable to obtain WebSocket login OTP.", "ERROR")
                return
            }
            
            // Conntect to the obtained URL directly
            val targetAccount = accountToUse
            mainHandler.post {
                connectWebSocketNewUrl(wsUrl, targetAccount)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "New REST API validation fatal: ${e.message}")
            addLog("New REST API authentication exception: ${e.message}. Attempting legacy fallback...", "ERROR")
            mainHandler.post {
                connectLegacy(token)
            }
        }
    }

    init {
        // Initialize history lists
        for ((symbol, _) in VOLATILITY_SYMBOLS) {
            histories[symbol] = mutableListOf()
        }
    }

    fun connect() {
        val token = pendingAuthorizeToken
        if (!token.isNullOrEmpty()) {
            sendAuthorizeRequest(token)
            return
        }

        if (webSocket != null) return

        _connectionState.value = "CONNECTING..."
        Log.d(TAG, "Connecting to public Deriv Options WebSocket...")
        val publicUrl = "wss://api.derivws.com/trading/v1/options/ws/public"
        addLog("Initiating connection to public options market data: $publicUrl", "INFO")

        val request = Request.Builder()
            .url(publicUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = "CONNECTED"
                Log.d(TAG, "Public Deriv WebSocket Open!")
                addLog("Public Options WebSocket Connection Established successfully!", "INFO")
                
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
                Log.e(TAG, "Public WebSocket failure: ${t.message}")
                _connectionState.value = "SERVER OFFLINE"
                this@DerivWebSocketManager.webSocket = null
                addLog("Public WebSocket socket failure: ${t.message}.", "ERROR")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                addLog("Public WebSocket closing: $reason (code: $code)", "INFO")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = "DISCONNECTED"
                this@DerivWebSocketManager.webSocket = null
                addLog("Public WebSocket channel disconnected cleanly.", "INFO")
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
                val rawErrorJson = json.toString()
                addLog("API Error [$errCode]: $errMsg", "ERROR")
                addLog("EXACT ERROR RESPONSE: $rawErrorJson", "ERROR")
                
                if (msgType == "authorize") {
                    _connectionState.value = "AUTH_FAILED"
                    _authErrorState.value = "API Error [$errCode]: $errMsg (Raw Response: $rawErrorJson)"
                    _authorizedBalance.value = null
                    _authorizedIsVirtual.value = null
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
                    val isVirtual = authObj.optInt("is_virtual", 0) == 1 || authObj.optBoolean("is_virtual", false)
                    
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
                    _authorizedIsVirtual.value = isVirtual
                    _authorizedScopes.value = scopesList
                    _authErrorState.value = null
                    _connectionState.value = "AUTHORIZED"
                    
                    addLog("Authorized context: $fullname ($email) | Virtual/Demo: $isVirtual | Balance: $$balance $currency | Scopes: $scopesList", "INBOUND")
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
                    val status = pocObj.optString("status", "open")
                    val isSold = pocObj.optBoolean("is_sold") || 
                                 pocObj.optInt("is_sold", 0) == 1 || 
                                 status.trim().lowercase() != "open"
                    val underlying = pocObj.optString("underlying", "")
                    val contractType = pocObj.optString("contract_type", "")
                    val buyPrice = pocObj.optDouble("buy_price", 0.0)
                    val bidPrice = pocObj.optDouble("bid_price", 0.0)
                    val profit = pocObj.optDouble("profit", 0.0)
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
        pendingAuthorizeToken = token
        _authErrorState.value = null
        addLog("Initiating dynamic REST-driven PAT token activation: ${if (token.length > 5) token.take(5) + "..." else token}", "OUTBOUND")
        scope.launch(Dispatchers.IO) {
            authenticateAndConnectNewApi(token, preferDemo)
        }
    }

    fun validateToken(token: String, onResult: (Boolean, String?) -> Unit) {
        addLog("Running diagnostics test for Token against Deriv Authorize API...", "INFO")
        val request = Request.Builder()
            .url(getClassicWsUrl())
            .build()
        
        var hasResponded = false
        val tempClient = OkHttpClient()
        tempClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                try {
                    val authReq = JSONObject().apply {
                        put("authorize", token)
                    }
                    webSocket.send(authReq.toString())
                    addLog("Diagnostics: Sent authorize API payload", "OUTBOUND")
                } catch (e: Exception) {
                    if (!hasResponded) {
                        hasResponded = true
                        onResult(false, e.message)
                        addLog("Diagnostics Exception: ${e.message}", "ERROR")
                    }
                    webSocket.close(1000, "Done")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val msgType = json.optString("msg_type")
                    if (msgType == "authorize") {
                        if (json.has("error")) {
                            val errorObj = json.optJSONObject("error")
                            val exactErrorJson = json.toString()
                            addLog("Diagnostic test failed! EXACT API ERROR RESPONSE: $exactErrorJson", "ERROR")
                            if (!hasResponded) {
                                hasResponded = true
                                onResult(false, exactErrorJson)
                            }
                        } else {
                            val authObj = json.optJSONObject("authorize")
                            val fullname = authObj?.optString("fullname") ?: "Trader"
                            addLog("Diagnostic test successful for $fullname!", "INFO")
                            if (!hasResponded) {
                                hasResponded = true
                                onResult(true, "Authorized successfully as $fullname")
                            }
                        }
                        webSocket.close(1000, "Done")
                    }
                } catch (e: Exception) {
                    if (!hasResponded) {
                        hasResponded = true
                        onResult(false, e.message)
                    }
                    webSocket.close(1000, "Done")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!hasResponded) {
                    hasResponded = true
                    val exactError = "Connection test failure: ${t.message}"
                    addLog("Diagnostic connection failure: ${t.message}", "ERROR")
                    onResult(false, exactError)
                }
            }
        })
    }

    fun sendBuyRequest(symbol: String, contractType: String, barrier: String, stake: Double, durationTicks: Int = 2) {
        val mappedContractType = when (contractType.uppercase()) {
            "EVEN", "DIGITEVEN" -> "DIGITEVEN"
            "ODD", "DIGITODD" -> "DIGITODD"
            "OVER", "DIGITOVER" -> "DIGITOVER"
            "UNDER", "DIGITUNDER" -> "DIGITUNDER"
            "DIFFERS", "DIGITDIFF" -> "DIGITDIFF"
            "MATCHES", "DIGITMATCH" -> "DIGITMATCH"
            else -> contractType
        }
        addLog("Transmitting Buy parameter proposal contract: $mappedContractType on $symbol (barrier: $barrier, stake: $$stake, duration: $durationTicks t)", "OUTBOUND")
        val ws = webSocket
        if (ws != null) {
            try {
                val json = JSONObject().apply {
                    put("buy", 1)
                    put("price", stake)
                    val params = JSONObject().apply {
                        put("amount", stake)
                        put("basis", "stake")
                        put("contract_type", mappedContractType)
                        put("currency", "USD")
                        put("duration", durationTicks)
                        put("duration_unit", "t")
                        put("symbol", symbol)
                        if (mappedContractType == "DIGITOVER" || mappedContractType == "DIGITUNDER" || mappedContractType == "DIGITDIFF" || mappedContractType == "DIGITMATCH") {
                            // Extract absolute digit (0-9) for standard digit barrier
                            val cleanBarrier = barrier.filter { it.isDigit() }
                            put("barrier", if (cleanBarrier.isNotEmpty()) cleanBarrier else "5")
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

    fun clearTickCaches() {
        addLog("Enforcing strict cache management: clearing internal tick buffers & resetting last-known quotes to ignore stale packets.", "INFO")
        histories.clear()
        lastPrices.clear()
        _tickUpdateFlow.value = null
        for ((symbol, _) in VOLATILITY_SYMBOLS) {
            histories[symbol] = mutableListOf()
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
