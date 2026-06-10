package com.example.data

data class DynamicBarrierResult(
    val optimalBarrier: Int,
    val confidence: Float,
    val winProbability: Float,
    val riskAdjustedPayout: Float,
    val recommendedContract: String  // "OVER" or "UNDER"
)

class DynamicBarrierEngine {
    
    fun calculateDynamicBarrier(
        history: List<Int>,
        currentDigit: Int,
        macroFrequencies: IntArray,
        momentumDirection: String  // "UP", "DOWN", "SIDEWAYS"
    ): DynamicBarrierResult {
        
        // Analyze digit distribution patterns (gaps relative to average frequency)
        val avgFrequency = macroFrequencies.average().toFloat().coerceAtLeast(1f)
        val digitGaps = macroFrequencies.indices.associateWith { index ->
            val freq = macroFrequencies[index]
            // Safe starvation calculation: lower frequency -> higher gap
            (avgFrequency - freq) / avgFrequency
        }
        
        val momentumAlignment = when (momentumDirection) {
            "UP" -> 1.0f
            "DOWN" -> -1.0f
            else -> 0.0f
        }
        
        // Find optimal barrier based on:
        // 1. Starved digits (lowest frequency)
        // 2. Momentum direction
        // 3. Risk/reward ratio for each possible barrier
        var bestBarrier = 4  // standard default
        var bestScore = -Float.MAX_VALUE
        
        // Evaluate possible barriers from 1 to 8 (so that both UNDER and OVER have actual possible winnings)
        for (barrier in 1..8) {
            val score = calculateBarrierScore(
                barrier = barrier,
                digitGaps = digitGaps,
                momentumAlignment = momentumAlignment,
                history = history
            )
            if (score > bestScore) {
                bestScore = score
                bestBarrier = barrier
            }
        }
        
        val contract = if (bestBarrier <= 4) "UNDER" else "OVER"
        val winDigits = if (contract == "UNDER") (0 until bestBarrier).count() else (bestBarrier + 1..9).count()
        val winProb = winDigits * 10f
        
        val payoutPercent = getPayoutForBarrier(bestBarrier, contract)
        val riskAdjustedPayout = winProb * payoutPercent / 100f
        
        return DynamicBarrierResult(
            optimalBarrier = bestBarrier,
            confidence = bestScore.coerceIn(0f, 100f),
            winProbability = winProb,
            riskAdjustedPayout = riskAdjustedPayout,
            recommendedContract = contract
        )
    }
    
    // Calculate barrier score considering starving digits, momentum, and historical performance
    private fun calculateBarrierScore(
        barrier: Int,
        digitGaps: Map<Int, Float>,
        momentumAlignment: Float,
        history: List<Int>
    ): Float {
        // If barrier <= 4, contract is UNDER barrier, win condition: digit < barrier.
        // Otherwise, contract is OVER barrier, win condition: digit > barrier.
        val isUnder = barrier <= 4
        val digitRange = if (isUnder) 0 until barrier else (barrier + 1)..9
        val digitCount = digitRange.count().coerceAtLeast(1)
        
        // 1. Starvation score: prefer barriers where winning digits are starved (under-represented)
        val starvationScore = digitRange.sumOf { digit ->
            (digitGaps[digit] ?: 0f).toDouble()
        }.toFloat() / digitCount
        
        // 2. Momentum alignment bonus:
        // UP trend: prefer OVER contract (barriers >= 5, so winning digits are 6-9)
        // DOWN trend: prefer UNDER contract (barriers <= 4, so winning digits are 0-3)
        val alignmentBonus = if (momentumAlignment > 0f) {
            if (!isUnder) 1.2f else 0.4f
        } else if (momentumAlignment < 05f) {
            if (isUnder) 1.2f else 0.4f
        } else {
            0.8f
        }
        
        // 3. Historical success hit rate over last 30 ticks
        val sampleSize = history.size.coerceAtMost(30)
        val hitRateScore = if (sampleSize > 0) {
            val hits = history.takeLast(sampleSize).count { digit ->
                if (isUnder) digit < barrier else digit > barrier
            }
            hits.toFloat() / sampleSize
        } else {
            0.5f
        }
        
        // Multiply weight parameters to output a robust confidence score
        return (starvationScore * 40f) + (alignmentBonus * 30f) + (hitRateScore * 30f)
    }

    private fun getPayoutForBarrier(barrier: Int, contractType: String): Float {
        return if (contractType == "UNDER") {
            when (barrier) {
                1 -> 970f
                2 -> 400f
                3 -> 233f
                4 -> 150f
                5 -> 100f
                6 -> 66f
                7 -> 42f
                8 -> 25f
                9 -> 11f
                else -> 100f
            }
        } else {
            when (barrier) {
                0 -> 11f
                1 -> 25f
                2 -> 42f
                3 -> 66f
                4 -> 100f
                5 -> 150f
                6 -> 233f
                7 -> 400f
                8 -> 900f
                else -> 100f
            }
        }
    }
}
