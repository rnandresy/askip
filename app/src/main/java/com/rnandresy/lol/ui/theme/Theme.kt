package com.rnandresy.lol.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═════════════════════════════════════════════════════════════════════════════
//  Thèmes
// ═════════════════════════════════════════════════════════════════════════════

enum class AppTheme(val displayName: String, val emoji: String, val isLight: Boolean = false) {
    // Neuf signes distincts : la pastille de thème ne montre déjà que trois
    // couleurs, le signe est ce qui reste pour les différencier au premier
    // coup d'œil dans la liste.
    SAKURA("Sakura", "❀", isLight = true),
    BLACK_WHITE("Noir & Blanc", "◐"),
    NEON("Néon", "✧"),
    NOSTALGIC("Nostalgique", "☾"),
    CRIMSON("Sang d'encre", "✦"),
    TABLOID("Tabloïd", "▤", isLight = true),
    MATRIX("Matrice", "▢"),
    DAYLIGHT("Grand jour", "☀", isLight = true),
    SYSTEM("Automatique", "◑")
}

// ── Sakura — le thème par défaut ─────────────────────────────────────────────
// Beige chaud et blanc, rose de cerisier pour l'accent, or pâle pour l'éclat.
// L'app parle de rumeurs : un fond crème et des pétales désamorcent le sujet
// mieux qu'un noir agressif, et laissent respirer un fil déjà dense.
private val sCream = Color(0xFFFBF7F1)      // fond général, beige très clair
private val sPaper = Color(0xFFFFFFFF)      // cartes
private val sBeige = Color(0xFFF2E9DE)      // surfaces secondaires
private val sBeige2 = Color(0xFFE7DACB)
private val sInk = Color(0xFF2C2723)        // texte, brun profond plutôt que noir
private val sInk2 = Color(0xFF7A6E62)       // texte secondaire
private val sRose = Color(0xFFB85575)       // accent principal, contrasté sur blanc
private val sRoseSoft = Color(0xFFF6DDE4)
private val sGold = Color(0xFFB08432)
private val sSage = Color(0xFF5E8577)
private val sOutline = Color(0xFFE2D5C6)

private val SakuraScheme = lightColorScheme(
    primary = sRose,
    onPrimary = Color.White,
    primaryContainer = sRoseSoft,
    onPrimaryContainer = Color(0xFF5C1B2E),
    secondary = sGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7EAD2),
    onSecondaryContainer = Color(0xFF4A360D),
    tertiary = sSage,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCEAE4),
    onTertiaryContainer = Color(0xFF1E3B33),
    background = sCream,
    onBackground = sInk,
    surface = sPaper,
    onSurface = sInk,
    surfaceVariant = sBeige,
    onSurfaceVariant = sInk2,
    outline = sOutline,
    outlineVariant = sBeige2,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFE3DF),
    onErrorContainer = Color(0xFF5C0F0A),
    inverseSurface = sInk,
    inverseOnSurface = sCream,
    inversePrimary = Color(0xFFFFB1C6),
    scrim = Color(0x59000000)
)

// ── Noir & Blanc ─────────────────────────────────────────────────────────────
private val bwBlack = Color(0xFF050505)
private val bwCard = Color(0xFF0F0F0F)
private val bwSurf = Color(0xFF1A1A1A)
private val bwSurf2 = Color(0xFF242424)
private val bwWhite = Color(0xFFFFFFFF)
private val bwGray1 = Color(0xFFCCCCCC)
private val bwGray2 = Color(0xFF8E8E8E)
private val bwGray3 = Color(0xFF444444)
private val bwGray4 = Color(0xFF2A2A2A)
private val bwRed = Color(0xFFFF3B3B)

private val BlackWhiteScheme = darkColorScheme(
    primary = bwWhite,
    onPrimary = bwBlack,
    primaryContainer = bwSurf,
    onPrimaryContainer = bwWhite,
    secondary = bwGray2,
    onSecondary = bwWhite,
    secondaryContainer = bwSurf2,
    onSecondaryContainer = bwGray1,
    tertiary = bwGray1,
    onTertiary = bwBlack,
    tertiaryContainer = bwSurf,
    onTertiaryContainer = bwWhite,
    background = bwBlack,
    onBackground = bwWhite,
    surface = bwCard,
    onSurface = bwWhite,
    surfaceVariant = bwSurf,
    onSurfaceVariant = bwGray2,
    outline = bwGray4,
    outlineVariant = bwGray3,
    error = bwRed,
    onError = bwWhite,
    errorContainer = Color(0xFF2A0000),
    onErrorContainer = bwRed,
    inverseSurface = bwWhite,
    inverseOnSurface = bwBlack,
    inversePrimary = bwBlack,
    scrim = Color(0x99000000)
)

// ── Néon — violet et cyan sur noir profond ───────────────────────────────────
private val nBg = Color(0xFF02020A)
private val nCard = Color(0xFF07071A)
private val nSurf = Color(0xFF0D0D28)
private val nSurf2 = Color(0xFF141438)
private val nPurple = Color(0xFF9D4EDD)
private val nCyan = Color(0xFF00D4FF)
private val nPink = Color(0xFFFF1E8C)
private val nText = Color(0xFFE8E0FF)
private val nGray = Color(0xFF8B7BA8)
private val nOut = Color(0xFF1E1845)

private val NeonScheme = darkColorScheme(
    primary = nPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A0040),
    onPrimaryContainer = Color(0xFFD9B8FF),
    secondary = nCyan,
    onSecondary = Color(0xFF001A22),
    secondaryContainer = Color(0xFF002233),
    onSecondaryContainer = nCyan,
    tertiary = nPink,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2A0018),
    onTertiaryContainer = nPink,
    background = nBg,
    onBackground = nText,
    surface = nCard,
    onSurface = nText,
    surfaceVariant = nSurf,
    onSurfaceVariant = nGray,
    outline = nOut,
    outlineVariant = Color(0xFF130F2A),
    error = Color(0xFFFF4060),
    onError = Color.White,
    errorContainer = Color(0xFF220010),
    onErrorContainer = Color(0xFFFF4060),
    inverseSurface = nText,
    inverseOnSurface = nBg,
    inversePrimary = nPurple,
    scrim = Color(0xAA02020A)
)

// ── Nostalgique — sépia chaud ────────────────────────────────────────────────
private val xBg = Color(0xFF0A0702)
private val xCard = Color(0xFF140E05)
private val xSurf = Color(0xFF1E1509)
private val xSurf2 = Color(0xFF281C0D)
private val xGold = Color(0xFFD4A847)
private val xAmber = Color(0xFF9C7838)
private val xCream = Color(0xFFE8D5A0)
private val xGray = Color(0xFF9A8365)
private val xOut = Color(0xFF3A2A10)

private val NostalgicScheme = darkColorScheme(
    primary = xGold,
    onPrimary = Color(0xFF0A0702),
    primaryContainer = Color(0xFF2A1E00),
    onPrimaryContainer = xGold,
    secondary = xAmber,
    onSecondary = Color(0xFF0A0702),
    secondaryContainer = Color(0xFF1E1400),
    onSecondaryContainer = xGold,
    tertiary = Color(0xFFB09060),
    onTertiary = Color(0xFF0A0702),
    tertiaryContainer = xSurf2,
    onTertiaryContainer = xCream,
    background = xBg,
    onBackground = xCream,
    surface = xCard,
    onSurface = xCream,
    surfaceVariant = xSurf,
    onSurfaceVariant = xGray,
    outline = xOut,
    outlineVariant = Color(0xFF1E1508),
    error = Color(0xFFD9543F),
    onError = Color.White,
    errorContainer = Color(0xFF220A00),
    onErrorContainer = Color(0xFFD9543F),
    inverseSurface = xCream,
    inverseOnSurface = xBg,
    inversePrimary = xGold,
    scrim = Color(0xAA0A0702)
)

// ── Sang d'encre — pour celles et ceux qui aiment le drama ───────────────────
private val cBg = Color(0xFF0A0304)
private val cCard = Color(0xFF15080A)
private val cSurf = Color(0xFF200C10)
private val cSurf2 = Color(0xFF2C1116)
private val cRed = Color(0xFFFF4757)
private val cRose = Color(0xFFFF8FA3)
private val cText = Color(0xFFFFE8EB)
private val cGray = Color(0xFFA8848A)
private val cOut = Color(0xFF3D1A20)

private val CrimsonScheme = darkColorScheme(
    primary = cRed,
    onPrimary = Color(0xFF1A0004),
    primaryContainer = Color(0xFF3D0810),
    onPrimaryContainer = cRose,
    secondary = cRose,
    onSecondary = Color(0xFF2A0009),
    secondaryContainer = cSurf2,
    onSecondaryContainer = cRose,
    tertiary = Color(0xFFFFB4A2),
    onTertiary = Color(0xFF2A0009),
    tertiaryContainer = cSurf,
    onTertiaryContainer = cText,
    background = cBg,
    onBackground = cText,
    surface = cCard,
    onSurface = cText,
    surfaceVariant = cSurf,
    onSurfaceVariant = cGray,
    outline = cOut,
    outlineVariant = Color(0xFF2A1015),
    error = Color(0xFFFF7043),
    onError = Color(0xFF1A0004),
    errorContainer = Color(0xFF3A1200),
    onErrorContainer = Color(0xFFFFB599),
    inverseSurface = cText,
    inverseOnSurface = cBg,
    inversePrimary = cRed,
    scrim = Color(0xAA0A0304)
)

// ── Tabloïd — papier journal ─────────────────────────────────────────────────
// Le thème le plus juste pour une app de rumeurs : encre noire sur papier
// jauni, rouge de une pour ce qui compte. On lit le campus comme un canard.
private val tPaper = Color(0xFFF6F1E7)
private val tCard = Color(0xFFFFFDF7)
private val tSurf = Color(0xFFEBE4D6)
private val tSurf2 = Color(0xFFDDD4C2)
private val tInk = Color(0xFF14120E)
private val tInk2 = Color(0xFF5C564A)
private val tRed = Color(0xFFC1121F)
private val tOut = Color(0xFFCFC5B0)

private val TabloidScheme = lightColorScheme(
    primary = tRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0E0),
    onPrimaryContainer = Color(0xFF5C0409),
    secondary = tInk,
    onSecondary = tPaper,
    secondaryContainer = tSurf,
    onSecondaryContainer = tInk,
    tertiary = Color(0xFF1D4E89),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9E6F7),
    onTertiaryContainer = Color(0xFF0B2444),
    background = tPaper,
    onBackground = tInk,
    surface = tCard,
    onSurface = tInk,
    surfaceVariant = tSurf,
    onSurfaceVariant = tInk2,
    outline = tOut,
    outlineVariant = tSurf2,
    error = Color(0xFF9B1B1B),
    onError = Color.White,
    errorContainer = Color(0xFFFFDDD8),
    onErrorContainer = Color(0xFF4A0A06),
    inverseSurface = tInk,
    inverseOnSurface = tPaper,
    inversePrimary = Color(0xFFFF8A8A),
    scrim = Color(0x66000000)
)

// ── Matrice — terminal phosphore ─────────────────────────────────────────────
private val mBg = Color(0xFF000A03)
private val mCard = Color(0xFF04150A)
private val mSurf = Color(0xFF071F0E)
private val mSurf2 = Color(0xFF0B2C14)
private val mGreen = Color(0xFF35F58C)
private val mGreenDim = Color(0xFF1E9E5A)
private val mText = Color(0xFFC8FFDE)
private val mGray = Color(0xFF5E8F72)
private val mOut = Color(0xFF13401F)

private val MatrixScheme = darkColorScheme(
    primary = mGreen,
    onPrimary = Color(0xFF001A08),
    primaryContainer = Color(0xFF032B12),
    onPrimaryContainer = mGreen,
    secondary = mGreenDim,
    onSecondary = Color(0xFF001A08),
    secondaryContainer = mSurf2,
    onSecondaryContainer = mGreen,
    tertiary = Color(0xFF7CFFC4),
    onTertiary = Color(0xFF001A08),
    tertiaryContainer = mSurf,
    onTertiaryContainer = mText,
    background = mBg,
    onBackground = mText,
    surface = mCard,
    onSurface = mText,
    surfaceVariant = mSurf,
    onSurfaceVariant = mGray,
    outline = mOut,
    outlineVariant = Color(0xFF0A2A13),
    error = Color(0xFFFF5C5C),
    onError = Color(0xFF1A0000),
    errorContainer = Color(0xFF2A0505),
    onErrorContainer = Color(0xFFFF9E9E),
    inverseSurface = mText,
    inverseOnSurface = mBg,
    inversePrimary = mGreenDim,
    scrim = Color(0xAA000A03)
)

// ── Grand jour — le thème clair ──────────────────────────────────────────────
// Toute l'app était sombre. Une salle de cours en plein soleil, un écran peu
// lumineux, ou simplement une préférence : ce thème rend l'app lisible partout.
private val lBg = Color(0xFFFBFAF9)
private val lCard = Color(0xFFFFFFFF)
private val lSurf = Color(0xFFF1EFED)
private val lSurf2 = Color(0xFFE6E3E0)
private val lInk = Color(0xFF14110F)
private val lInk2 = Color(0xFF6B6560)
private val lAccent = Color(0xFF6D28D9)
private val lOut = Color(0xFFDDD8D3)

private val DaylightScheme = lightColorScheme(
    primary = lAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE4FF),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF0E7490),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F3FA),
    onSecondaryContainer = Color(0xFF073B48),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEEDB),
    onTertiaryContainer = Color(0xFF4A2200),
    background = lBg,
    onBackground = lInk,
    surface = lCard,
    onSurface = lInk,
    surfaceVariant = lSurf,
    onSurfaceVariant = lInk2,
    outline = lOut,
    outlineVariant = lSurf2,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E2),
    onErrorContainer = Color(0xFF5C0F0A),
    inverseSurface = lInk,
    inverseOnSurface = lBg,
    inversePrimary = Color(0xFFCBA6FF),
    scrim = Color(0x66000000)
)

// ═════════════════════════════════════════════════════════════════════════════
//  Couleurs propres à Askip, hors Material
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Ce que Material ne couvre pas : les couleurs qui portent du sens métier
 * (verdict d'une rumeur, Scoop, chaleur, série) et quelques nuances de fond.
 */
data class AskipPalette(
    val confirmed: Color = VerdictConfirmed,
    val debunked: Color = VerdictDebunked,
    val contested: Color = VerdictContested,
    val unknown: Color = VerdictUnknown,
    val scoop: Color = ScoopCyan,
    val scoopBg: Color = ScoopCyanDim,
    val heatLow: Color = HeatLow,
    val heatHigh: Color = HeatHigh,
    val streak: Color = StreakFlame,
    /** Fond des cartes surélevées, légèrement détaché du fond général. */
    val elevated: Color = Color(0xFF141414),
    /** Voile posé sur les images pour garder le texte lisible. */
    val mediaScrim: Color = Color(0x66000000),
    // ── Habillage des bulles ──────────────────────────────────────────────
    // Chaque bouton de l'app est une bulle : un fond, un liseré, une ombre et
    // un reflet en haut. Ces quatre teintes sont déclinées par thème pour que
    // le relief reste lisible aussi bien sur crème que sur noir.
    /** Remplissage d'une bulle secondaire. */
    val bubble: Color = Color(0xFF1C1C1C),
    /** Liseré fin qui détache la bulle du fond. */
    val bubbleBorder: Color = Color(0x33FFFFFF),
    /** Reflet en haut de bulle — c'est lui qui donne l'aspect « verre ». */
    val gloss: Color = Color(0x26FFFFFF),
    /** Teinte de l'ombre portée. */
    val shadow: Color = Color(0xB3000000),
    /** Poussière d'étoiles. */
    val sparkle: Color = Color(0xFFFFE9A8),
    /** Pétales de cerisier. */
    val petal: Color = Color(0xFFF3C6D3),
    val isLight: Boolean = false
)

val LocalAskipPalette = staticCompositionLocalOf { AskipPalette() }

/** Préférences de confort, lues par les composants sans passer par les paramètres. */
val LocalHapticsEnabled = staticCompositionLocalOf { true }
val LocalReduceMotion = staticCompositionLocalOf { false }

private fun paletteFor(theme: AppTheme, dark: Boolean): AskipPalette = when {
    theme == AppTheme.SAKURA -> AskipPalette(
        confirmed = Color(0xFF3F7D5E),
        debunked = Color(0xFFB3384F),
        contested = sGold,
        unknown = Color(0xFF9C9086),
        scoop = Color(0xFF3E6E9E),
        scoopBg = Color(0x1A3E6E9E),
        heatLow = Color(0xFFE08A4B),
        heatHigh = Color(0xFFCF5A56),
        elevated = sPaper,
        mediaScrim = Color(0x3D000000),
        bubble = sPaper,
        // Sur du crème, un liseré blanc ne se voit pas : on prend un beige
        // plus soutenu que le fond pour que la bulle ait un contour net.
        bubbleBorder = Color(0xFFDCCDBB),
        gloss = Color(0x99FFFFFF),
        shadow = Color(0x1F6B4E2E),
        sparkle = Color(0xFFE0B65C),
        petal = Color(0xFFF0BCCB),
        isLight = true
    )
    theme == AppTheme.DAYLIGHT || (theme == AppTheme.SYSTEM && !dark) -> AskipPalette(
        confirmed = Color(0xFF047857),
        debunked = Color(0xFFB91C1C),
        contested = Color(0xFFB45309),
        unknown = Color(0xFF71717A),
        scoop = Color(0xFF0E7490),
        scoopBg = Color(0x1A0E7490),
        heatLow = Color(0xFFEA580C),
        heatHigh = Color(0xFFDC2626),
        elevated = Color(0xFFFFFFFF),
        mediaScrim = Color(0x40000000),
        bubble = Color(0xFFFFFFFF),
        bubbleBorder = Color(0xFFD8D3CD),
        gloss = Color(0x99FFFFFF),
        shadow = Color(0x1A000000),
        sparkle = Color(0xFF9A7BE0),
        petal = Color(0xFFCBB6F5),
        isLight = true
    )
    theme == AppTheme.TABLOID -> AskipPalette(
        confirmed = Color(0xFF1D6B3C),
        debunked = tRed,
        contested = Color(0xFF9A6A00),
        unknown = Color(0xFF7A7264),
        scoop = Color(0xFF1D4E89),
        scoopBg = Color(0x141D4E89),
        heatLow = Color(0xFFC1121F),
        heatHigh = Color(0xFF8B0A12),
        elevated = tCard,
        mediaScrim = Color(0x40000000),
        bubble = tCard,
        bubbleBorder = Color(0xFFC8BCA6),
        gloss = Color(0x80FFFFFF),
        shadow = Color(0x261A1408),
        sparkle = tRed,
        petal = Color(0xFFE8CFC0),
        isLight = true
    )
    theme == AppTheme.MATRIX -> AskipPalette(
        confirmed = mGreen,
        debunked = Color(0xFFFF4D4D),
        contested = Color(0xFFD8FF4D),
        unknown = mGray,
        scoop = Color(0xFF7CFFC4),
        scoopBg = Color(0x267CFFC4),
        heatLow = Color(0xFFB6FF4D),
        heatHigh = mGreen,
        elevated = Color(0xFF05190C),
        bubble = Color(0xFF08210F),
        bubbleBorder = Color(0x5535F58C),
        gloss = Color(0x2635F58C),
        sparkle = mGreen,
        petal = Color(0xFF1E9E5A)
    )
    theme == AppTheme.NEON -> AskipPalette(
        elevated = Color(0xFF0B0B22),
        bubble = Color(0xFF12123A),
        bubbleBorder = Color(0x669D4EDD),
        gloss = Color(0x2600D4FF),
        sparkle = Color(0xFF00D4FF),
        petal = Color(0xFFFF1E8C)
    )
    theme == AppTheme.NOSTALGIC -> AskipPalette(
        confirmed = Color(0xFF7FA05A),
        contested = xGold,
        elevated = Color(0xFF181005),
        bubble = Color(0xFF23180A),
        bubbleBorder = Color(0x66D4A847),
        gloss = Color(0x26D4A847),
        sparkle = xGold,
        petal = Color(0xFFC49A5A)
    )
    theme == AppTheme.CRIMSON -> AskipPalette(
        elevated = Color(0xFF190A0D),
        bubble = Color(0xFF220C11),
        bubbleBorder = Color(0x66FF4757),
        gloss = Color(0x26FF8FA3),
        sparkle = Color(0xFFFF8FA3),
        petal = Color(0xFFFF4757)
    )
    else -> AskipPalette()
}

// ═════════════════════════════════════════════════════════════════════════════
//  Tokens de mise en page
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Une échelle d'espacement unique. Quand tout le monde pioche ici,
 * les écrans respirent pareil sans qu'on ait à y penser.
 */
object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
    val huge = 40.dp
}

/**
 * Rayons généreux, à la manière d'iOS : plus une surface est grande, plus son
 * arrondi l'est aussi. Des angles trop vifs donnent un rendu « formulaire ».
 */
object Radius {
    val xs = 8.dp
    val sm = 12.dp
    val md = 18.dp
    val lg = 24.dp
    val xl = 32.dp
    val pill = 999.dp
}

/** Durées d'animation. Courtes : l'app doit se sentir immédiate. */
object Motion {
    const val FAST = 120
    const val NORMAL = 220
    const val SLOW = 380
    const val CELEBRATE = 900

    /**
     * Le ressort de référence.
     *
     * Tout ce qui bouge à l'écran utilise le même : c'est cette cohérence,
     * plus que la durée, qui donne la sensation « coulée » des apps iPhone.
     * Un rebond franc mais court — assez pour se sentir vivant, jamais assez
     * pour faire attendre.
     */
    fun <T> spring(): SpringSpec<T> = spring(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Réservé aux éléments qu'on veut voir « surgir » : réactions, pastilles. */
    fun <T> pop(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

private val AskipShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.xs),
    small = RoundedCornerShape(Radius.sm),
    medium = RoundedCornerShape(Radius.md),
    large = RoundedCornerShape(Radius.lg),
    extraLarge = RoundedCornerShape(Radius.xl)
)

// ═════════════════════════════════════════════════════════════════════════════
//  Typographie
// ═════════════════════════════════════════════════════════════════════════════

/**
 * La typographie de l'app.
 *
 * Plus menue et plus aérée qu'avant : les titres perdent deux points et une
 * graisse, les libellés gagnent de l'interlettrage. Le gras noir a disparu —
 * il donnait un ton de gros titre à une app qui raconte des potins de couloir.
 *
 * L'interlettrage positif sur les petites tailles n'est pas un détail : c'est
 * lui qui rend une police système lisible et gracieuse à 10 sp, là où un texte
 * serré devient une bouillie grise.
 *
 * Pour poser une vraie police d'écriture : déposer le fichier dans
 * `res/font/`, déclarer un `FontFamily`, et le passer en `fontFamily` ici —
 * un seul endroit à changer, toute l'app suit.
 */
val AskipTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 30.sp,
        letterSpacing = (-0.6).sp, lineHeight = 36.sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 25.sp,
        letterSpacing = (-0.4).sp, lineHeight = 31.sp
    ),
    displaySmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp),
    // Le grand titre qui se replie au défilement.
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    // Le corps du fil : assez grand pour se lire d'une traite, assez aéré pour
    // que dix rumeurs empilées ne forment pas un bloc.
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 12.sp,
        lineHeight = 18.sp, letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 0.5.sp)
)

// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun AskipTheme(
    appTheme: AppTheme = AppTheme.SAKURA,
    haptics: Boolean = true,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    val colors = when (appTheme) {
        AppTheme.SAKURA -> SakuraScheme
        AppTheme.BLACK_WHITE -> BlackWhiteScheme
        AppTheme.NEON -> NeonScheme
        AppTheme.NOSTALGIC -> NostalgicScheme
        AppTheme.CRIMSON -> CrimsonScheme
        AppTheme.TABLOID -> TabloidScheme
        AppTheme.MATRIX -> MatrixScheme
        AppTheme.DAYLIGHT -> DaylightScheme
        AppTheme.SYSTEM -> if (systemDark) BlackWhiteScheme else SakuraScheme
    }

    CompositionLocalProvider(
        LocalAskipPalette provides paletteFor(appTheme, systemDark),
        LocalHapticsEnabled provides haptics,
        LocalReduceMotion provides reduceMotion
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = AskipTypography,
            shapes = AskipShapes,
            content = content
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  Aperçu des thèmes
// ═════════════════════════════════════════════════════════════════════════════

/**
 * Les trois couleurs qui suffisent à reconnaître un thème : le fond, la
 * surface des cartes, l'accent.
 *
 * Le sélecteur des réglages s'en sert pour montrer ce qu'on va obtenir — un
 * nom et un emoji ne disent pas si « Nostalgique » est clair ou sombre.
 */
data class ThemeSwatch(
    val background: Color,
    val surface: Color,
    val accent: Color
)

/** L'aperçu d'un thème. `systemDark` ne sert qu'au thème automatique. */
fun swatchFor(theme: AppTheme, systemDark: Boolean = true): ThemeSwatch {
    val s = when (theme) {
        AppTheme.SAKURA -> SakuraScheme
        AppTheme.BLACK_WHITE -> BlackWhiteScheme
        AppTheme.NEON -> NeonScheme
        AppTheme.NOSTALGIC -> NostalgicScheme
        AppTheme.CRIMSON -> CrimsonScheme
        AppTheme.TABLOID -> TabloidScheme
        AppTheme.MATRIX -> MatrixScheme
        AppTheme.DAYLIGHT -> DaylightScheme
        AppTheme.SYSTEM -> if (systemDark) BlackWhiteScheme else SakuraScheme
    }
    return ThemeSwatch(s.background, s.surface, s.primary)
}
