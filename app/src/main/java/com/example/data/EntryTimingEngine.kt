package com.example.data

import com.example.ui.viewmodel.LiveTradeSignal

data class DigitPlane(
    val name: String,                  // "UNDER" or "OVER"
    val digits: List<Int>,             // digits in this plane
    val barrier: Int,                  // barrier value
    val targetPercentage: Float,        // sum of individual digit percentages (e.g., 40.0f)
    val accumulatedPercentage: Float,  // current accumulated percentage (e.g. count/total * 100)
    val digitOccurrences: Map<Int, Int> // actual counts/frequencies of each digit
)

data class EntryTimingResult(
    val signal: LiveTradeSignal,
    val underPlane: DigitPlane,
    val overPlane: DigitPlane,
    val isEntryReady: Boolean,
    val remainingPercentage: Float,
    val recommendedAction: String // "WAIT", "ENTER_UNDER", "ENTER_OVER"
)

class EntryTimingEngine {

    /**
     * Estimates and compiles the entry timing state based on post-signal ticks.
     */
    fun evaluateEntryTiming(
        signal: LiveTradeSignal,
        postSignalTicks: List<Int>
    ): EntryTimingResult {
        val barrierVal = signal.barrier.toIntOrNull() ?: 4
        
        // Split digits 0-9 into two planes based on the barrier
        val underPlaneDigits = (0 until barrierVal).toList()
        val overPlaneDigits = ((barrierVal + 1)..9).toList()
        
        // Base target percentage calculations (each digit has 10% base chance)
        val underTarget = underPlaneDigits.size * 10f
        val overTarget = overPlaneDigits.size * 10f
        
        // Counts for under and over plane digits
        val digitCounts = postSignalTicks.groupingBy { it }.eachCount()
        
        val underOccurrences = underPlaneDigits.associateWith { digitCounts[it] ?: 0 }
        val overOccurrences = overPlaneDigits.associateWith { digitCounts[it] ?: 0 }
        
        val totalTicks = postSignalTicks.size
        
        val underAccumulated = if (totalTicks > 0) {
            (underPlaneDigits.sumOf { digitCounts[it] ?: 0 }.toFloat() / totalTicks) * 100f
        } else {
            0f
        }
        
        val overAccumulated = if (totalTicks > 0) {
            (overPlaneDigits.sumOf { digitCounts[it] ?: 0 }.toFloat() / totalTicks) * 100f
        } else {
            0f
        }
        
        val underPlane = DigitPlane(
            name = "UNDER",
            digits = underPlaneDigits,
            barrier = barrierVal,
            targetPercentage = underTarget,
            accumulatedPercentage = underAccumulated,
            digitOccurrences = underOccurrences
        )
        
        val overPlane = DigitPlane(
            name = "OVER",
            digits = overPlaneDigits,
            barrier = barrierVal,
            targetPercentage = overTarget,
            accumulatedPercentage = overAccumulated,
            digitOccurrences = overOccurrences
        )
        
        // Check signal side and evaluate opposite plane's criteria
        val isBarrierContract = signal.contractType == "UNDER" || signal.contractType == "OVER"
        
        if (!isBarrierContract) {
            // For non-barrier contracts, default to instantly ready so we don't break them
            return EntryTimingResult(
                signal = signal,
                underPlane = underPlane,
                overPlane = overPlane,
                isEntryReady = true,
                remainingPercentage = 0f,
                recommendedAction = if (signal.contractType == "EVEN") "ENTER_EVEN" else if (signal.contractType == "ODD") "ENTER_ODD" else "ENTER_DIFFERS"
            )
        }
        
        val isUnderSignal = signal.contractType == "UNDER"
        val oppositePlane = if (isUnderSignal) overPlane else underPlane
        val oppositeAccumulated = oppositePlane.accumulatedPercentage
        val oppositeTarget = oppositePlane.targetPercentage
        
        // Entry triggers when the opposite plane meets or exceeds its target percentage
        // If there are no ticks yet, wait for at least 1 tick before triggering to calculate correctly
        val isEntryReady = totalTicks > 0 && oppositeAccumulated >= oppositeTarget
        
        val remainingPercentage = (oppositeTarget - oppositeAccumulated).coerceAtLeast(0f)
        
        val recommendedAction = if (isEntryReady) {
            if (isUnderSignal) "ENTER_UNDER" else "ENTER_OVER"
        } else {
            "WAIT"
        }
        
        return EntryTimingResult(
            signal = signal,
            underPlane = underPlane,
            overPlane = overPlane,
            isEntryReady = isEntryReady,
            remainingPercentage = remainingPercentage,
            recommendedAction = recommendedAction
        )
    }
}
