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

/**
 * Immutable blueprint mapping the fixed rules of the broker platform for Over/Under contracts.
 */
data class BrokerContractSpecs(
    val orderName: String,            // e.g., "UNDER 4" or "OVER 5"
    val targetBarrier: Int,           // The exact boundary digit parameter
    val baselineWinDigits: List<Int>, // Winning digit outcomes
    val staticPayoutPercentage: String // Broker-facing fixed return
)

object MasterContractDatabase {
    // Hardcoded Dictionary containing the entire UNDER family matrix
    val underContracts = mapOf(
        1 to BrokerContractSpecs("UNDER 1", 1, listOf(0), "~970%"),
        2 to BrokerContractSpecs("UNDER 2", 2, listOf(0, 1), "~400%"),
        3 to BrokerContractSpecs("UNDER 3", 3, listOf(0, 1, 2), "~233%"),
        4 to BrokerContractSpecs("UNDER 4", 4, listOf(0, 1, 2, 3), "~150%"),
        5 to BrokerContractSpecs("UNDER 5", 5, listOf(0, 1, 2, 3, 4), "~100%"),
        6 to BrokerContractSpecs("UNDER 6", 6, listOf(0, 1, 2, 3, 4, 5), "~66%"),
        7 to BrokerContractSpecs("UNDER 7", 7, listOf(0, 1, 2, 3, 4, 5, 6), "~42%"),
        8 to BrokerContractSpecs("UNDER 8", 8, listOf(0, 1, 2, 3, 4, 5, 6, 7), "~25%"),
        9 to BrokerContractSpecs("UNDER 9", 9, listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), "~11%")
    )

    // Hardcoded Dictionary containing the entire OVER family matrix
    val overContracts = mapOf(
        0 to BrokerContractSpecs("OVER 0", 0, listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), "~11%"),
        1 to BrokerContractSpecs("OVER 1", 1, listOf(2, 3, 4, 5, 6, 7, 8, 9), "~25%"),
        2 to BrokerContractSpecs("OVER 2", 2, listOf(3, 4, 5, 6, 7, 8, 9), "~42%"),
        3 to BrokerContractSpecs("OVER 3", 3, listOf(4, 5, 6, 7, 8, 9), "~66%"),
        4 to BrokerContractSpecs("OVER 4", 4, listOf(5, 6, 7, 8, 9), "~100%"),
        5 to BrokerContractSpecs("OVER 5", 5, listOf(6, 7, 8, 9), "~150%"),
        6 to BrokerContractSpecs("OVER 6", 6, listOf(7, 8, 9), "~233%"),
        7 to BrokerContractSpecs("OVER 7", 7, listOf(8, 9), "~400%"),
        8 to BrokerContractSpecs("OVER 8", 8, listOf(9), "~900%")
    )
}

data class MultContractSpecs(
    val orderName: String,
    val payoutLabel: String,
    val multiplierValue: Double
)

object MultContractDatabase {
    val multipliers = mapOf(
        50 to MultContractSpecs("x50", "~85%", 50.0),
        100 to MultContractSpecs("x100", "~170%", 100.0),
        200 to MultContractSpecs("x200", "~340%", 200.0),
        500 to MultContractSpecs("x500", "~850%", 500.0)
    )
}

