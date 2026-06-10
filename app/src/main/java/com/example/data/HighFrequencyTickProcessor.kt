package com.example.data

import kotlin.math.abs

// Enriched telemetry data layer tracking individual multi-digit starvation weights
data class PrecisionMatrixOutput(
    val lastDigit: Int,
    val stabilityScoreS101: Float,
    val calculatedSpan: Int,
    val completeCandidates: List<Int>,
    val candidateStarvationCoefficients: List<Float>, // Maps 1:1 with positions
    val targetContractFamily: String,
    val calculatedBarrier: String,
    val isFilterSequencePassed: Boolean,
    val diagnosticLogMessage: String
)

// Represents the real-time operational status of an individual profile track
data class ProfileExecutionTrack(
    val contractType: String,      // "UNDER", "OVER", "DIFFERS" or "NONE"
    val barrierParameter: String,  // E.g., "1", "4", "N/A"
    val brokerPayoutPct: String,   // E.g., "~970%", "~100%"
    val validationMessage: String, // "SUCCESS", "REJECTED_OUTLIER", etc.
    val isSafeToExecute: Boolean   // Final operational gate flag
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
    private val microLookbackWindow = 10
    private val slidingTickCache = java.util.Collections.synchronizedList(mutableListOf<Int>())

    /**
     * Executes the absolute top-to-bottom calculation sequence for an inbound quadrant tick.
     * Call this function every single time your Deriv WebSocket pushes a fresh tick update.
     */
    fun processIncomingMarketTick(rawPrice: Double, targetContractFilter: String, isOneSecond: Boolean = false, cushionSpacing: Int = 2): UnifiedTickState {
        val actualMacroSize = if (isOneSecond) 30 else macroLookbackWindow
        val actualMicroSize = if (isOneSecond) 6 else microLookbackWindow

        // 1. String Isolation Layer (Extract last digit integer)
        val priceStr = rawPrice.toString()
        val extractedDigit = priceStr.substring(priceStr.length - 1).toIntOrNull() ?: 0

        // Maintain the sliding memory window thread-safely
        synchronized(slidingTickCache) {
            slidingTickCache.add(extractedDigit)
            if (slidingTickCache.size > actualMacroSize) {
                slidingTickCache.removeAt(0)
            }
        }

        // Handle cold-start/warm-up data phase safety net
        if (slidingTickCache.size < actualMacroSize) {
            val awaitingTrack = ProfileExecutionTrack("N/A", "N/A", "0%", "Awaiting Matrix Core Data", false)
            return UnifiedTickState(extractedDigit, 0f, "AWAITING_DATA", emptyList(), awaitingTrack, awaitingTrack)
        }

        // Take snapshot list for immutable atomic batch calculation
        val currentHistory = synchronized(slidingTickCache) { slidingTickCache.toList() }

        // 2. Compute Global Parity Balance Strain & Stability Score
        val oddCount = currentHistory.count { it % 2 != 0 }
        val oddPercentage = (oddCount.toFloat() / actualMacroSize) * 100f
        val evenPercentage = 100f - oddPercentage

        val rawDivergence = abs(oddPercentage - evenPercentage)
        val calculatedStability = ((rawDivergence / 25.0f) * 100f).coerceIn(0f, 100f)

        // HARD COOLDOWN MATRIX CHECK: If market is sideways, drop both immediately
        if (calculatedStability < 40.0f) {
            val lockedTrack = ProfileExecutionTrack("NONE", "N/A", "0%", if (isOneSecond) "1S COOLDOWN" else "CHOPPY DEAD ZONE", false)
            return UnifiedTickState(extractedDigit, calculatedStability, "CHOPPY DEAD ZONE", emptyList(), lockedTrack, lockedTrack)
        }

        // 3. Determine Reversion Regime Targets
        val globalRegime = if (evenPercentage >= 55.0f) {
            "REVERSION_TO_EVEN"
        } else if (oddPercentage >= 55.0f) {
            "REVERSION_TO_ODD"
        } else {
            "BALANCED_COOLDOWN"
        }

        if (globalRegime == "BALANCED_COOLDOWN") {
            val coolingTrack = ProfileExecutionTrack("NONE", "N/A", "0%", "BALANCED_COOLDOWN", false)
            return UnifiedTickState(extractedDigit, calculatedStability, globalRegime, emptyList(), coolingTrack, coolingTrack)
        }

        // 4. UNBIASED GLOBAL STARVATION ENGINE (Scans ALL 10 digits for pure drought)
        val macroFrequencies = IntArray(10)
        currentHistory.forEach { macroFrequencies[it]++ }

        val lowestMacroCount = macroFrequencies.minOrNull() ?: 0
        val rawStarvedDigits = (0..9).filter { macroFrequencies[it] == lowestMacroCount }

        val final3Candidates = mutableListOf<Int>()

        // CASE 1: FLOODING TRAP (4 OR 5 DIGITS ARE EQUALLY STARVED)
        if (rawStarvedDigits.size > 3) {
            val microHistory = currentHistory.takeLast(actualMicroSize)
            val microFrequencies = IntArray(10)
            microHistory.forEach { microFrequencies[it]++ }

            val sortedByRecentDrought = rawStarvedDigits.sortedWith(
                compareBy<Int> { microFrequencies[it] } // Coldest in last 10 ticks
                .thenBy { it } // Fallback to maintain order
            )
            final3Candidates.addAll(sortedByRecentDrought.take(3))
        }
        // CASE 2: PERFECT STRUCTURE (Exactly 3 digits found)
        else if (rawStarvedDigits.size == 3) {
            final3Candidates.addAll(rawStarvedDigits)
        }
        // CASE 3: VACUUM TRAP (ONLY 2 DIGITS ARE STARVED)
        else if (rawStarvedDigits.size == 2) {
            final3Candidates.addAll(rawStarvedDigits)
            val primaryAnchor1 = final3Candidates[0]

            val spatialQuadrantPool = when {
                primaryAnchor1 <= 3 -> listOf(0, 1, 2, 3)
                primaryAnchor1 >= 6 -> listOf(6, 7, 8, 9)
                else -> listOf(4, 5)
            }

            val paddingCandidate = spatialQuadrantPool
                .filter { it !in final3Candidates }
                .minByOrNull { macroFrequencies[it] }
                ?: (0..9).filter { it !in final3Candidates }.minByOrNull { macroFrequencies[it] } ?: 0
            final3Candidates.add(paddingCandidate)
        }
        // CASE 4: EXTREME LIQUIDITY CORRIDOR (Only 1 digit starved)
        else {
            final3Candidates.addAll(rawStarvedDigits)
            val sequentialColdest = (0..9)
                .filter { it !in final3Candidates }
                .sortedBy { macroFrequencies[it] }
                .take(2)
            final3Candidates.addAll(sequentialColdest)
        }

        val parsedCandidates = final3Candidates.take(3)

        // 5. Parallel Track Execution Compiler Engine using mathematically locked out-of-span strategy compiler
        val riskyEvaluatedTrack = compileDefinitiveContract(parsedCandidates, extractedDigit, macroFrequencies, oddPercentage, evenPercentage, isSafer = false)
        val saferEvaluatedTrack = compileDefinitiveContract(parsedCandidates, extractedDigit, macroFrequencies, oddPercentage, evenPercentage, isSafer = true)

        return UnifiedTickState(
            lastExtractedDigit = extractedDigit,
            stabilityScore = calculatedStability,
            globalRegime = globalRegime,
            activeCandidates = parsedCandidates,
            riskyProfile = riskyEvaluatedTrack,
            saferProfile = saferEvaluatedTrack
        )
    }

    /**
     * Translates the normalized 3-digit cluster into a mathematically sound trade.
     * Enforces raw limit gates, stability bounds, and defensive cushioning via
     * the weight-aware QuantitativeContractCompiler out-of-span strategy engine.
     */
    private fun compileDefinitiveContract(
        normalizedCandidates: List<Int>,
        extractedDigit: Int,
        macroFrequencies: IntArray,
        oddPercentage: Float,
        evenPercentage: Float,
        isSafer: Boolean
    ): ProfileExecutionTrack {
        if (normalizedCandidates.size < 3) {
            return ProfileExecutionTrack("NONE", "N/A", "0%", "INCOMPLETE", false)
        }

        val compiler = QuantitativeContractCompiler()
        val strategyResult = compiler.compileContractStrategy(
            currentDigit = extractedDigit,
            frequencies = macroFrequencies,
            oddPercentage = oddPercentage,
            evenPercentage = evenPercentage,
            completeCandidates = normalizedCandidates,
            isSafer = isSafer
        )

        // Convert StratResult contract type
        val mappedContractType = when (strategyResult.chosenContractType) {
            "DIGITUNDER" -> "UNDER"
            "DIGITOVER" -> "OVER"
            "DIGITEVEN" -> "EVEN"
            "DIGITODD" -> "ODD"
            "DIGITDIFF" -> "DIFFERS"
            else -> "NONE"
        }

        val barrierParam = strategyResult.chosenBarrierParameter

        // Logging exactly to the millisecond
        android.util.Log.d(
            "HighFrequencyStrategyLogger",
            "[${System.currentTimeMillis()}] TUPLE: $normalizedCandidates | CALCULATED SPAN: ${strategyResult.activeSpan} | STRATEGY VERDICT: $mappedContractType | BARRIER PARAM: $barrierParam"
        )

        val payoutVal = when (mappedContractType) {
            "UNDER" -> getUnderPayoutFor(barrierParam.toIntOrNull() ?: -1)
            "OVER" -> getOverPayoutFor(barrierParam.toIntOrNull() ?: -1)
            "DIFFERS" -> "~11%"
            "EVEN", "ODD" -> "~100%"
            else -> "0%"
        }

        return ProfileExecutionTrack(
            contractType = mappedContractType,
            barrierParameter = barrierParam,
            brokerPayoutPct = payoutVal,
            validationMessage = strategyResult.structuralDiagnosticLog,
            isSafeToExecute = strategyResult.isFilterSequencePassed
        )
    }

    private fun getUnderPayoutFor(barrier: Int): String {
        return when (barrier) {
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
    }

    private fun getOverPayoutFor(barrier: Int): String {
        return when (barrier) {
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
    }

    fun getSlidingTickCache(): List<Int> {
        return synchronized(slidingTickCache) { slidingTickCache.toList() }
    }

    fun getMacroFrequencies(): IntArray {
        val freqs = IntArray(10) { 0 }
        val snapshot = getSlidingTickCache()
        for (digit in snapshot) {
            if (digit in 0..9) {
                freqs[digit]++
            }
        }
        return freqs
    }
}

