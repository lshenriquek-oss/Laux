package com.codeoneditor.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Pure Monochrome Light Theme (#FFFFFF background, black/gray text)
private val LightMonochromeColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBEBEB),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF333333),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF555555),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF4F4F4),
    onSurfaceVariant = Color(0xFF4A4A4A),
    outline = Color(0xFFCCCCCC),
    outlineVariant = Color(0xFFE0E0E0),
    error = Color(0xFF000000),
    onError = Color(0xFFFFFFFF)
)

// Pure Monochrome Dark Theme (#000000 background, white/gray text)
private val DarkMonochromeColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF222222),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFAAAAAA),
    onTertiary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF262626),
    error = Color(0xFFFFFFFF),
    onError = Color(0xFF000000)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val appColors = if (darkTheme) DarkAppColors else LightAppColors
    val colorScheme = if (darkTheme) DarkMonochromeColorScheme else LightMonochromeColorScheme

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}


