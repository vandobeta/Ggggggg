package com.example.data

/**
 * Single digit performance metrics.
 */
data class LivePredictionModel(
    val digit: Int,
    val quadrant: String,
    val confidence: Float,
    val occurrencePct: Float
)

/**
 * Summary metrics for a single Volatility Symbol scan.
 */
data class MarketScanResult(
    val symbol: String,
    val displayName: String,
    val totalEdgeScore: Float,
    val macroEvenPct: Float,
    val microEvenVelocity: Float,
    val primePredictionDigit: Int,
    val primeDigitPct: Float,
    val recommendedContract: String,
    val confidence: Float
)

/**
 * Full state packet for a selected market index.
 */
data class CompleteDataPacket(
    val symbol: String,
    val displayName: String,
    val livePing: Long,
    val quadWeights: Map<String, Float>, // key: "LO", "LE", "HO", "HE", val: percentage
    val digitBreakdowns: IntArray,       // occurrences out of historical lookup (count)
    val momentumScore: Float,
    val noiseScore: Float,
    val stabilityScore: Float,
    val predictionsList: List<LivePredictionModel>,
    val tickHistory: List<Int>,
    val lastTickValue: Double,
    val isStableConnection: Boolean
)
