package com.quant.algoterminal

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.example.data.QuantitativeContractCompiler

class StrategyCompilerTest {

    private lateinit var compiler: QuantitativeContractCompiler

    @Before
    fun setUp() {
        compiler = QuantitativeContractCompiler()
    }

    @Test
    fun verify_SIG_48944_Cushion_Enforcement() {
        // Mock data directly from your failing live UI log screenshot
        val currentDigit = 4
        val frequencies = intArrayOf(5, 4, 3, 2, 6, 8, 5, 4, 3, 10) // Mock lookback map
        val oddPercentage = 34.0f
        val evenPercentage = 66.0f // Even dominates, pulling reversion gravity to Odds
        val completeCandidates = listOf(1, 0, 4) // [Primary Anchor, Noise Guard 1, Noise Guard 2]

        val result = compiler.compileContractStrategy(
            currentDigit = currentDigit,
            frequencies = frequencies,
            oddPercentage = oddPercentage,
            evenPercentage = evenPercentage,
            completeCandidates = completeCandidates
        )

        // ASSERTIONS TO LOCK OUT FAULTY AI LOGIC:
        assertTrue("Strategy should be approved", result.isFilterSequencePassed)
        assertEquals("Should choose primary engine OVER_UNDER", "DIGITUNDER", result.chosenContractType)
        
        // The exact fix: max candidate is 4. (4 + 2) = 6. It must NOT recommend UNDER 4.
        assertNotEquals("CRITICAL FAULT: App recommended barrier inside the chaos span!", "4", result.chosenBarrierParameter)
        assertEquals("Airtight protection buffer must select barrier 6", "6", result.chosenBarrierParameter)
    }

    @Test
    fun verify_Dead_Zone_Lockout_Suspension() {
        // Test Case: Unstable market split (51% Even vs 49% Odd)
        val completeCandidates = listOf(3, 2, 4)
        
        val result = compiler.compileContractStrategy(
            currentDigit = 3,
            frequencies = IntArray(10),
            oddPercentage = 49.0f,
            evenPercentage = 51.0f,
            completeCandidates = completeCandidates
        )

        // Market Stability Score evaluates below 40%
        assertFalse("Dead zone must immediately suspend signals", result.isFilterSequencePassed)
        assertEquals("Contract family must be rejected", "NONE", result.chosenContractType)
        assertEquals("Barrier must be neutralized", "-1", result.chosenBarrierParameter)
    }

    @Test
    fun verify_Absolute_Span9_Chaos_Blacklist() {
        // Test Case: Total market dispersion across the absolute floor and ceiling [1, 9, 0]
        val completeCandidates = listOf(1, 9, 0) // Max 9, Min 0 -> Span of 9

        val result = compiler.compileContractStrategy(
            currentDigit = 0,
            frequencies = IntArray(10),
            oddPercentage = 40.0f,
            evenPercentage = 60.0f,
            completeCandidates = completeCandidates
        )

        assertFalse("Span 9 has zero edge and must be blacklisted", result.isFilterSequencePassed)
        assertEquals("Contract execution must abort", "NONE", result.chosenContractType)
    }
}
