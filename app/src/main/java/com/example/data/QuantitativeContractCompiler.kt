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
        frequencies: IntArray, // Frequency map of the lookback window
        oddPercentage: Float,
        evenPercentage: Float,
        completeCandidates: List<Int> // Ordered: [Primary Anchor (Index 0), Noise Guard 1, Noise Guard 2]
    ): StrategySelectionResult {

        if (completeCandidates.size < 3) {
            return StrategySelectionResult(
                currentDigit, 0.0f, -1, -1, completeCandidates,
                "NONE", "-1", false, "ABORTED: Empty candidate pools."
            )
        }

        val primaryAnchor = completeCandidates[0]
        val maxDigit = completeCandidates.maxOrNull() ?: 0
        val minDigit = completeCandidates.minOrNull() ?: 0
        val calculatedSpan = maxDigit - minDigit

        val rawDivergence = abs(oddPercentage - evenPercentage)
        val stabilityScoreS101 = ((rawDivergence / maxDivergenceThreshold) * 100f).coerceAtMost(100f)

        // GATEKEEPER 1: DEAD ZONE LOCKOUT
        if (stabilityScoreS101 < 40.0f) {
            return StrategySelectionResult(
                currentDigit, stabilityScoreS101, calculatedSpan, primaryAnchor, completeCandidates,
                "NONE", "-1", false, "LOCKOUT: Dead Zone (<40%)."
            )
        }

        // EXCLUSION GUARD: Hard blacklist on absolute randomness
        if (calculatedSpan == 9) {
            return StrategySelectionResult(
                currentDigit, stabilityScoreS101, calculatedSpan, primaryAnchor, completeCandidates,
                "NONE", "-1", false, "CRITICAL LOCKOUT: Span 9 Detected (0-9 Full Chaos)."
            )
        }

        var resolvedContract = "NONE"
        var resolvedBarrier = "-1"
        var isApproved = true
        var logMessage = "SUCCESS: Optimal Out-of-Span Matrix Locked"

        // Explicit Digit Territories
        val lowDivision = listOf(0, 1, 2, 3)
        val midDivision = listOf(4, 5, 6)
        val highDivision = listOf(7, 8, 9)

        when {
            // 1. 📉 DIGIT UNDER TERRITORY (Lows Division)
            primaryAnchor in lowDivision -> {
                if (calculatedSpan <= 4) {
                    val calculatedBarrier = maxDigit + 2
                    // Invalid Boundary Interception: Catch UNDER 0 or any mathematically absurd boundary
                    if (calculatedBarrier <= 0) {
                        val (pType, pBarrier) = performDigitDiffRedirect(primaryAnchor, completeCandidates, frequencies)
                        resolvedContract = pType
                        resolvedBarrier = pBarrier
                        logMessage = "INTERCEPTED: Invalid DIGITUNDER barrier ($calculatedBarrier <= 0). Redirected to DIGITDIFF safely."
                    } else {
                        resolvedContract = "DIGITUNDER"
                        resolvedBarrier = calculatedBarrier.coerceIn(0, 9).toString()
                        logMessage = "SUCCESS: DIGITUNDER approved. Barrier calculated out-of-span: $resolvedBarrier."
                    }
                } else {
                    // Span breaks wide: The High-Span Sniper Pivot -> spawns DIGITDIFF
                    val (pType, pBarrier) = performDigitDiffRedirect(primaryAnchor, completeCandidates, frequencies)
                    resolvedContract = pType
                    resolvedBarrier = pBarrier
                    logMessage = "PIVOT: High-Span DIGITUNDER blocked (Span $calculatedSpan >= 5). Spawning DIGITDIFF safely targeting $resolvedBarrier."
                }
            }

            // 2. 📈 DIGIT OVER TERRITORY (Highs Division)
            primaryAnchor in highDivision -> {
                if (calculatedSpan <= 4) {
                    val calculatedBarrier = minDigit - 2
                    // Invalid Boundary Interception: Catch OVER 9, UNDER 0, or negative boundaries
                    if (calculatedBarrier < 0 || calculatedBarrier >= 9) {
                        val (pType, pBarrier) = performDigitDiffRedirect(primaryAnchor, completeCandidates, frequencies)
                        resolvedContract = pType
                        resolvedBarrier = pBarrier
                        logMessage = "INTERCEPTED: Invalid DIGITOVER barrier ($calculatedBarrier). Redirected to DIGITDIFF safely."
                    } else {
                        resolvedContract = "DIGITOVER"
                        resolvedBarrier = calculatedBarrier.toString()
                        logMessage = "SUCCESS: DIGITOVER approved. Barrier calculated out-of-span: $resolvedBarrier."
                    }
                } else {
                    // Span breaks wide: The High-Span Sniper Pivot -> spawns DIGITDIFF
                    val (pType, pBarrier) = performDigitDiffRedirect(primaryAnchor, completeCandidates, frequencies)
                    resolvedContract = pType
                    resolvedBarrier = pBarrier
                    logMessage = "PIVOT: High-Span DIGITOVER blocked (Span $calculatedSpan >= 5). Spawning DIGITDIFF safely targeting $resolvedBarrier."
                }
            }

            // 3. ↔️ EVEN / ODD PARITY-HUNTING TERRITORY (Mids Division)
            primaryAnchor in midDivision -> {
                // Parity Kill-Switch: Active under Span 3-4 and Stability < 60%
                if (stabilityScoreS101 < 60.0f) {
                    return StrategySelectionResult(
                        currentDigit, stabilityScoreS101, calculatedSpan, primaryAnchor, completeCandidates,
                        "NONE", "-1", false, "KILL-SWITCH: Parity aborted. Stability below 60.0%."
                    )
                }

                // Spring Elasticity Matrix
                val isMacroEven = evenPercentage > oddPercentage
                val localEvens = completeCandidates.count { it % 2 == 0 }
                val localOdds = completeCandidates.count { it % 2 != 0 }

                // Elasticity Core Rule: Use accumulation tension of parity switching
                resolvedContract = when {
                    isMacroEven && localOdds > localEvens -> "DIGITODD" // Switching to Even with high Odds ratio snaps back to Odd
                    !isMacroEven && localEvens > localOdds -> "DIGITEVEN" // Switching to Odd with high Evens ratio snaps back to Even
                    isMacroEven -> "DIGITEVEN"
                    else -> "DIGITODD"
                }
                resolvedBarrier = "N/A"
                logMessage = "SPRING SYSTEM: Parity hunting activated. Macro Switching: ${if (isMacroEven) "EVEN" else "ODD"}. Local E-0 Ratio: $localEvens-$localOdds. Resolved: $resolvedContract"
            }

            else -> {
                resolvedContract = "NONE"
                resolvedBarrier = "-1"
                isApproved = false
                logMessage = "REJECTED: Unidentified territory boundaries for Anchor $primaryAnchor."
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

    private fun performDigitDiffRedirect(
        primaryAnchor: Int,
        completeCandidates: List<Int>,
        frequencies: IntArray
    ): Pair<String, String> {
        val excluded = (0..9).filter { it !in completeCandidates }
        val chosenDiffDigit = excluded.maxByOrNull { digit ->
            val distance = abs(digit - primaryAnchor)
            // Pick based on maximum physical distance (magnet filter) and lower frequency
            distance * 1000 + (100 - (frequencies.getOrNull(digit) ?: 0))
        } ?: 0
        return Pair("DIGITDIFF", chosenDiffDigit.toString())
    }
}
