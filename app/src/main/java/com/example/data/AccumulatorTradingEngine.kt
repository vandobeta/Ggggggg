package com.example.data

data class AccumulatorState(
    val contractId: Long,
    val symbol: String,
    val growthRate: Float,        // 0.01 to 0.05
    val currentBarrier: Double,   // Current accumulated barrier level
    val ticksStayedIn: Int,       // Ticks maintaining position
    val lastProfit: Double,
    val isSpiraling: Boolean = false,  // FAILSAFE FLAG
    val spiralWarningTriggered: Boolean = false
)

data class MomentumAnalysis(
    val isSafeToEnter: Boolean,
    val momentumDirection: String, // "UP", "DOWN", "SIDEWAYS"
    val momentumVelocity: Int,
    val spiralRiskScore: Float,
    val recommendation: String
)

data class AutoSellDecision(
    val shouldSell: Boolean,
    val reason: String,
    val exitPrice: Double
)

class AccumulatorTradingEngine {
    
    companion object {
        const val SPIRAL_VELOCITY_THRESHOLD = 3    // 3 consecutive same-direction ticks
        const val SPIRAL_MOMENTUM_THRESHOLD = 0.75f // 75% momentum concentration
        const val BARRIER_DROP_THRESHOLD = 0.02    // 2% barrier drop triggers warning
        const val AUTO_SELL_TRIGGER = 5            // Sell after 5 spiral warnings
    }
    
    fun analyzeMomentumForAccumulator(
        tickHistory: List<Int>,
        lookbackWindow: Int = 20
    ): MomentumAnalysis {
        val recentTicks = tickHistory.takeLast(lookbackWindow)
        if (recentTicks.size < 5) {
            return MomentumAnalysis(
                isSafeToEnter = true,
                momentumDirection = "SIDEWAYS",
                momentumVelocity = 0,
                spiralRiskScore = 0f,
                recommendation = "INSUFFICIENT DATA: Default entry allowed."
            )
        }

        val netMovement = calculateNetMomentum(recentTicks)
        val velocity = calculateMomentumVelocity(recentTicks)
        val spiralRisk = detectSpiralPattern(recentTicks)
        
        val isSafe = spiralRisk < SPIRAL_MOMENTUM_THRESHOLD
        val direction = when {
            netMovement > 0.5f -> "UP"
            netMovement < -0.5f -> "DOWN"
            else -> "SIDEWAYS"
        }

        return MomentumAnalysis(
            isSafeToEnter = isSafe,
            momentumDirection = direction,
            momentumVelocity = velocity,
            spiralRiskScore = spiralRisk,
            recommendation = when {
                spiralRisk >= 0.9f -> "CRITICAL: DO NOT ENTER (Severe spiral risk)"
                spiralRisk >= SPIRAL_MOMENTUM_THRESHOLD -> "HIGH RISK: Consider MULTDOWN/MULTUP"
                velocity > 2 -> "TREND STRONG: Wait for pullback"
                else -> "SAFE: Good entry point"
            }
        )
    }
    
    fun shouldAutoSell(state: AccumulatorState): AutoSellDecision {
        if (state.contractId == -1L) return AutoSellDecision(false, "No active contract", 0.0)
        
        val sellReason = when {
            state.ticksStayedIn >= 200 -> "Max ticks reached (Failsafe trigger at 200 Ticks)"
            state.isSpiraling && state.ticksStayedIn >= 50 -> "Extended spiral detected during active trade"
            state.spiralWarningTriggered && state.ticksStayedIn >= 30 -> "Active spiral warning threshold breached"
            state.lastProfit < -0.5 * state.currentBarrier -> "Significant draw-down tolerance exceeded"
            else -> null
        }
        
        return if (sellReason != null) {
            AutoSellDecision(
                shouldSell = true,
                reason = sellReason,
                exitPrice = state.lastProfit // exit current valuation
            )
        } else {
            AutoSellDecision(false, "Hold position", state.lastProfit)
        }
    }

    private fun calculateNetMomentum(ticks: List<Int>): Float {
        var directionScore = 0f
        for (i in 1 until ticks.size) {
            val diff = ticks[i] - ticks[i - 1]
            if (diff > 0) directionScore += 0.1f
            else if (diff < 0) directionScore -= 0.1f
        }
        return directionScore
    }

    private fun calculateMomentumVelocity(ticks: List<Int>): Int {
        var maxStreak = 0
        var currentStreak = 0
        var lastDirection = 0 // 1: UP, -1: DOWN, 0: START
        
        for (i in 1 until ticks.size) {
            val direction = when {
                ticks[i] > ticks[i - 1] -> 1
                ticks[i] < ticks[i - 1] -> -1
                else -> 0
            }
            if (direction == 0) {
                currentStreak = 0
                continue
            }
            if (direction == lastDirection) {
                currentStreak++
            } else {
                currentStreak = 1
                lastDirection = direction
            }
            if (currentStreak > maxStreak) {
                maxStreak = currentStreak
            }
        }
        return maxStreak
    }

    private fun detectSpiralPattern(ticks: List<Int>): Float {
        // Spiral pattern indicates persistent directional bias (same-direction clusters)
        var upMoves = 0
        var downMoves = 0
        val totalMoves = ticks.size - 1
        if (totalMoves <= 0) return 0f

        for (i in 1 until ticks.size) {
            if (ticks[i] > ticks[i - 1]) upMoves++
            else if (ticks[i] < ticks[i - 1]) downMoves++
        }
        val maxMoves = kotlin.math.max(upMoves, downMoves)
        return maxMoves.toFloat() / totalMoves.toFloat()
    }
}
