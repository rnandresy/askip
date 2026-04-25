package com.rnandresy.lol.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Palette
val PurplePrimary = Color(0xFF7C4DFF)
val PurpleVariant = Color(0xFF6200EE)
val Cyan = Color(0xFF00E5FF)
val DarkBg = Color(0xFF0D0D1A)
val DarkSurface = Color(0xFF1A1A2E)
val DarkSurfaceVariant = Color(0xFF252540)
val DarkOutline = Color(0xFF3D3D60)
val OnDarkPrimary = Color.White
val ErrorRed = Color(0xFFFF5252)
val SuccessGreen = Color(0xFF69F0AE)

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3700B3),
    onPrimaryContainer = Color(0xFFBB86FC),
    secondary = Cyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D57),
    onSecondaryContainer = Cyan,
    background = DarkBg,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0B0D0),
    outline = DarkOutline,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PurpleVariant,
    onPrimary = Color.White,
    secondary = Color(0xFF018786),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF121212),
    surface = Color.White,
    onSurface = Color(0xFF121212),
    surfaceVariant = Color(0xFFEEEEF5),
    onSurfaceVariant = Color(0xFF444466),
    outline = Color(0xFFCCCCDD),
    error = ErrorRed
)

@Composable
fun AskipTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = if (dark) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(
            displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
            titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
            bodyLarge = TextStyle(fontSize = 16.sp),
            bodyMedium = TextStyle(fontSize = 14.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
            labelSmall = TextStyle(fontSize = 11.sp)
        ),
        content = content
    )
}