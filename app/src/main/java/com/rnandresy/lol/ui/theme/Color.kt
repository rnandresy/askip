package com.rnandresy.lol.ui.theme

import androidx.compose.ui.graphics.Color

// ── Historique — conservés pour compatibilité ────────────────────────────────
val AskipPurple = Color(0xFF7C4DFF)
val DarkBg = Color(0xFF0D001F)
val DarkSurf = Color(0xFF1E1E2E)

// ── Admin — doré dans tous les thèmes ────────────────────────────────────────
// Ces teintes ne suivent pas le thème : le statut d'admin doit rester
// reconnaissable au premier coup d'œil, quel que soit l'habillage choisi.
val AdminGold = Color(0xFFFFD700)
val AdminGoldSoft = Color(0xFFFFE566)
val AdminGoldDim = Color(0xFFB8860B)
val AdminGoldBg = Color(0x1AFFD700)

// ── Sémantique de la rumeur ──────────────────────────────────────────────────
// Un verdict doit se lire sans lire : vert = confirmé, rouge = démenti,
// ambre = ça se dispute, gris = on ne sait pas encore.
val VerdictConfirmed = Color(0xFF10B981)
val VerdictDebunked = Color(0xFFEF4444)
val VerdictContested = Color(0xFFF59E0B)
val VerdictUnknown = Color(0xFF8E8E93)

/** Le Scoop : rare, donc traité comme une pierre précieuse. */
val ScoopCyan = Color(0xFF22D3EE)
val ScoopCyanDim = Color(0x2622D3EE)

/** « Ça chauffe » — dégradé braise utilisé sur le badge des rumeurs brûlantes. */
val HeatLow = Color(0xFFFB923C)
val HeatHigh = Color(0xFFEF4444)

/** La série de jours actifs. */
val StreakFlame = Color(0xFFFF6D00)
