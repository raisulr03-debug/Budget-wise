package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MintLight,
    onPrimary = CarbonDark,
    primaryContainer = MintGreenPrimary,
    onPrimaryContainer = Color.White,
    secondary = ChampagneGold,
    onSecondary = CarbonDark,
    background = CarbonDark,
    surface = CarbonSurface,
    onBackground = HighContrastText,
    onSurface = HighContrastText,
    surfaceVariant = Color(0xFF1E2421),
    onSurfaceVariant = Color(0xFFCFD8DC),
    error = CrimsonRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = MintGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = MintGreenPrimary,
    secondary = ChampagneGold,
    onSecondary = CarbonDark,
    background = Color(0xFFF9FBF9),
    surface = Color.White,
    onBackground = CarbonDark,
    onSurface = CarbonDark,
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF37474F),
    error = CrimsonRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamicColor false by default to preserve the luxury Brand style!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
