package com.rnandresy.lol.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Couleurs admin — dorées dans TOUS les thèmes ─────────────────────────────
val AdminGold      = Color(0xFFFFD700)
val AdminGoldSoft  = Color(0xFFFFE566)
val AdminGoldDim   = Color(0xFFB8860B)
val AdminGoldBg    = Color(0x1AFFD700)

// ─────────────────────────────────────────────────────────────────────────────
// NOIR & BLANC — thème par défaut
// ─────────────────────────────────────────────────────────────────────────────
private val bwBlack      = Color(0xFF050505)
private val bwBlackCard  = Color(0xFF0F0F0F)
private val bwBlackSurf  = Color(0xFF1A1A1A)
private val bwBlackSurf2 = Color(0xFF242424)
private val bwWhite      = Color(0xFFFFFFFF)
private val bwGray1      = Color(0xFFCCCCCC)
private val bwGray2      = Color(0xFF888888)
private val bwGray3      = Color(0xFF444444)
private val bwGray4      = Color(0xFF2A2A2A)
private val bwRed        = Color(0xFFFF3B3B)

private val BlackWhiteScheme = darkColorScheme(
    primary              = bwWhite,
    onPrimary            = bwBlack,
    primaryContainer     = bwBlackSurf,
    onPrimaryContainer   = bwWhite,
    secondary            = bwGray2,
    onSecondary          = bwWhite,
    secondaryContainer   = bwBlackSurf2,
    onSecondaryContainer = bwGray1,
    tertiary             = bwGray1,
    onTertiary           = bwBlack,
    tertiaryContainer    = bwBlackSurf,
    onTertiaryContainer  = bwWhite,
    background           = bwBlack,
    onBackground         = bwWhite,
    surface              = bwBlackCard,
    onSurface            = bwWhite,
    surfaceVariant       = bwBlackSurf,
    onSurfaceVariant     = bwGray2,
    outline              = bwGray4,
    outlineVariant       = bwGray3,
    error                = bwRed,
    onError              = bwWhite,
    errorContainer       = Color(0xFF2A0000),
    onErrorContainer     = bwRed,
    inverseSurface       = bwWhite,
    inverseOnSurface     = bwBlack,
    inversePrimary       = bwBlack,
    scrim                = Color(0x99000000)
)

// ─────────────────────────────────────────────────────────────────────────────
// NÉON — violet et cyan sur noir profond
// ─────────────────────────────────────────────────────────────────────────────
private val nBg     = Color(0xFF02020A)
private val nCard   = Color(0xFF07071A)
private val nSurf   = Color(0xFF0D0D28)
private val nSurf2  = Color(0xFF141438)
private val nPurple = Color(0xFF9D4EDD)
private val nCyan   = Color(0xFF00D4FF)
private val nPink   = Color(0xFFFF1E8C)
private val nText   = Color(0xFFE8E0FF)
private val nGray   = Color(0xFF7A6B99)
private val nOut    = Color(0xFF1E1845)

private val NeonScheme = darkColorScheme(
    primary              = nPurple,
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFF1A0040),
    onPrimaryContainer   = nPurple,
    secondary            = nCyan,
    onSecondary          = Color(0xFF001A22),
    secondaryContainer   = Color(0xFF002233),
    onSecondaryContainer = nCyan,
    tertiary             = nPink,
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFF2A0018),
    onTertiaryContainer  = nPink,
    background           = nBg,
    onBackground         = nText,
    surface              = nCard,
    onSurface            = nText,
    surfaceVariant       = nSurf,
    onSurfaceVariant     = nGray,
    outline              = nOut,
    outlineVariant       = Color(0xFF130F2A),
    error                = Color(0xFFFF4060),
    onError              = Color.White,
    errorContainer       = Color(0xFF220010),
    onErrorContainer     = Color(0xFFFF4060),
    inverseSurface       = nText,
    inverseOnSurface     = nBg,
    inversePrimary       = nPurple
)

// ─────────────────────────────────────────────────────────────────────────────
// NOSTALGIQUE — sépia chaud sur brun foncé
// ─────────────────────────────────────────────────────────────────────────────
private val xBg     = Color(0xFF0A0702)
private val xCard   = Color(0xFF140E05)
private val xSurf   = Color(0xFF1E1509)
private val xSurf2  = Color(0xFF281C0D)
private val xGold   = Color(0xFFD4A847)
private val xAmber  = Color(0xFF8B6A30)
private val xCream  = Color(0xFFE8D5A0)
private val xGray   = Color(0xFF8A7355)
private val xOut    = Color(0xFF3A2A10)

private val NostalgicScheme = darkColorScheme(
    primary              = xGold,
    onPrimary            = Color(0xFF0A0702),
    primaryContainer     = Color(0xFF2A1E00),
    onPrimaryContainer   = xGold,
    secondary            = xAmber,
    onSecondary          = Color(0xFF0A0702),
    secondaryContainer   = Color(0xFF1E1400),
    onSecondaryContainer = xGold,
    tertiary             = Color(0xFFB09060),
    onTertiary           = Color(0xFF0A0702),
    background           = xBg,
    onBackground         = xCream,
    surface              = xCard,
    onSurface            = xCream,
    surfaceVariant       = xSurf,
    onSurfaceVariant     = xGray,
    outline              = xOut,
    outlineVariant       = Color(0xFF1E1508),
    error                = Color(0xFFCC4433),
    onError              = Color.White,
    errorContainer       = Color(0xFF220A00),
    onErrorContainer     = Color(0xFFCC4433),
    inverseSurface       = xCream,
    inverseOnSurface     = xBg,
    inversePrimary       = xGold
)

// ─────────────────────────────────────────────────────────────────────────────
enum class AppTheme(val displayName: String, val emoji: String) {
    BLACK_WHITE("Noir & Blanc", "◼"),
    NEON("Néon", "💜"),
    NOSTALGIC("Nostalgique", "🕯")
}

val AskipTypography = Typography(
    displayLarge  = TextStyle(fontWeight = FontWeight.Black,     fontSize = 34.sp, letterSpacing = (-1).sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    displaySmall  = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 24.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 22.sp),
    headlineMedium= TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 20.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 18.sp),
    titleLarge    = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 18.sp),
    titleMedium   = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 16.sp),
    titleSmall    = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 14.sp),
    bodyLarge     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge    = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 12.sp, letterSpacing = 0.1.sp),
    labelSmall    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 11.sp, letterSpacing = 0.2.sp)
)

@Composable
fun AskipTheme(appTheme: AppTheme = AppTheme.BLACK_WHITE, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = when (appTheme) {
            AppTheme.BLACK_WHITE -> BlackWhiteScheme
            AppTheme.NEON        -> NeonScheme
            AppTheme.NOSTALGIC   -> NostalgicScheme
        },
        typography = AskipTypography,
        content    = content
    )
}