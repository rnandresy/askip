package com.rnandresy.lol.ui.theme

/**
 * Le vocabulaire de signes de l'app.
 *
 * Les emojis ont disparu au profit de marques fines et monochromes : elles
 * prennent la couleur du texte, se fondent dans la typographie et ne jurent
 * pas d'un thème à l'autre — un emoji colorié reste colorié sur fond crème
 * comme sur fond noir, et finit toujours par jurer avec l'un des deux.
 *
 * **Tous les signes réunis ici appartiennent au plan multilingue de base et
 * sont couverts par les polices livrées avec Android.** C'est une contrainte,
 * pas une préférence : les hiéroglyphes égyptiens (U+13000 et au-delà) ne sont
 * dans aucune police système, et s'afficheraient en carrés vides sur la
 * quasi-totalité des téléphones. Avant d'en ajouter un ici, vérifier qu'il
 * s'affiche sur un vrai appareil, pas seulement dans l'éditeur.
 *
 * Un même signe peut servir à plusieurs choses : c'est voulu. Un alphabet
 * court se retient, une collection de pictogrammes se subit.
 */
object Glyphs {

    // ── Marques de base ───────────────────────────────────────────────────
    /** L'étoile fine, marque générique de l'app. */
    const val STAR = "⋆"
    /** L'étoile pleine, pour ce qui est acquis ou brûlant. */
    const val SPARK = "✦"
    /** L'étoile creuse, pour ce qui est disponible ou à saisir. */
    const val SPARK_HOLLOW = "✧"
    /** Le losange, pour ce qui est rare et précieux. */
    const val GEM = "⟡"
    /** La fleur, signature du thème Sakura. */
    const val FLOWER = "❀"
    /** La fleur pleine, sa variante marquée. */
    const val FLOWER_FULL = "✿"
    /** Le cœur creux. */
    const val HEART = "♡"
    /** Le croissant, pour la nuit et le sommeil. */
    const val MOON = "☾"
    /** Le soleil fin, pour le jour et le sujet quotidien. */
    const val SUN = "☀"
    /** Le cercle vide : ce qui est masqué, anonyme, indéterminé. */
    const val VEIL = "◌"
    /** Le point médian, séparateur discret. */
    const val DOT = "‧"
    /** Le plus indice, pour ajouter. */
    const val PLUS = "₊"

    // ── Verdicts et états ─────────────────────────────────────────────────
    const val YES = "✓"
    const val NO = "✕"
    const val WARN = "⚠"
    const val LOCK = "⌂"
    const val KEY = "⚿"

    // ── Rôles ─────────────────────────────────────────────────────────────
    /** L'administration du campus. */
    const val CROWN = "✧"
    /** L'école. */
    const val SCHOOL = "⌘"

    // ── Formats de publication ────────────────────────────────────────────
    const val RUMOR = "⋆"
    const val POLL = "◈"
    const val CONFESSION = "◌"
    const val SEALED = "⌂"
    const val CHAIN = "⋯"
    const val TRUTH = "⚖"

    // ── Médias ────────────────────────────────────────────────────────────
    const val PHOTO = "◫"
    const val VIDEO = "▷"
    const val VOICE = "◍"
    const val FILE = "▤"

    // ── Actions ───────────────────────────────────────────────────────────
    const val REPLY = "↩"
    const val COPY = "⧉"
    const val TRASH = "✕"
    const val FLAG = "⚑"
    const val SEND = "→"
    const val NEXT = "›"
    const val BACK = "‹"
    const val SEARCH = "⌕"
    const val BELL = "◠"
    const val PEOPLE = "◍"
    const val CHAT = "⌯"

    // ── Réactions ─────────────────────────────────────────────────────────
    // Six marques, une par intention. Elles restent lisibles à 10 sp, ce que
    // les emojis colorés ne font pas.
    val REACTIONS = listOf("♡", "✦", "◎", "◡", "✧", "✓")

    /** Ce que chaque réaction veut dire, pour les lecteurs d'écran. */
    val REACTION_LABELS = mapOf(
        "♡" to "j'aime",
        "✦" to "ça chauffe",
        "◎" to "surprenant",
        "◡" to "ça me touche",
        "✧" to "brillant",
        "✓" to "d'accord"
    )

    // ── Ornements ─────────────────────────────────────────────────────────
    /** Le filet décoratif, en tête d'écran vide ou de section. */
    const val FLOURISH = "⋆ ˚ ✦ ˚ ⋆"
    /** Sa version courte. */
    const val FLOURISH_SHORT = "˚ ⋆ ˚"
}
