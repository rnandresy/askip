package com.rnandresy.lol.utils

import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.Verdict
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Tout ce qui fait qu'une rumeur vit : son masque anonyme, ses tags,
 * ce qui monte en ce moment, et le tri du fil.
 *
 * Volontairement sans état et sans réseau : tout se calcule à partir
 * des posts déjà en mémoire, donc c'est instantané et ça ne coûte
 * aucune lecture Firestore supplémentaire.
 */
object RumorEngine {

    // ── Masques anonymes ──────────────────────────────────────────────────────
    // Une confession sans identité, c'est du bruit. Avec un masque stable,
    // « Le Corbeau #7B2 » devient un personnage qu'on suit dans le fil de
    // commentaires — sans jamais révéler qui se cache derrière.

    private val MASKS = listOf(
        "Le Corbeau", "L'Ombre", "Le Fantôme", "Le Hibou", "Le Renard",
        "Le Masque", "L'Anonyme", "Le Témoin", "La Taupe", "Le Chat noir",
        "Le Murmure", "L'Inconnu", "Le Veilleur", "Le Rôdeur", "Le Spectre",
        "La Sentinelle", "Le Guetteur", "Le Passant", "L'Écho", "Le Souffleur"
    )

    /**
     * Pseudonyme stable pour un utilisateur dans le contexte d'un post donné.
     * Même personne + même post = même masque, toujours.
     * Même personne + autre post = autre masque : impossible de recouper.
     */
    fun anonAlias(userId: String, postId: String): String {
        if (userId.isBlank()) return "Quelqu'un"
        val h = stableHash("$userId::$postId")
        val mask = MASKS[(h % MASKS.size).toInt()]
        val tag = "%03X".format((h / 7) % 4096)
        return "$mask #$tag"
    }

    /** Version courte, pour les listes serrées. */
    fun anonAliasShort(userId: String, postId: String): String {
        if (userId.isBlank()) return "Quelqu'un"
        val h = stableHash("$userId::$postId")
        return "${MASKS[(h % MASKS.size).toInt()]} #${"%03X".format((h / 7) % 4096)}"
    }

    /** Hash déterministe et positif — [String.hashCode] suffit et ne change pas. */
    private fun stableHash(s: String): Long = abs(s.hashCode().toLong()).coerceAtLeast(1L)

    // ── Tags ──────────────────────────────────────────────────────────────────

    private val TAG_REGEX = Regex("#([\\p{L}0-9_]{2,20})")

    /** Récupère les #tags écrits dans le texte, en minuscules et sans doublon. */
    fun extractTags(content: String): List<String> =
        TAG_REGEX.findAll(content)
            .map { it.groupValues[1].lowercase() }
            .distinct()
            .take(MAX_TAGS_PER_POST)
            .toList()

    /** Fusionne les tags choisis dans l'éditeur et ceux écrits dans le texte. */
    fun mergeTags(selected: List<String>, content: String): List<String> =
        (selected + extractTags(content))
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_TAGS_PER_POST)

    data class TrendingTag(val slug: String, val count: Int, val heat: Double)

    /**
     * Ce dont le campus parle en ce moment : on pondère chaque tag par la
     * chaleur des posts qui le portent, pas seulement par leur nombre.
     * Un tag sur trois rumeurs brûlantes passe devant un tag sur dix posts morts.
     */
    fun trendingTags(posts: List<Post>, limit: Int = 8): List<TrendingTag> {
        val now = System.currentTimeMillis()
        val agg = HashMap<String, Pair<Int, Double>>()
        posts.forEach { post ->
            val heat = post.hotScore(now)
            post.tags.forEach { tag ->
                val (c, h) = agg[tag] ?: (0 to 0.0)
                agg[tag] = (c + 1) to (h + heat)
            }
        }
        return agg.entries
            .map { TrendingTag(it.key, it.value.first, it.value.second) }
            .sortedWith(compareByDescending<TrendingTag> { it.heat }.thenByDescending { it.count })
            .take(limit)
    }

    // ── Tri du fil ────────────────────────────────────────────────────────────

    /** Les posts épinglés restent en tête, le reste suit l'ordre demandé. */
    private fun pinnedFirst(posts: List<Post>): Pair<List<Post>, List<Post>> =
        posts.partition { it.isPinned }

    fun sortHot(posts: List<Post>, now: Long = System.currentTimeMillis()): List<Post> {
        val (pinned, rest) = pinnedFirst(posts.filterNot { it.isExpired(now) })
        return pinned.sortedByDescending { it.timestamp } +
            rest.sortedByDescending { it.hotScore(now) }
    }

    fun sortFresh(posts: List<Post>, now: Long = System.currentTimeMillis()): List<Post> {
        val (pinned, rest) = pinnedFirst(posts.filterNot { it.isExpired(now) })
        return pinned.sortedByDescending { it.timestamp } +
            rest.sortedByDescending { it.timestamp }
    }

    fun sortLegends(posts: List<Post>): List<Post> =
        posts.sortedByDescending { it.legendScore() }

    /**
     * Applique l'onglet choisi. Les confessions ont leur propre onglet, donc
     * on les sort des fils « ça chauffe » et « frais » : sinon l'anonymat
     * noierait les rumeurs signées.
     */
    fun applyTab(
        posts: List<Post>,
        tab: FeedTab,
        now: Long = System.currentTimeMillis()
    ): List<Post> = when (tab) {
        FeedTab.HOT -> sortHot(posts.filterNot { it.isConfession() }, now)
        FeedTab.FRESH -> sortFresh(posts.filterNot { it.isConfession() }, now)
        FeedTab.CONFESSIONS -> sortFresh(posts.filter { it.isConfession() }, now)
        FeedTab.LEGENDS -> sortLegends(posts.filterNot { it.isExpired(now) })
    }

    // ── Le Scoop du jour ──────────────────────────────────────────────────────

    fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun yesterday(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    /** Reste-t-il un Scoop à donner aujourd'hui ? */
    fun canScoop(lastScoopDate: String): Boolean = lastScoopDate != today()

    /** Millisecondes avant la recharge du Scoop (minuit). */
    fun msUntilScoopReset(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (cal.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    // ── Les trois actualités ──────────────────────────────────────────────────

    /**
     * Découpe le fil en trois espaces qui ne se mélangent jamais.
     *
     * Une rumeur vocale ne remonte pas dans l'actualité principale, et un
     * serment reste sur la Page de Vérité : sans cette séparation stricte, les
     * trois pages auraient exactement le même contenu et aucune identité.
     */
    fun applySection(
        posts: List<Post>,
        section: FeedSection,
        tab: FeedTab,
        now: Long = System.currentTimeMillis()
    ): List<Post> = when (section) {
        FeedSection.MAIN ->
            applyTab(posts.filterNot { it.isVoice() || it.isTruth() }, tab, now)

        // Le vocal se consomme dans l'ordre d'arrivée : on écoute une
        // conversation, on ne trie pas un classement.
        FeedSection.VOICE ->
            sortFresh(posts.filter { it.isVoice() }, now)

        // Les serments les plus récents d'abord — la page se lit comme un
        // registre, pas comme un palmarès.
        FeedSection.TRUTH ->
            sortFresh(posts.filter { it.isTruth() }, now)
    }

    // ── Le registre des serments ──────────────────────────────────────────────

    /**
     * Le compteur de la Page de Vérité.
     *
     * La page se présente comme solennelle et n'accepte que des publications
     * faites sous serment. Elle tient donc, en toute rigueur, le compte des
     * serments que le campus a démentis. C'est l'app elle-même qui fait la
     * blague : elle ne commente pas, elle mesure.
     */
    data class TruthLedger(
        val sworn: Int,
        val judged: Int,
        val perjuries: Int,
        val upheld: Int
    ) {
        /** Part de serments démentis parmi ceux que le campus a tranchés. */
        val perjuryRate: Float
            get() = if (judged == 0) 0f else perjuries.toFloat() / judged

        /**
         * Le verdict de la page sur elle-même. Ton de greffier : on énonce,
         * on ne se moque pas — c'est ce qui rend la chose drôle.
         */
        val statement: String
            get() = when {
                sworn == 0 -> "Aucun serment prêté. La page est vierge."
                judged == 0 -> "Serments enregistrés. Le campus n'a pas encore tranché."
                perjuryRate >= 0.85f -> "Plus personne ne fait semblant."
                perjuryRate >= 0.60f -> "La vérité est en difficulté."
                perjuryRate >= 0.30f -> "Un serment sur deux tient. C'est déjà ça."
                perjuryRate > 0f -> "Étonnamment honnête. Ça ne durera pas."
                else -> "Aucun parjure constaté. C'est suspect."
            }
    }

    fun truthLedger(posts: List<Post>): TruthLedger {
        val sworn = posts.filter { it.isTruth() }
        val judged = sworn.filter { it.verdict() != Verdict.INVESTIGATING }
        return TruthLedger(
            sworn = sworn.size,
            judged = judged.size,
            perjuries = judged.count { it.verdict() == Verdict.DEBUNKED },
            upheld = judged.count { it.verdict() == Verdict.CONFIRMED }
        )
    }

    // ── Le Pari ───────────────────────────────────────────────────────────────

    /**
     * Cote figée au moment du pari.
     *
     * Elle récompense deux choses : aller contre la foule, et arriver tôt.
     * Suivre l'avis général sur une rumeur déjà tranchée ne rapporte presque
     * rien — c'est voulu, sinon parier ne demanderait aucun jugement.
     *
     * @param ratioForSide part des votes déjà du côté choisi, entre 0 et 1
     * @param votes total de votes au moment du pari
     */
    fun betOdds(ratioForSide: Float, votes: Int): Float {
        // Un côté à 20 % rapporte 5×, un côté à 80 % rapporte 1,25×.
        val contrarian = 1f / ratioForSide.coerceIn(0.2f, 1f)
        // Parier avant que le campus se prononce vaut jusqu'à 1,5×.
        val earliness = 1f + (1f - (votes.coerceAtMost(BET_EARLY_VOTES).toFloat() /
            BET_EARLY_VOTES)) * 0.5f
        return (contrarian * earliness).coerceIn(1f, BET_MAX_ODDS)
    }

    /** Part des votes du côté [onCredible] à cet instant. */
    fun sideRatio(credibleCount: Int, fakeCount: Int, onCredible: Boolean): Float {
        val total = credibleCount + fakeCount
        if (total == 0) return 0.5f
        return if (onCredible) credibleCount.toFloat() / total
        else fakeCount.toFloat() / total
    }

    // ── La Météo du campus ────────────────────────────────────────────────────

    enum class Weather(val label: String, val blurb: String) {
        CALM("Calme plat", "Personne n'a rien à dire aujourd'hui."),
        CLEAR("Dégagé", "Ça papote tranquillement."),
        BREEZY("Ça bruisse", "Quelques rumeurs circulent."),
        STORMY("Orageux", "Ça chauffe sérieusement."),
        CHAOS("Tempête", "Le campus est en feu.")
    }

    data class CampusWeather(
        val weather: Weather,
        val postsToday: Int,
        val verdictsToday: Int,
        /** Part de rumeurs contestées ou démenties — l'indice de friction. */
        val dramaRatio: Float
    )

    /**
     * L'ambiance du campus, calculée sur les dernières 24 h.
     *
     * Entièrement local, à partir des rumeurs déjà en mémoire : aucune lecture
     * supplémentaire, et l'indicateur se met à jour tout seul à chaque réaction
     * qui arrive.
     */
    fun campusWeather(
        posts: List<Post>,
        now: Long = System.currentTimeMillis()
    ): CampusWeather {
        val dayAgo = now - 86_400_000L
        val recent = posts.filter { it.timestamp >= dayAgo }
        val verdicts = recent.sumOf { it.verdictVotes() }
        val engagement = recent.sumOf { it.totalReactions() + it.commentCount }

        val drama = if (recent.isEmpty()) 0f else {
            recent.count {
                val v = it.verdict()
                v == Verdict.CONTESTED || v == Verdict.DEBUNKED
            }.toFloat() / recent.size
        }

        // Un score composite : le volume compte, mais la friction compte double.
        val heat = recent.size * 2 + engagement + verdicts + (drama * 40).toInt()

        val weather = when {
            recent.isEmpty() -> Weather.CALM
            heat >= 220 -> Weather.CHAOS
            heat >= 110 -> Weather.STORMY
            heat >= 40 -> Weather.BREEZY
            else -> Weather.CLEAR
        }
        return CampusWeather(weather, recent.size, verdicts, drama)
    }

    // ── Le droit de réponse ───────────────────────────────────────────────────

    /**
     * Cette rumeur cite-t-elle [username] nommément ?
     * On compare sans casse et en ignorant la ponctuation collée à la mention.
     */
    fun mentions(content: String, username: String): Boolean {
        if (username.isBlank()) return false
        val target = username.lowercase()
        return content.split(Regex("\\s+")).any {
            it.startsWith("@") &&
                it.removePrefix("@").trimEnd('.', ',', '!', '?', ':', ';').lowercase() == target
        }
    }

    // ── Le sujet du jour ──────────────────────────────────────────────────────
    // Une amorce quotidienne. Elle change à minuit et elle est la même pour
    // tout le monde : c'est ce qui fait qu'on ouvre l'app le matin.

    private val DAILY_PROMPTS = listOf(
        "Quelle est la rumeur la plus folle que t'as entendue cette semaine ?",
        "Qui forme le couple le moins discret du campus ?",
        "Balance le prof le plus imprévisible (sans nom, on n'est pas des sauvages).",
        "Le pire truc jamais vu en amphi ?",
        "Quelle soirée a laissé le plus de séquelles ?",
        "Le plus gros mytho du campus, c'est qui ?",
        "Une rumeur que tout le monde croit et qui est fausse ?",
        "Qu'est-ce qui s'est vraiment passé après la dernière soirée ?",
        "Le secret le plus mal gardé de ta promo ?",
        "La théorie la plus tordue que t'as sur quelqu'un ?",
        "Qui mérite le titre de star du campus cette semaine ?",
        "Confesse le truc que t'as fait et que personne ne sait.",
        "La plus grosse arnaque du campus ?",
        "Qu'est-ce qu'on ne t'a jamais pardonné ?"
    )

    /** Le sujet change chaque jour, identique pour tout le monde. */
    fun dailyPrompt(): String {
        val day = (System.currentTimeMillis() / 86_400_000L).toInt()
        return DAILY_PROMPTS[abs(day) % DAILY_PROMPTS.size]
    }
}
