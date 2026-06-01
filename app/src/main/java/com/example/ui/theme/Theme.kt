package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IndigoColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    secondary = IndigoSecondary,
    background = IndigoBackground,
    surface = IndigoSurface,
    onPrimary = Color.White,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFF1F5F9)
)

private val EmeraldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    background = EmeraldBackground,
    surface = EmeraldSurface,
    onPrimary = Color.White,
    onBackground = Color(0xFFE0F2F1),
    onSurface = Color(0xFFE0F2F1)
)

private val CosmicColorScheme = darkColorScheme(
    primary = CosmicPrimary,
    secondary = CosmicSecondary,
    background = CosmicBackground,
    surface = CosmicSurface,
    onPrimary = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9)
)

private val SunriseColorScheme = darkColorScheme(
    primary = SunrisePrimary,
    secondary = SunriseSecondary,
    background = SunriseBackground,
    surface = SunriseSurface,
    onPrimary = Color.Black,
    onBackground = Color(0xFFFFE0B2),
    onSurface = Color(0xFFFFE0B2)
)

// NEW LIGHT COLOR SCHEMES
private val IndigoLightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    secondary = IndigoSecondary,
    background = Color(0xFFF1F5F9),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

private val EmeraldLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    background = Color(0xFFF0FDF4),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF064E3B),
    onSurface = Color(0xFF064E3B)
)

private val CosmicLightColorScheme = lightColorScheme(
    primary = CosmicPrimary,
    secondary = CosmicSecondary,
    background = Color(0xFFF0F9FF),
    surface = Color.White,
    onPrimary = Color(0xFF0C4A6E),
    onBackground = Color(0xFF0C4A6E),
    onSurface = Color(0xFF0C4A6E)
)

private val SunriseLightColorScheme = lightColorScheme(
    primary = SunrisePrimary,
    secondary = SunriseSecondary,
    background = Color(0xFFFFFBEB),
    surface = Color.White,
    onPrimary = Color.Black,
    onBackground = Color(0xFF78350F),
    onSurface = Color(0xFF78350F)
)

@Composable
fun MyApplicationTheme(
    themeName: String = "Google Indigo",
    isDarkMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) {
        when (themeName) {
            "Google Emerald" -> EmeraldColorScheme
            "Google Cosmic" -> CosmicColorScheme
            "Pixel Sunrise" -> SunriseColorScheme
            else -> IndigoColorScheme
        }
    } else {
        when (themeName) {
            "Google Emerald" -> EmeraldLightColorScheme
            "Google Cosmic" -> CosmicLightColorScheme
            "Pixel Sunrise" -> SunriseLightColorScheme
            else -> IndigoLightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
