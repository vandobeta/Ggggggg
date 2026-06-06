package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Indigo Theme palette
val IndigoPrimary = Color(0xFF3F51B5)
val IndigoSecondary = Color(0xFF303F9F)
val IndigoBackground = Color(0xFF0A0F24)
val IndigoSurface = Color(0xFF151C3F)

// Emerald Theme palette
val EmeraldPrimary = Color(0xFF009688)
val EmeraldSecondary = Color(0xFF00796B)
val EmeraldBackground = Color(0xFF001F1B)
val EmeraldSurface = Color(0xFF003730)

// Cosmic Slate palette
val CosmicPrimary = Color(0xFF38BDF8)
val CosmicSecondary = Color(0xFF0369A1)
val CosmicBackground = Color(0xFF0B0F13)
val CosmicSurface = Color(0xFF11171E)

// Sunrise Theme palette
val SunrisePrimary = Color(0xFFF59E0B)
val SunriseSecondary = Color(0xFFD97706)
val SunriseBackground = Color(0xFF1E1B10)
val SunriseSurface = Color(0xFF2E2616)

// Comic Indigo/Sunrise etc...

// Quadrant Colors
val ColorLowerOdd = Color(0xFF8B5CF6)    // Purple / Indigo
val ColorLowerEven = Color(0xFFFBBF24)   // Amber / Golden
val ColorHigherOdd = Color(0xFFF43F5E)   // Rose / Pink
val ColorHigherEven = Color(0xFF06B6D4)  // Cyan / Sky Blue

fun getDigitColor(digit: Int): Color {
    return when (digit) {
        1, 3 -> ColorLowerOdd
        0, 2, 4 -> ColorLowerEven
        5, 7, 9 -> ColorHigherOdd
        6, 8 -> ColorHigherEven
        else -> Color.Gray
    }
}

fun getQuadrantColor(quadrant: String): Color {
    val q = quadrant.uppercase().trim()
    return when {
        q.contains("LOWER ODD") || q == "LO" -> ColorLowerOdd
        q.contains("LOWER EVEN") || q == "LE" -> ColorLowerEven
        q.contains("HIGHER ODD") || q == "HO" -> ColorHigherOdd
        q.contains("HIGHER EVEN") || q == "HE" -> ColorHigherEven
        else -> Color.Gray
    }
}

