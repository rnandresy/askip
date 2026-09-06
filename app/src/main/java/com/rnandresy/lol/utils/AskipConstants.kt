package com.rnandresy.lol.utils

// ── Collections Firestore ─────────────────────────────────────────────────────
const val COL_USERS         = "users"
const val COL_PROFILES      = "profiles"
const val COL_POSTS         = "posts"
const val COL_COMMENTS      = "comments"
const val COL_STORIES       = "stories"
const val COL_CONVERSATIONS = "conversations"
const val COL_MESSAGES      = "messages"
const val COL_GROUPS        = "groups"
const val COL_NOTIFICATIONS = "notifications"
const val COL_REPORTS       = "reports"
const val COL_BADGES        = "badges"
const val COL_ACHIEVEMENTS  = "achievements"
const val COL_SEALED        = "sealed"
const val COL_BETS          = "bets"
const val COL_LINKS         = "links"
const val COL_REPLIES       = "replies"

/** Document unique de la sous-collection scellée d'une rumeur. */
const val DOC_SEALED        = "payload"

// ── Pagination ────────────────────────────────────────────────────────────────
/** Taille d'une page de rumeurs plus anciennes, au-delà de la fenêtre temps réel. */
const val PAGE_SIZE_FEED     = 30L

/**
 * Fenêtre analysée côté client pour le classement « ça chauffe » et les
 * tendances. Au-delà, le fil continue via [PAGE_SIZE_FEED] : rien n'est
 * inatteignable, seul le tri par chaleur s'arrête à cette fenêtre.
 */
const val HOT_WINDOW_SIZE    = 120L

/**
 * Profils gardés en mémoire.
 *
 * Ils servent partout : résolution des pseudos et photos sur chaque post,
 * autocomplétion des mentions, liste des membres. Un plafond trop bas ferait
 * disparaître des gens de l'annuaire et casserait l'affichage de leurs posts,
 * donc la limite est haute — elle protège d'un emballement, pas de l'usage
 * normal d'un campus.
 */
const val MAX_PROFILES_CACHED = 500L

/** Messages chargés à l'ouverture d'une conversation. */
const val MESSAGES_WINDOW     = 400L

/** Commentaires chargés sous une rumeur. */
const val COMMENTS_WINDOW     = 300L

// ── Limites contenu ───────────────────────────────────────────────────────────
const val MAX_POST_LENGTH       = 1000
const val MAX_COMMENT_LENGTH    = 500
const val MAX_CONFESSION_LENGTH = 300
const val MAX_STORY_LENGTH      = 200
const val MAX_MESSAGE_LENGTH    = 2000
const val MAX_BIO_LENGTH        = 150
const val MAX_USERNAME_LENGTH   = 20
const val MIN_USERNAME_LENGTH   = 3
const val MAX_TAGS_PER_POST     = 3

// ── Médias ────────────────────────────────────────────────────────────────────
const val MAX_IMAGE_DIMENSION   = 1440      // px — côté le plus long
const val IMAGE_QUALITY         = 82        // qualité JPEG
const val MAX_UPLOAD_SIZE_MB    = 25
const val MAX_AUDIO_SECONDS     = 180       // 3 min
const val STORY_DURATION_MS     = 24L * 60 * 60 * 1000  // 24 h

// ── Rate limiting client ──────────────────────────────────────────────────────
const val MIN_MS_BETWEEN_POSTS    = 20_000L   // 20 s
const val MIN_MS_BETWEEN_COMMENTS = 5_000L
const val MIN_MS_BETWEEN_MESSAGES = 500L
const val MAX_POSTS_PER_HOUR      = 15

// ── Réactions ─────────────────────────────────────────────────────────────────
// Des mots, pas des signes. La clé sert à retrouver le champ correspondant en
// base : un signe la rendrait dépendante de son encodage, illisible dans la
// console Firebase, et interdirait de changer le dessin sans réécrire toutes
// les réactions déjà posées. Le dessin, lui, se choisit côté interface.
val REACTIONS = listOf("love", "fire", "lol", "shock", "eyes")

/** Libellés lus par les lecteurs d'écran — même ordre que [REACTIONS]. */
val REACTION_LABELS = mapOf(
    "love" to "J'aime",
    "fire" to "Ça chauffe",
    "lol" to "Drôle",
    "shock" to "Choquant",
    "eyes" to "Intrigant"
)

// ── Le Scoop : une pépite par jour, par personne ───────────────────────────────
// Rareté volontaire. Un Scoop pèse lourd dans le classement « ça chauffe »,
// donc le donner est un vrai choix — c'est ce qui lui donne de la valeur.
const val SCOOPS_PER_DAY       = 1
const val SCOOP_HOT_WEIGHT     = 12.0

// ── Rumeur-mètre : crédible vs bidon ──────────────────────────────────────────
/** En dessous de ce total de votes, le verdict reste « en enquête ». */
const val VERDICT_MIN_VOTES    = 8
const val VERDICT_CONFIRMED_AT = 0.72f   // ≥ 72 % de « crédible » → confirmé
const val VERDICT_DEBUNKED_AT  = 0.28f   // ≤ 28 % de « crédible » → démenti

// ── Clout : la réputation d'informateur ───────────────────────────────────────
const val CLOUT_PER_CREDIBLE   = 3L      // quelqu'un croit ta rumeur
const val CLOUT_PER_FAKE       = -2L     // quelqu'un la démonte
const val CLOUT_PER_SCOOP      = 8L      // on t'accorde un Scoop
const val CLOUT_CONFIRMED_POST = 40L     // ta rumeur passe « CONFIRMÉE »
const val CLOUT_DEBUNKED_POST  = -25L    // ta rumeur passe « DÉMENTIE »

// ── Le Pari : miser sur un verdict avant qu'il tombe ──────────────────────────
// Trois jetons par jour, pas plus. Le jeton n'est pas la récompense — c'est le
// droit de parier. Le gain, lui, est du clout. Résultat : on ne mise pas au
// hasard, et un nouveau venu peut jouer dès le premier jour sans réputation.
const val BETS_PER_DAY      = 3

/** Mises possibles — plus on mise, plus le gain et la perte suivent. */
val BET_STAKES = listOf(1, 2, 3)

/** Gain de base pour une mise d'un jeton, avant cote et bonus de précocité. */
const val BET_BASE_REWARD   = 8L

/** Perte fixe par jeton misé. Ça pique sans jamais ruiner. */
const val BET_PENALTY       = 8L

/** Cote maximale — parier contre tout le monde rapporte gros, pas infini. */
const val BET_MAX_ODDS      = 5.0f

/** En dessous de ce nombre de votes, le pari compte comme précoce. */
const val BET_EARLY_VOTES   = 30

// ── La Capsule scellée : une rumeur qu'on ne peut pas encore lire ─────────────
// Le verrou est appliqué par les règles Firestore (comparaison à `request.time`),
// pas par l'affichage : le contenu est réellement illisible avant l'heure.

/** Durées proposées à la publication, en heures. */
val SEAL_DURATIONS_HOURS = listOf(1, 6, 24, 72, 168)

/** Nombre de clés qui ouvre une capsule avant l'heure prévue. */
const val SEAL_KEYS_TO_OPEN = 10

// ── Le Téléphone arabe : une rumeur à plusieurs mains ────────────────────────
const val CHAIN_MAX_LINKS   = 7
const val CHAIN_MAX_LENGTH  = 140   // caractères par maillon

// ── Types de rumeur additionnels ──────────────────────────────────────────────
const val POST_TYPE_SEALED  = "sealed"
const val POST_TYPE_CHAIN   = "chain"
const val POST_TYPE_VOICE   = "voice"
const val POST_TYPE_TRUTH   = "truth"

// ── XP et niveaux ─────────────────────────────────────────────────────────────
const val XP_BET         = 6L
const val XP_CHAIN_LINK  = 12L
const val XP_POST        = 20L
const val XP_COMMENT     = 8L
const val XP_REACTION    = 2L
const val XP_VERDICT     = 4L
const val XP_STORY       = 10L
const val XP_QUEST       = 60L
const val XP_DAILY_LOGIN = 15L

/**
 * Palier d'XP du niveau [level] (1-indexé). Courbe volontairement douce au début
 * puis de plus en plus lente : on monte vite les premiers jours, on s'accroche ensuite.
 */
fun xpForLevel(level: Int): Long {
    if (level <= 1) return 0L
    val l = (level - 1).toLong()
    return 100L * l * l / 2 + 150L * l
}

fun levelForXp(xp: Long): Int {
    var lvl = 1
    while (lvl < 99 && xp >= xpForLevel(lvl + 1)) lvl++
    return lvl
}

/** Titre affiché à côté du niveau. */
fun levelTitle(level: Int): String = when {
    level >= 40 -> "Légende du campus"
    level >= 30 -> "Oracle"
    level >= 22 -> "Grand reporter"
    level >= 15 -> "Informateur"
    level >= 10 -> "Fouineur"
    level >= 6  -> "Curieux"
    level >= 3  -> "Bavard"
    else        -> "Nouveau"
}

// ── Stories ───────────────────────────────────────────────────────────────────
val STORY_COLORS = listOf(
    "#7C3AED", "#DC2626", "#059669", "#D97706",
    "#2563EB", "#DB2777", "#0891B2", "#4F46E5"
)

// Seize marques, toutes différentes : c'est un sélecteur, deux figures
// identiques y seraient impossibles à distinguer l'une de l'autre.
//
// Ce sont des clés, pas des signes : le dessin correspondant se choisit
// côté interface. Les groupes et les stories déjà en base portent encore
// l'ancien caractère — l'interface sait lire les deux.
val STORY_MARKS = listOf(
    "star", "sparkle", "flower", "heart",
    "moon", "sun", "gem", "veil",
    "bubble", "crown", "flame", "wave",
    "ball", "drama", "key", "skull"
)

// ── Types de post ─────────────────────────────────────────────────────────────
const val POST_TYPE_NORMAL     = "normal"
const val POST_TYPE_POLL       = "poll"
const val POST_TYPE_CONFESSION = "confession"

// ── Les trois actualités ──────────────────────────────────────────────────────
/**
 * Trois façons d'habiter le même campus.
 *
 * Ce ne sont pas trois tris du même fil : chacune impose sa propre règle du jeu.
 * On n'écrit pas dans la Vocale, on n'y parle que ; et on ne publie dans la
 * Vérité qu'après avoir juré.
 */
enum class FeedSection(val label: String, val tagline: String) {
    MAIN("Actualité", "Tout ce qui se raconte"),
    VOICE("Vocal", "Ici, on ne lit pas. On écoute."),
    TRUTH("Vérité", "Uniquement des faits, sous serment.")
}

// ── Onglets de tri, dans l'actualité principale ───────────────────────────────
enum class FeedTab(val label: String) {
    HOT("Ça chauffe"),
    FRESH("Frais"),
    CONFESSIONS("Confessions"),
    LEGENDS("Légendes")
}

// ── L'actualité vocale ────────────────────────────────────────────────────────
// Pas de texte, du tout : ni post, ni commentaire. Une voix porte l'hésitation,
// l'accent, le rire retenu — tout ce qu'un message écrit efface.
const val MAX_VOICE_POST_SECONDS    = 120
const val MAX_VOICE_COMMENT_SECONDS = 60

// ── La Page de Vérité ─────────────────────────────────────────────────────────
// Une page solennelle où l'on ne publie que sous serment. Elle tient elle-même
// le compte des serments démentis par le campus : c'est ce compteur qui fait
// tout le sel de l'endroit.
const val OATH_TEXT =
    "Je jure sur l'honneur que ce que je m'apprête à publier est rigoureusement exact."

const val PERJURY_STAMP = "PARJURE"

// ── Tags de rumeur ────────────────────────────────────────────────────────────
// Un tag range la rumeur dans un rayon. Le fil par tag, c'est le « salon » du sujet.
data class TagDef(val slug: String, val label: String)

val RUMOR_TAGS = listOf(
    TagDef("amphi",   "Amphi"),
    TagDef("couple",  "Couples"),
    TagDef("prof",    "Profs"),
    TagDef("exam",    "Exams"),
    TagDef("soiree",  "Soirées"),
    TagDef("sport",   "Sport"),
    TagDef("drama",   "Drama"),
    TagDef("mystere", "Mystère"),
    TagDef("bon_plan", "Bons plans"),
    TagDef("wtf",     "WTF"),
)

fun tagDef(slug: String): TagDef? = RUMOR_TAGS.firstOrNull { it.slug == slug }

// ── Motifs de signalement ─────────────────────────────────────────────────────
val REPORT_REASONS = listOf(
    "Contenu offensant",
    "Harcèlement",
    "Spam",
    "Fausse information",
    "Contenu inapproprié",
    "Usurpation d'identité",
    "Autre"
)
