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
    primary = ElegantGoldAccent,
    secondary = ElegantGoldAccent,
    tertiary = ElegantGoldAccent,
    background = PureDarkBackground,
    surface = ElegantCardSurface,
    onPrimary = PureDarkBackground,
    onSecondary = PureDarkBackground,
    onBackground = ElegantWhite,
    onSurface = ElegantTextPrimary,
    surfaceVariant = ElegantBarBackground,
    onSurfaceVariant = ElegantTextMuted
)

private val LightColorScheme = darkColorScheme(
    primary = ElegantGoldAccent,
    secondary = ElegantGoldAccent,
    tertiary = ElegantGoldAccent,
    background = PureDarkBackground,
    surface = ElegantCardSurface,
    onPrimary = PureDarkBackground,
    onSecondary = PureDarkBackground,
    onBackground = ElegantWhite,
    onSurface = ElegantTextPrimary,
    surfaceVariant = ElegantBarBackground,
    onSurfaceVariant = ElegantTextMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep styling consistent for luxury (override dynamic colors with premium brand colors by default)
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
