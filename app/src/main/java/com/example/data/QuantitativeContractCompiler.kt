package com.example.data

import kotlin.math.abs

data class StrategySelectionResult(
    val lastDigit: Int,
    val stabilityScoreS101: Float,
    val activeSpan: Int,
    val primaryAnchorDigit: Int,
    val completeCandidatePool: List<Int>,
    val chosenContractType: String,      // "DIGITUNDER", "DIGITOVER", "DIGITEVEN", "DIGITODD", "DIGITDIFF", "NONE"
    val chosenBarrierParameter: String,  // "4", "5", "6", "0", "N/A", "-1"
    val isFilterSequencePassed: Boolean,
    val structuralDiagnosticLog: String
)

class QuantitativeContractCompiler {

    private val maxDivergenceThreshold = 25.0f // Scales 25% raw divergence to a 100% score baseline

    fun compileContractStrategy(
        currentDigit: Int,
        frequencies: IntArray, // Frequency map of the 50-tick lookback window
        oddPercentage: Float,
        evenPercentage: Float,
        completeCandidates: List<Int> // Ordered: [Primary Anchor (Index 0), Noise Guard 1, Noise Guard 2]
    ): StrategySelectionResult {

        val rawDivergence = abs(oddPercentage - evenPercentage)
        val stabilityScoreS101 = ((rawDivergence / maxDivergenceThreshold) * 100f).coerceAtMost(100f)

        // GATEKEEPER 1: DEAD ZONE LOCKOUT
        if (stabilityScoreS101 < 40.0f) {
            return StrategySelectionResult(
                currentDigit, stabilityScoreS101, -1, -1, completeCandidates,
                "NONE", "-1", false, "LOCKOUT: Dead Zone (<40%)."
            )
        }

        val primaryAnchor = completeCandidates.getOrNull(0) ?: return StrategySelectionResult(
            currentDigit, stabilityScoreS101, -1, -1, completeCandidates,
            "NONE", "-1", false, "ABORTED: Empty candidate pools."
        )
        
        val maxDigit = completeCandidates.maxOrNull() ?: 0
        val minDigit = completeCandidates.minOrNull() ?: 0
        val calculatedSpan = maxDigit - minDigit

        // EXCLUSION GUARD: Hard blacklist on absolute randomness
        if (calculatedSpan == 9) {
            return StrategySelectionResult(
                currentDigit, stabilityScoreS101, calculatedSpan, primaryAnchor, completeCandidates,
                "NONE", "-1", false, "CRITICAL LOCKOUT: Span 9 Detected (0-9 Full Chaos)."
            )
        }

        val isAnchorOdd = primaryAnchor % 2 != 0
        var resolvedContract = "NONE"
        var resolvedBarrier = "-1"
        var isApproved = true
        var logMessage = "SUCCESS: Optimal Out-of-Span Matrix Locked"

        when {
            // ENGINE A: OVER/UNDER (Trend-Following) -> Span <= 2
            calculatedSpan <= 2 -> {
                if (stabilityScoreS101 > 85.0f) {
                    resolvedContract = "DIGITDIFF"
                    resolvedBarrier = primaryAnchor.toString()
                    logMessage = "EXECUTE: Sniper Zone Override on Span <= 2. Targeting Anchor."
                } else {
                    // Force dynamic out-of-span cushioning using safety buffers 4, 5, 6
                    if (evenPercentage >= 55.0f) { 
                        // Even dominates -> Reverting to Odds. Highest weight is Odd.
                        resolvedContract = "DIGITUNDER"
                        resolvedBarrier = (maxDigit + 2).coerceIn(4, 6).toString()
                    } else {
                        resolvedContract = "DIGITOVER"
                        resolvedBarrier = (minDigit - 2).coerceIn(4, 6).toString()
                    }
                }
            }

            // ENGINE B: EVEN/ODD (Parity-Hunting) -> Span 3 to 4
            calculatedSpan in 3..4 -> {
                // HARD KILL-SWITCH: Drop trade if stability falls out of strict bounds
                if (stabilityScoreS101 < 60.0f) {
                    return StrategySelectionResult(
                        currentDigit, stabilityScoreS101, calculatedSpan, primaryAnchor, completeCandidates,
                        "NONE", "-1", false, "KILL-SWITCH: Parity aborted. Stability below 60%."
                    )
                }

                if (stabilityScoreS101 > 85.0f) {
                    resolvedContract = "DIGITDIFF"
                    resolvedBarrier = primaryAnchor.toString()
                    logMessage = "EXECUTE: Sniper Zone Override on Span 3-4. Targeting Anchor."
                } else {
                    // Factor in structural weight gravity directly
                    resolvedContract = if (isAnchorOdd) "DIGITODD" else "DIGITEVEN"
                    resolvedBarrier = "N/A"
                }
            }

            // ENGINE C: DIFFERS (Elasticity/Volatility Trap) -> Span > 4 (Secondary Engine Only)
            calculatedSpan > 4 -> {
                if (stabilityScoreS101 > 85.0f) {
                    resolvedContract = "DIGITDIFF"
                    resolvedBarrier = primaryAnchor.toString()
                    logMessage = "EXECUTE: High-Conviction Sniper Differs Target Locked."
                } 
                else if (calculatedSpan == 8 && minDigit == 1 && maxDigit == 9) {
                    // Special Elasticity Trap: Market volume locked in 1-9 range. 0 is dead.
                    resolvedContract = "DIGITDIFF"
                    resolvedBarrier = "0"
                    logMessage = "EXECUTE: Extreme Elasticity Trap Active. Targeting Isolated 0."
                } 
                else {
                    // Enforce structural secondary engine suppression: Block general wide-span differs
                    isApproved = false
                    resolvedContract = "NONE"
                    resolvedBarrier = "-1"
                    logMessage = "REJECTED: High Dispersion Chaos Trap. Suppressing low-conviction Differs."
                }
            }
        }

        return StrategySelectionResult(
            lastDigit = currentDigit,
            stabilityScoreS101 = stabilityScoreS101,
            activeSpan = calculatedSpan,
            primaryAnchorDigit = primaryAnchor,
            completeCandidatePool = completeCandidates,
            chosenContractType = resolvedContract,
            chosenBarrierParameter = resolvedBarrier,
            isFilterSequencePassed = isApproved,
            structuralDiagnosticLog = logMessage
        )
    }
}
