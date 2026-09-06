package com.rnandresy.lol.utils

import kotlin.math.abs

/**
 * Les missions du jour.
 *
 * Trois missions, tirées de façon déterministe à partir de la date : tout le
 * monde a les mêmes, elles changent à minuit, et elles n'exigent aucune
 * lecture serveur. C'est la boucle qui donne une raison de revenir demain
 * même quand le fil est calme.
 */
enum class QuestKind {
    REACT,      // réagir à des rumeurs
    COMMENT,    // commenter
    POST,       // publier une rumeur
    VERDICT,    // trancher : crédible ou bidon
    SCOOP,      // dépenser son Scoop du jour
    STORY,      // publier une story
    CONFESS,    // publier une confession
    OPEN_TAG,   // explorer un salon de tag
    BET,        // miser un jeton sur un verdict
    CHAIN,      // ajouter un maillon au téléphone arabe
    KEY         // donner sa clé à une capsule scellée
}

data class QuestDef(
    val id: String,
    val kind: QuestKind,
    val emoji: String,
    val title: String,
    val target: Int,
    val xp: Long = XP_QUEST
)

val ALL_QUESTS = listOf(
    QuestDef("react_5", QuestKind.REACT, "🔥", "Réagis à 5 rumeurs", 5),
    QuestDef("react_12", QuestKind.REACT, "⚡", "Réagis à 12 rumeurs", 12, XP_QUEST * 2),
    QuestDef("comment_3", QuestKind.COMMENT, "💬", "Laisse 3 commentaires", 3),
    QuestDef("comment_1", QuestKind.COMMENT, "🗨️", "Commente une rumeur", 1),
    QuestDef("post_1", QuestKind.POST, "📢", "Balance une rumeur", 1),
    QuestDef("verdict_5", QuestKind.VERDICT, "⚖️", "Tranche sur 5 rumeurs", 5),
    QuestDef("verdict_10", QuestKind.VERDICT, "🔍", "Mène l'enquête sur 10 rumeurs", 10, XP_QUEST * 2),
    QuestDef("scoop_1", QuestKind.SCOOP, "💎", "Offre ton Scoop du jour", 1, XP_QUEST + 20),
    QuestDef("story_1", QuestKind.STORY, "📖", "Publie une story", 1),
    QuestDef("confess_1", QuestKind.CONFESS, "🎭", "Publie une confession", 1),
    QuestDef("tag_2", QuestKind.OPEN_TAG, "🏷️", "Explore 2 salons", 2),
    QuestDef("bet_1", QuestKind.BET, "🎟", "Mise un jeton sur un verdict", 1),
    QuestDef("bet_3", QuestKind.BET, "🎰", "Place 3 paris", 3, XP_QUEST * 2),
    QuestDef("chain_1", QuestKind.CHAIN, "📞", "Ajoute un maillon à une chaîne", 1),
    QuestDef("key_2", QuestKind.KEY, "🔑", "Donne 2 clés à des capsules", 2)
)

/**
 * État des missions pour une journée.
 * [counts] est indexé par id de mission, [claimed] retient les XP déjà encaissés.
 */
data class QuestProgress(
    val date: String = "",
    val counts: Map<String, Int> = emptyMap(),
    val claimed: Set<String> = emptySet()
) {
    fun countOf(id: String) = counts[id] ?: 0
    fun isDone(def: QuestDef) = countOf(def.id) >= def.target
    fun isClaimed(def: QuestDef) = def.id in claimed
    fun canClaim(def: QuestDef) = isDone(def) && !isClaimed(def)

    fun progressOf(def: QuestDef): Float =
        (countOf(def.id).toFloat() / def.target).coerceIn(0f, 1f)

    // ── Sérialisation compacte pour DataStore ─────────────────────────────────
    // Format : date|id:count,id:count|id,id
    fun serialize(): String {
        val c = counts.entries.joinToString(",") { "${it.key}:${it.value}" }
        return "$date|$c|${claimed.joinToString(",")}"
    }

    companion object {
        fun parse(raw: String): QuestProgress {
            if (raw.isBlank()) return QuestProgress()
            val parts = raw.split("|")
            if (parts.size < 3) return QuestProgress()
            val counts = parts[1].split(",")
                .mapNotNull { entry ->
                    val kv = entry.split(":")
                    if (kv.size == 2) kv[0] to (kv[1].toIntOrNull() ?: 0) else null
                }
                .toMap()
            val claimed = parts[2].split(",").filter { it.isNotBlank() }.toSet()
            return QuestProgress(parts[0], counts, claimed)
        }
    }
}

object Quests {

    /** Les trois missions du jour — identiques pour tout le monde, nouvelles à minuit. */
    fun today(date: String = RumorEngine.today()): List<QuestDef> {
        val seed = abs(date.hashCode())
        val pool = ALL_QUESTS.toMutableList()
        val picked = mutableListOf<QuestDef>()
        var s = seed
        repeat(3) {
            if (pool.isEmpty()) return@repeat
            s = (s * 1_103_515_245 + 12_345) and 0x7FFFFFFF
            val def = pool.removeAt(s % pool.size)
            // Deux missions du même type le même jour, c'est répétitif.
            pool.removeAll { it.kind == def.kind }
            picked += def
        }
        return picked
    }

    /** Remet les compteurs à zéro si on a changé de jour. */
    fun normalize(progress: QuestProgress, date: String = RumorEngine.today()): QuestProgress =
        if (progress.date == date) progress else QuestProgress(date = date)

    /** Incrémente les missions du jour concernées par [kind]. */
    fun record(
        progress: QuestProgress,
        kind: QuestKind,
        by: Int = 1,
        date: String = RumorEngine.today()
    ): QuestProgress {
        val current = normalize(progress, date)
        val relevant = today(date).filter { it.kind == kind }
        if (relevant.isEmpty()) return current
        val counts = current.counts.toMutableMap()
        relevant.forEach { def ->
            counts[def.id] = (counts[def.id] ?: 0) + by
        }
        return current.copy(counts = counts)
    }

    fun claim(progress: QuestProgress, def: QuestDef): QuestProgress =
        if (progress.canClaim(def)) progress.copy(claimed = progress.claimed + def.id)
        else progress

    /** XP en attente d'être encaissée — sert à afficher la pastille. */
    fun claimableXp(progress: QuestProgress, date: String = RumorEngine.today()): Long =
        today(date).filter { progress.canClaim(it) }.sumOf { it.xp }

    fun allDone(progress: QuestProgress, date: String = RumorEngine.today()): Boolean =
        today(date).all { progress.isDone(it) }
}
