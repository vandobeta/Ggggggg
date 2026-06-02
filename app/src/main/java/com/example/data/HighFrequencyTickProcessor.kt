package com.example.data

import kotlin.math.abs

// Represents the real-time operational status of an individual profile track
data class ProfileExecutionTrack(
    val contractType: String, // "UNDER" or "OVER" or "N/A"
    val barrierParameter: String, // E.g., "1", "4", "N/A"
    val brokerPayoutPct: String, // E.g., "~970%", "~100%"
    val validationMessage: String, // "SUCCESS", "REJECTED_OUTLIER", etc.
    val isSafeToExecute: Boolean // Final operational gate flag
)

// The master state object broadcasted to your UI and Haptic managers on every tick
data class UnifiedTickState(
    val lastExtractedDigit: Int,
    val stabilityScore: Float,
    val globalRegime: String, // "REVERSION_TO_EVEN", "REVERSION_TO_ODD", "AWAITING_DATA"
    val activeCandidates: List<Int>,
    val riskyProfile: ProfileExecutionTrack,
    val saferProfile: ProfileExecutionTrack
)

data class QuadrantRecommendation(
    val selectedZone: String,          // "LOWER", "HIGHER", "MID_SCATTERED"
    val targetParityRegime: String,     // "REVERSION_TO_EVEN", "REVERSION_TO_ODD", "NEUTRAL_BALANCED"
    val primeCandidateDigits: List<Int>, // The final 2-3 mixed digits
    val calculatedLifespan: Int,
    val marketStabilityScore: Float,    // New metrics tracer
    val isSignalApproved: Boolean       // Execution lock flag
)

data class ContractValidationResult(
    val selectedContract: BrokerContractSpecs?,
    val isExecutionSafe: Boolean,
    val structuralRejectionReason: String = ""
)

class HighFrequencyTickProcessor {
    private val macroLookbackWindow = 50
    private val maxExpectedDivergence = 25.0f // Scales divergence up to 100%
    private val slidingTickCache = java.util.Collections.synchronizedList(mutableListOf<Int>())

    /**
     * Executes the absolute top-to-bottom calculation sequence for an inbound quadrant tick.
     * Call this function every single time your Deriv WebSocket pushes a fresh tick update.
     */
    fun processIncomingMarketTick(rawPrice: Double, targetContractFilter: String): UnifiedTickState {
        // 1. String Isolation Layer (Extract last digit integer)
        val priceStr = rawPrice.toString()
        val extractedDigit = priceStr.substring(priceStr.length - 1).toIntOrNull() ?: 0

        // Maintain the sliding memory window thread-safely
        synchronized(slidingTickCache) {
            slidingTickCache.add(extractedDigit)
            if (slidingTickCache.size > macroLookbackWindow) {
                slidingTickCache.removeAt(0)
            }
        }

        // Handle cold-start/warm-up data phase safety net
        if (slidingTickCache.size < macroLookbackWindow) {
            val awaitingTrack = ProfileExecutionTrack("N/A", "N/A", "0%", "Awaiting Matrix Core Data", false)
            return UnifiedTickState(extractedDigit, 0f, "AWAITING_DATA", emptyList(), awaitingTrack, awaitingTrack)
        }

        // Take snapshot list for immutable atomic batch calculation
        val currentHistory = synchronized(slidingTickCache) { slidingTickCache.toList() }

        // 2. Compute Global Parity Balance Strain & Stability Score
        val oddCount = currentHistory.count { it % 2 != 0 }
        val oddPercentage = (oddCount.toFloat() / macroLookbackWindow) * 100f
        val evenPercentage = 100f - oddPercentage

        val rawDivergence = abs(oddPercentage - evenPercentage)
        val calculatedStability = ((rawDivergence / maxExpectedDivergence) * 100f).coerceIn(0f, 100f)

        // HARD COOLDOWN MATRIX CHECK: If market is sideways, drop both immediately
        if (calculatedStability < 40.0f) {
            val lockedTrack = ProfileExecutionTrack("N/A", "N/A", "0%", "CHOPPY DEAD ZONE", false)
            return UnifiedTickState(extractedDigit, calculatedStability, "CHOPPY DEAD ZONE", emptyList(), lockedTrack, lockedTrack)
        }

        // 3. Determine Reversion Regime Targets
        val globalRegime: String
        val primarySearchPool: List<Int>
        val hybridHedgePool: List<Int>

        if (oddPercentage >= 55.0f) {
            globalRegime = "REVERSION_TO_EVEN"
            primarySearchPool = listOf(0, 2, 4, 6, 8)
            hybridHedgePool = listOf(1, 3, 5, 7, 9)
        } else if (evenPercentage >= 55.0f) {
            globalRegime = "REVERSION_TO_ODD"
            primarySearchPool = listOf(1, 3, 5, 7, 9)
            hybridHedgePool = listOf(0, 2, 4, 6, 8)
        } else {
            val lockedTrack = ProfileExecutionTrack("N/A", "N/A", "0%", "Insufficient Parity Strain", false)
            return UnifiedTickState(extractedDigit, calculatedStability, "BALANCED_COOLDOWN", emptyList(), lockedTrack, lockedTrack)
        }

        // 4. Frequency Mapping Layer (Find Starved Matrix Anomalies)
        val digitCounts = IntArray(10)
        currentHistory.forEach { digitCounts[it]++ }

        val sortedPrimary = primarySearchPool.sortedBy { digitCounts[it] }
        val core1 = sortedPrimary[0]
        val core2 = sortedPrimary[1]

        // 5. Four-Quadrant Spatial Zone Classification
        val maxCore = maxOf(core1, core2)
        val minCore = minOf(core1, core2)
        val spatialZone = when {
            maxCore <= 4 -> "LOWER"
            minCore >= 5 -> "HIGHER"
            else -> "MID_SCATTERED"
        }

        // 6. Hybrid Convergence Mix Insertion
        val candidatesList = mutableListOf(core1, core2)
        val hedgeDigit = when (spatialZone) {
            "LOWER" -> hybridHedgePool.filter { it <= 4 }.minByOrNull { digitCounts[it] }
            "HIGHER" -> hybridHedgePool.filter { it >= 5 }.minByOrNull { digitCounts[it] }
            else -> hybridHedgePool.minByOrNull { digitCounts[it] }
        }
        hedgeDigit?.let { candidatesList.add(it) }
        val final3Candidates = candidatesList.distinct().take(3)

        // 7. Parallel Track Execution Compiler Engine
        val riskyEvaluatedTrack = evaluateProfileSelection(final3Candidates, targetContractFilter, "RISKY")
        val saferEvaluatedTrack = evaluateProfileSelection(final3Candidates, targetContractFilter, "LESS_RISKY")

        return UnifiedTickState(
            lastExtractedDigit = extractedDigit,
            stabilityScore = calculatedStability,
            globalRegime = globalRegime,
            activeCandidates = final3Candidates,
            riskyProfile = riskyEvaluatedTrack,
            saferProfile = saferEvaluatedTrack
        )
    }

    /**
     * Isolated functional routine to process contract filters and reject bad outcomes
     */
    private fun evaluateProfileSelection(candidates: List<Int>, filter: String, profile: String): ProfileExecutionTrack {
        val maxDigit = candidates.maxOrNull() ?: 0
        val minDigit = candidates.minOrNull() ?: 0
        val span = maxDigit - minDigit

        if (filter == "UNDER") {
            return if (profile == "RISKY") {
                // Sniper Target: Maximize edge by selecting the extreme low boundary
                val targetBarrier = minDigit + 1

                // Outlier Filter: Guard against leaks going at or above the boundary
                val containsViolator = candidates.any { it >= targetBarrier }
                if (containsViolator || span > 4) {
                    ProfileExecutionTrack("UNDER", "N/A", "0%", "Sniper Rejected (Outlier/Span)", false)
                } else {
                    val estPayout = when (targetBarrier) {
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
                    ProfileExecutionTrack("UNDER", targetBarrier.toString(), estPayout, "SUCCESS", true)
                }
            } else {
                // Safety Net Track: Add a 2-digit cushion buffer past the highest
                val targetBarrier = (maxDigit + 2).coerceAtMost(9)
                val estPayout = when (targetBarrier) {
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
                ProfileExecutionTrack("UNDER", targetBarrier.toString(), estPayout, "SUCCESS", true)
            }
        } else {
            // HANDLE THE "OVER" CONTRACT EVALUATION RUN
            return if (profile == "RISKY") {
                val targetBarrier = maxDigit - 1
                val containsViolator = candidates.any { it <= targetBarrier }
                if (containsViolator || span > 4) {
                    ProfileExecutionTrack("OVER", "N/A", "0%", "Sniper Rejected (Outlier/Span)", false)
                } else {
                    val estPayout = when (targetBarrier) {
                        0 -> "~11%"
                        1 -> "~25%"
                        2 -> "~42%"
                        3 -> "~66%"
                        4 -> "~100%"
                        5 -> "~150%"
                        6 -> "~233%"
                        7 -> "~400%"
                        8 -> "~900%"
                        else -> "~100%"
                    }
                    ProfileExecutionTrack("OVER", targetBarrier.toString(), estPayout, "SUCCESS", true)
                }
            } else {
                val targetBarrier = (minDigit - 2).coerceAtLeast(0)
                val estPayout = when (targetBarrier) {
                    0 -> "~11%"
                    1 -> "~25%"
                    2 -> "~42%"
                    3 -> "~66%"
                    4 -> "~100%"
                    5 -> "~150%"
                    6 -> "~233%"
                    7 -> "~400%"
                    8 -> "~900%"
                    else -> "~100%"
                }
                ProfileExecutionTrack("OVER", targetBarrier.toString(), estPayout, "SUCCESS", true)
            }
        }
    }
}
