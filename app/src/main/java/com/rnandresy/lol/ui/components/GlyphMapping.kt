package com.rnandresy.lol.ui.components

import com.rnandresy.lol.model.Verdict
import com.rnandresy.lol.ui.theme.AppTheme
import com.rnandresy.lol.utils.FeedSection
import com.rnandresy.lol.utils.FeedTab
import com.rnandresy.lol.utils.QuestKind
import com.rnandresy.lol.utils.RumorEngine

/**
 * Le pont entre les données et les dessins.
 *
 * Les tables de `utils` et `model` gardent leurs clés — des slugs, des enums,
 * parfois un caractère hérité que des milliers de documents Firestore portent
 * déjà. Ce fichier est le seul endroit qui sait quelle figure leur répond.
 *
 * La règle est celle de [glyphForReaction] : la couche des données n'a pas à
 * connaître les dessins. On peut redessiner toute l'app sans ouvrir un seul
 * fichier de `utils`, et ajouter un salon sans ouvrir un seul fichier de `ui`.
 */

// ─────────────────────────────────────────────────────────────────────────────
//  Les caractères hérités
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Traduit un signe stocké en base — le signe d'un groupe, celui d'une story —
 * en figure dessinée.
 *
 * Ces chaînes-là ne peuvent pas être remplacées par un enum : elles sont déjà
 * écrites dans les documents des utilisateurs. On les lit donc telles quelles,
 * et c'est ici qu'elles retrouvent un dessin. Tout signe inconnu retombe sur
 * l'éclat, qui ne veut rien dire de précis et ne ment donc sur rien.
 */
fun glyphForSymbol(symbol: String): GlyphKind {
    val mark = symbol.trim()
    // Les marques posées depuis la refonte sont déjà des noms de figures.
    GlyphKind.entries.firstOrNull { it.name.equals(mark, ignoreCase = true) }
        ?.let { return it }
    return when (mark) {
        "✦", "✩", "✰", "★", "⋆" -> GlyphKind.STAR
        "✧", "₊", "⁺", "✨" -> GlyphKind.SPARKLE
        "❀", "✿", "❁", "✽" -> GlyphKind.FLOWER
        "♡", "♥" -> GlyphKind.HEART
        "☾", "☽", "◐" -> GlyphKind.MOON
        "☀", "☼" -> GlyphKind.SUN
        "⟡", "◈", "◇", "◆" -> GlyphKind.GEM
        "◌", "◍", "◎", "○", "◑" -> GlyphKind.VEIL
        "⌯", "⌾" -> GlyphKind.BUBBLE
        "▤", "▢", "▣", "⌂" -> GlyphKind.DOC
        "⚖" -> GlyphKind.SCALE
        "⌕" -> GlyphKind.SEARCH
        "⚔" -> GlyphKind.DRAMA
        "✓", "✔" -> GlyphKind.CHECK
        "✕", "✗", "✖" -> GlyphKind.CROSS
        "✎", "✐" -> GlyphKind.PENCIL
        "⚽" -> GlyphKind.BALL
        "☠" -> GlyphKind.SKULL
        "⚿", "⚷" -> GlyphKind.KEY
        "⋯", "⋮" -> GlyphKind.LINK
        "⌘", "⌬" -> GlyphKind.BOOK
        "#" -> GlyphKind.TICKET
        else -> GlyphKind.SPARKLE
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Les tables de l'app
// ─────────────────────────────────────────────────────────────────────────────

/** Les dix salons. */
fun glyphForTag(slug: String): GlyphKind = when (slug) {
    "amphi" -> GlyphKind.PEOPLE
    "couple" -> GlyphKind.HEART
    "prof" -> GlyphKind.BOOK
    "exam" -> GlyphKind.PENCIL
    "soiree" -> GlyphKind.SPARKLE
    "sport" -> GlyphKind.BALL
    "drama" -> GlyphKind.DRAMA
    "mystere" -> GlyphKind.VEIL
    "bon_plan" -> GlyphKind.GEM
    "wtf" -> GlyphKind.SKULL
    else -> GlyphKind.TICKET
}

/** Les trois actualités. */
fun glyphForSection(section: FeedSection): GlyphKind = when (section) {
    FeedSection.MAIN -> GlyphKind.DOC
    FeedSection.VOICE -> GlyphKind.WAVE
    FeedSection.TRUTH -> GlyphKind.SCALE
}

/** Les onglets de tri de l'actualité principale. */
fun glyphForFeedTab(tab: FeedTab): GlyphKind = when (tab) {
    FeedTab.HOT -> GlyphKind.FLAME
    FeedTab.FRESH -> GlyphKind.SPARKLE
    FeedTab.CONFESSIONS -> GlyphKind.VEIL
    FeedTab.LEGENDS -> GlyphKind.CROWN
}

/** La météo du campus, du calme plat à la tempête. */
fun glyphForWeather(weather: RumorEngine.Weather): GlyphKind = when (weather) {
    RumorEngine.Weather.CALM -> GlyphKind.MOON
    RumorEngine.Weather.CLEAR -> GlyphKind.SUN
    RumorEngine.Weather.BREEZY -> GlyphKind.WAVE
    RumorEngine.Weather.STORMY -> GlyphKind.FLAME
    RumorEngine.Weather.CHAOS -> GlyphKind.SKULL
}

/** Le verdict rendu par le campus sur une rumeur. */
fun glyphForVerdict(verdict: Verdict): GlyphKind = when (verdict) {
    Verdict.INVESTIGATING -> GlyphKind.SEARCH
    Verdict.CONTESTED -> GlyphKind.DRAMA
    Verdict.CONFIRMED -> GlyphKind.CHECK
    Verdict.DEBUNKED -> GlyphKind.CROSS
}

/** Les neuf thèmes. */
fun glyphForTheme(theme: AppTheme): GlyphKind = when (theme) {
    AppTheme.SAKURA -> GlyphKind.FLOWER
    AppTheme.BLACK_WHITE -> GlyphKind.VEIL
    AppTheme.NEON -> GlyphKind.SPARKLE
    AppTheme.NOSTALGIC -> GlyphKind.MOON
    AppTheme.CRIMSON -> GlyphKind.GEM
    AppTheme.TABLOID -> GlyphKind.DOC
    AppTheme.MATRIX -> GlyphKind.CHART
    AppTheme.DAYLIGHT -> GlyphKind.SUN
    AppTheme.SYSTEM -> GlyphKind.BUBBLE
}

/** Les missions du jour, par ce qu'elles demandent de faire. */
fun glyphForQuest(kind: QuestKind): GlyphKind = when (kind) {
    QuestKind.REACT -> GlyphKind.HEART
    QuestKind.COMMENT -> GlyphKind.BUBBLE
    QuestKind.POST -> GlyphKind.PENCIL
    QuestKind.VERDICT -> GlyphKind.SCALE
    QuestKind.SCOOP -> GlyphKind.GEM
    QuestKind.STORY -> GlyphKind.PHOTO
    QuestKind.CONFESS -> GlyphKind.VEIL
    QuestKind.OPEN_TAG -> GlyphKind.TICKET
    QuestKind.BET -> GlyphKind.CHART
    QuestKind.CHAIN -> GlyphKind.LINK
    QuestKind.KEY -> GlyphKind.KEY
}

/** Les formats de publication. */
fun glyphForPostFormat(type: String): GlyphKind = when (type) {
    "normal" -> GlyphKind.STAR
    "poll" -> GlyphKind.CHART
    "confession" -> GlyphKind.VEIL
    "sealed" -> GlyphKind.LOCK
    "chain" -> GlyphKind.LINK
    "truth" -> GlyphKind.SCALE
    else -> GlyphKind.STAR
}

/** Le jeton de pari : une gemme, la seule monnaie du campus. */
val BetTokenGlyph: GlyphKind = GlyphKind.GEM

/** Le côté d'un pari : crédible ou bidon. */
fun glyphForBetSide(onCredible: Boolean): GlyphKind =
    if (onCredible) GlyphKind.CHECK else GlyphKind.CROSS

/**
 * Les trophées.
 *
 * Groupés par famille : poster, commenter, se confier, sonder, raconter,
 * parler aux gens, tenir la série. Deux trophées d'une même famille partagent
 * la figure — c'est la rareté, pas le dessin, qui les distingue.
 */
fun glyphForAchievement(id: String): GlyphKind = when (id) {
    "first_post", "ten_posts", "twenty_five_p", "fifty_posts" -> GlyphKind.PENCIL
    "first_comment", "commentator", "deep_comment" -> GlyphKind.BUBBLE
    "confessor", "dark_confessor" -> GlyphKind.VEIL
    "poll_creator", "poll_master" -> GlyphKind.CHART
    "first_story", "storyteller" -> GlyphKind.PHOTO
    "social", "social_plus" -> GlyphKind.PEOPLE
    "streak_3", "streak_7", "streak_30" -> GlyphKind.FLAME
    "eni_pride" -> GlyphKind.BOOK
    "badge_maker" -> GlyphKind.GEM
    "mood_master" -> GlyphKind.MOON
    else -> GlyphKind.STAR
}

/** Le cadre d'avatar choisi dans « Modifier le profil ». */
fun glyphForAvatarFrame(frame: String): GlyphKind? = when (frame) {
    "fire" -> GlyphKind.FLAME
    "star" -> GlyphKind.STAR
    "rainbow" -> GlyphKind.FLOWER
    "gold" -> GlyphKind.CROWN
    else -> null
}

/** Ce qui a déclenché une notification. */
fun glyphForNotification(type: String, fromAdmin: Boolean): GlyphKind = when (type) {
    "new_post_admin", "new_post", "mention_everyone" -> GlyphKind.STAR
    "mention", "message" -> if (fromAdmin) GlyphKind.CROWN else GlyphKind.BUBBLE
    else -> GlyphKind.BELL
}

/** Les trois premières marches d'un classement. */
fun glyphForRank(rank: Int): GlyphKind = when (rank) {
    1 -> GlyphKind.CROWN
    2 -> GlyphKind.STAR
    3 -> GlyphKind.SPARKLE
    else -> GlyphKind.SPARKLE
}
