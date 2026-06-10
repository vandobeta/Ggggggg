package com.example.data

data class TrendReversalSignal(
    val reversalType: String,      // "BULLISH", "BEARISH", or "NONE"
    val retestCount: Int,          // 0 to 3
    val supportLevel: Double,
    val resistanceLevel: Double,
    val isEntryReady: Boolean,
    val confidence: Float,
    val entryDirection: String     // "MULTUP" or "MULTDOWN" or "WAIT"
)

internal data class IntermediaryTrendReversal(
    val type: String,
    val momentumBefore: Float,
    val momentumAfter: Float
)

class TrendReversalEngine {
    
    companion object {
        const val RETEST_THRESHOLD = 3
        const val LOOKBACK_WINDOW = 50
        const val MIN_REVERSAL_MAGNITUDE = 0.3f  // 30% momentum shift
    }
    
    fun detectTrendReversal(tickHistory: List<Int>): TrendReversalSignal {
        val recentTicks = tickHistory.takeLast(LOOKBACK_WINDOW)
        if (recentTicks.size < 20) {
            return TrendReversalSignal(
                reversalType = "NONE",
                retestCount = 0,
                supportLevel = 0.0,
                resistanceLevel = 9.0,
                isEntryReady = false,
                confidence = 0f,
                entryDirection = "WAIT"
            )
        }
        
        // Calculate momentum before and after midpoint
        val midpoint = recentTicks.size / 2
        val firstHalf = recentTicks.take(midpoint)
        val secondHalf = recentTicks.takeLast(recentTicks.size - midpoint)
        
        val momentumBefore = calculateNetDirection(firstHalf)
        val momentumAfter = calculateNetDirection(secondHalf)
        
        // Detect reversal: e.g. from down (negative) to up (positive), or up (positive) to down (negative)
        val reversalDetected = when {
            momentumBefore < -0.1f && momentumAfter > MIN_REVERSAL_MAGNITUDE -> {
                IntermediaryTrendReversal("BULLISH", momentumBefore, momentumAfter)
            }
            momentumBefore > 0.1f && momentumAfter < -MIN_REVERSAL_MAGNITUDE -> {
                IntermediaryTrendReversal("BEARISH", momentumBefore, momentumAfter)
            }
            else -> null
        }
        
        // Track retests
        if (reversalDetected != null) {
            return trackRetests(recentTicks, reversalDetected)
        }
        
        return TrendReversalSignal(
            reversalType = "NONE",
            retestCount = 0,
            supportLevel = 0.0,
            resistanceLevel = 9.0,
            isEntryReady = false,
            confidence = 0f,
            entryDirection = "WAIT"
        )
    }
    
    private fun trackRetests(
        ticks: List<Int>,
        reversal: IntermediaryTrendReversal
    ): TrendReversalSignal {
        val level = if (reversal.type == "BULLISH") {
            ticks.minOrNull()?.toDouble() ?: 0.0  // Support = recent low
        } else {
            ticks.maxOrNull()?.toDouble() ?: 9.0  // Resistance = recent high
        }
        
        // Count retests that successfully held
        val retests = countRetests(ticks, level, reversal.type)
        val isReady = retests >= RETEST_THRESHOLD
        
        return TrendReversalSignal(
            reversalType = reversal.type,
            retestCount = retests.coerceAtMost(RETEST_THRESHOLD),
            supportLevel = if (reversal.type == "BULLISH") level else 0.0,
            resistanceLevel = if (reversal.type == "BEARISH") level else 9.0,
            isEntryReady = isReady,
            confidence = (retests.toFloat() / RETEST_THRESHOLD).coerceAtMost(1.0f),
            entryDirection = if (reversal.type == "BULLISH") "MULTUP" else "MULTDOWN"
        )
    }
    
    private fun countRetests(ticks: List<Int>, level: Double, type: String): Int {
        var count = 0
        val tolerance = 0.5  // 0.5 digit tolerance
        
        for (tick in ticks) {
            val hitLevel = when (type) {
                "BULLISH" -> tick.toDouble() <= level + tolerance  // Touched support and held
                "BEARISH" -> tick.toDouble() >= level - tolerance  // Touched resistance and held
                else -> false
            }
            if (hitLevel) count++
        }
        return count
    }

    private fun calculateNetDirection(ticks: List<Int>): Float {
        if (ticks.size < 2) return 0f
        var changes = 0
        var netDirection = 0f
        for (i in 1 until ticks.size) {
            val diff = ticks[i] - ticks[i - 1]
            if (diff > 0) {
                netDirection += 1f
                changes++
            } else if (diff < 0) {
                netDirection -= 1f
                changes++
            }
        }
        return if (changes > 0) netDirection / changes else 0f
    }
}
