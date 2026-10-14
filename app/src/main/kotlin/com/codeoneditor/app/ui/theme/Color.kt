package com.codeoneditor.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val bg: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LightAppColors = AppColors(
    bg = Color.White,
    surface = Color(0xFFF5F5F5),
    border = Color(0xFFD6D6D6),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF555555)
)

val DarkAppColors = AppColors(
    bg = Color.Black,
    surface = Color(0xFF111111),
    border = Color(0xFF2A2A2A),
    textPrimary = Color.White,
    textSecondary = Color(0xFF9A9A9A)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val HighDensityBg: Color
    @androidx.compose.runtime.Composable get() = LocalAppColors.current.bg

val HighDensitySurface: Color
    @androidx.compose.runtime.Composable get() = LocalAppColors.current.surface

val HighDensityBorder: Color
    @androidx.compose.runtime.Composable get() = LocalAppColors.current.border

val HighDensityTextPrimary: Color
    @androidx.compose.runtime.Composable get() = LocalAppColors.current.textPrimary

val HighDensityTextSecondary: Color
    @androidx.compose.runtime.Composable get() = LocalAppColors.current.textSecondary
