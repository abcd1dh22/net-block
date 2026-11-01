package com.zihad.netblock.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** iOS-style palette. Follows the system light/dark setting. */
@Immutable
data class AppColors(
    val bg: Color,
    val card: Color,
    val fill: Color,
    val separator: Color,
    val label: Color,
    val secondary: Color,
    val blue: Color,
    val red: Color,
    val green: Color,
    val selectedSegment: Color,
    val switchOff: Color,
    val isDark: Boolean
)

private val LightPalette = AppColors(
    bg = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    fill = Color(0xFFE3E3E8),
    separator = Color(0xFFD1D1D6),
    label = Color(0xFF000000),
    secondary = Color(0xFF6C6C70),
    blue = Color(0xFF007AFF),
    red = Color(0xFFFF3B30),
    green = Color(0xFF34C759),
    selectedSegment = Color(0xFFFFFFFF),
    switchOff = Color(0xFFE9E9EA),
    isDark = false
)

private val DarkPalette = AppColors(
    bg = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    fill = Color(0xFF2C2C2E),
    separator = Color(0xFF38383A),
    label = Color(0xFFFFFFFF),
    secondary = Color(0xFF8E8E93),
    blue = Color(0xFF0A84FF),
    red = Color(0xFFFF453A),
    green = Color(0xFF30D158),
    selectedSegment = Color(0xFF636366),
    switchOff = Color(0xFF39393D),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { DarkPalette }

private val Type = Typography(
    headlineMedium = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
)

@Composable
fun NetBlockTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.blue, onPrimary = Color.White,
            background = c.bg, onBackground = c.label,
            surface = c.card, onSurface = c.label,
            surfaceVariant = c.fill, onSurfaceVariant = c.secondary,
            error = c.red
        )
    } else {
        lightColorScheme(
            primary = c.blue, onPrimary = Color.White,
            background = c.bg, onBackground = c.label,
            surface = c.card, onSurface = c.label,
            surfaceVariant = c.fill, onSurfaceVariant = c.secondary,
            error = c.red
        )
    }
    CompositionLocalProvider(LocalAppColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = Type, content = content)
    }
}
