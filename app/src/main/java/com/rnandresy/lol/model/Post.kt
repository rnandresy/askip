package com.rnandresy.lol.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.rnandresy.lol.utils.SCOOP_HOT_WEIGHT
import com.rnandresy.lol.utils.VERDICT_CONFIRMED_AT
import com.rnandresy.lol.utils.VERDICT_DEBUNKED_AT
import com.rnandresy.lol.utils.VERDICT_MIN_VOTES
import kotlin.math.pow

/** Où en est une rumeur : le campus a tranché, ou pas encore. */
enum class Verdict(val label: String, val emoji: String, val colorHex: String) {
    INVESTIGATING("En enquête", "⌕", "#8E8E93"),
    CONTESTED("Contestée", "⚔", "#F59E0B"),
    CONFIRMED("Confirmée", "✓", "#10B981"),
    DEBUNKED("Démentie", "✕", "#EF4444")
}

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoUrl: String = "",
    val content: String = "",
    val postType: String = "normal",
    @get:PropertyName("isAnonymous")
    @set:PropertyName("isAnonymous")
    var isAnonymous: Boolean = false,
    // ── Médias ────────────────────────────────────────────────────────────────
    val imageUrl: String = "",
    val videoUrl: String = "",
    val audioUrl: String = "",        // note vocale dans le post
    val audioDuration: Int = 0,
    val fileUrl: String = "",
    val fileName: String = "",
    // ── Sondage ───────────────────────────────────────────────────────────────
    val pollOption1: String = "",
    val pollOption2: String = "",
    val pollVotes1: Int = 0,
    val pollVotes2: Int = 0,
    val pollVoters: List<String> = emptyList(),
    // ── Réactions ─────────────────────────────────────────────────────────────
    val likedBy: List<String> = emptyList(),
    val fireBy: List<String> = emptyList(),
    val lolBy: List<String> = emptyList(),
    val shockBy: List<String> = emptyList(),
    val eyesBy: List<String> = emptyList(),
    // ── Rumeur-mètre ──────────────────────────────────────────────────────────
    /** Ceux qui trouvent la rumeur crédible. */
    val credibleBy: List<String> = emptyList(),
    /** Ceux qui la trouvent bidon. */
    val fakeBy: List<String> = emptyList(),
    /** Le Scoop : une pépite par personne et par jour. Rare, donc précieux. */
    val scoopBy: List<String> = emptyList(),
    // ── Capsule scellée ───────────────────────────────────────────────────────
    /** 0 = rumeur normale. Sinon, date à laquelle le contenu devient lisible. */
    val sealedUntil: Long = 0L,
    /** Nombre de clés qui ouvrent la capsule avant l'heure. 0 = impossible. */
    val keysNeeded: Int = 0,
    /** Ceux qui ont déjà donné leur clé. */
    val keysBy: List<String> = emptyList(),
    // ── Téléphone arabe ───────────────────────────────────────────────────────
    /** Nombre de maillons posés. 0 pour une rumeur classique. */
    val chainCount: Int = 0,
    /** Ceux qui ont déjà ajouté un maillon — une seule contribution par personne. */
    val chainAuthors: List<String> = emptyList(),
    /**
     * Dernier maillon, recopié ici pour l'aperçu du fil.
     * Sans cette dénormalisation, afficher une chaîne dans la liste imposerait
     * un écouteur Firestore par carte visible.
     */
    val chainLastContent: String = "",
    val chainLastAuthor: String = "",
    // ── Droit de réponse ──────────────────────────────────────────────────────
    /** Les personnes citées qui ont exercé leur droit de réponse. */
    val repliedBy: List<String> = emptyList(),
    // ── Classement ────────────────────────────────────────────────────────────
    val tags: List<String> = emptyList(),
    // ── Meta ──────────────────────────────────────────────────────────────────
    val commentCount: Int = 0,
    @get:PropertyName("isPinned")
    @set:PropertyName("isPinned")
    var isPinned: Boolean = false,
    @get:PropertyName("isEdited")
    @set:PropertyName("isEdited")
    var isEdited: Boolean = false,
    /** 0 = permanent. Sinon la rumeur disparaît du fil après cette date. */
    val expiresAt: Long = 0L,
    val timestamp: Long = 0L
) {

    // ── Réactions ─────────────────────────────────────────────────────────────

    fun getUserReaction(uid: String): String? = when {
        uid in likedBy -> "♡"
        uid in fireBy -> "✦"
        uid in lolBy -> "◎"
        uid in shockBy -> "◎"
        uid in eyesBy -> "◎"
        else -> null
    }

    fun reactionCount(emoji: String) = when (emoji) {
        "love" -> likedBy.size
        "fire" -> fireBy.size
        "lol" -> lolBy.size
        "shock" -> shockBy.size
        "eyes" -> eyesBy.size
        else -> 0
    }

    @Exclude
    fun totalReactions(): Int =
        likedBy.size + fireBy.size + lolBy.size + shockBy.size + eyesBy.size

    /**
     * Applique une réaction en local, sans attendre le réseau.
     * Une seule réaction par personne : la nouvelle remplace l'ancienne.
     */
    fun withReaction(uid: String, emoji: String?): Post = copy(
        likedBy = likedBy.toggled(uid, emoji == "♡"),
        fireBy = fireBy.toggled(uid, emoji == "✦"),
        lolBy = lolBy.toggled(uid, emoji == "◎"),
        shockBy = shockBy.toggled(uid, emoji == "◎"),
        eyesBy = eyesBy.toggled(uid, emoji == "◎")
    )

    // ── Rumeur-mètre ──────────────────────────────────────────────────────────

    @Exclude
    fun verdictVotes(): Int = credibleBy.size + fakeBy.size

    /** Part de « crédible », entre 0 et 1. Vaut 0.5 tant que personne n'a voté. */
    @Exclude
    fun credibilityRatio(): Float {
        val total = verdictVotes()
        return if (total == 0) 0.5f else credibleBy.size.toFloat() / total
    }

    @Exclude
    fun verdict(): Verdict {
        if (verdictVotes() < VERDICT_MIN_VOTES) return Verdict.INVESTIGATING
        val ratio = credibilityRatio()
        return when {
            ratio >= VERDICT_CONFIRMED_AT -> Verdict.CONFIRMED
            ratio <= VERDICT_DEBUNKED_AT -> Verdict.DEBUNKED
            else -> Verdict.CONTESTED
        }
    }

    /** null = pas encore voté, true = crédible, false = bidon. */
    fun myVerdictVote(uid: String): Boolean? = when (uid) {
        in credibleBy -> true
        in fakeBy -> false
        else -> null
    }

    fun withVerdictVote(uid: String, credible: Boolean?): Post = copy(
        credibleBy = credibleBy.toggled(uid, credible == true),
        fakeBy = fakeBy.toggled(uid, credible == false)
    )

    fun withScoop(uid: String): Post =
        if (uid in scoopBy) this else copy(scoopBy = scoopBy + uid)

    fun withKey(uid: String): Post =
        if (uid in keysBy) this else copy(keysBy = keysBy + uid)

    // ── Sondage ───────────────────────────────────────────────────────────────

    fun withVote(uid: String, option: Int): Post =
        if (uid in pollVoters) this
        else copy(
            pollVotes1 = pollVotes1 + if (option == 1) 1 else 0,
            pollVotes2 = pollVotes2 + if (option == 2) 1 else 0,
            pollVoters = pollVoters + uid
        )

    // ── Classement « ça chauffe » ─────────────────────────────────────────────

    /**
     * Score de chaleur, façon gravité : l'engagement pousse vers le haut,
     * le temps tire vers le bas. Une rumeur d'hier doit être vraiment brûlante
     * pour tenir tête à une rumeur de ce matin.
     */
    @Exclude
    fun hotScore(now: Long = System.currentTimeMillis()): Double {
        val engagement = totalReactions() +
            2.0 * commentCount +
            SCOOP_HOT_WEIGHT * scoopBy.size +
            1.5 * verdictVotes() +
            0.5 * pollVoters.size
        val hours = ((now - timestamp).coerceAtLeast(0L)) / 3_600_000.0
        val boost = if (isPinned) 250.0 else 0.0
        return boost + engagement / (hours + 2.0).pow(1.45)
    }

    /** Score « tout temps », sans décroissance — pour le panthéon. */
    @Exclude
    fun legendScore(): Double =
        totalReactions() + 2.0 * commentCount + 10.0 * scoopBy.size +
            (if (verdict() == Verdict.CONFIRMED) 30.0 else 0.0)

    // ── Divers ────────────────────────────────────────────────────────────────

    @Exclude
    fun hasMedia() = imageUrl.isNotBlank() || videoUrl.isNotBlank() ||
        audioUrl.isNotBlank() || fileUrl.isNotBlank()

    @Exclude
    fun isExpired(now: Long = System.currentTimeMillis()) = expiresAt in 1 until now

    @Exclude
    fun isConfession() = postType == "confession" || isAnonymous

    // ── Les trois actualités ──────────────────────────────────────────────────

    /** Une rumeur de l'actualité vocale : elle ne se lit pas, elle s'écoute. */
    @Exclude
    fun isVoice() = postType == "voice" && audioUrl.isNotBlank()

    /** Une rumeur publiée sous serment, sur la Page de Vérité. */
    @Exclude
    fun isTruth() = postType == "truth"

    /**
     * Le serment a été démenti par le campus.
     *
     * C'est toute l'ironie de la Page de Vérité : on n'y publie que sous
     * serment, et c'est le verdict collectif qui décide si le serment tenait.
     */
    @Exclude
    fun isPerjury() = isTruth() && verdict() == Verdict.DEBUNKED

    /** Le serment a tenu : le campus a confirmé. */
    @Exclude
    fun isUpheld() = isTruth() && verdict() == Verdict.CONFIRMED

    // ── Capsule scellée ───────────────────────────────────────────────────────

    @Exclude
    fun isSealed() = sealedUntil > 0L

    /**
     * La capsule est-elle ouverte ? Deux façons : l'heure est passée, ou le
     * campus a réuni assez de clés. Le serveur applique exactement la même
     * règle — ceci n'est qu'un miroir pour l'affichage.
     */
    @Exclude
    fun isUnsealed(now: Long = System.currentTimeMillis()): Boolean =
        !isSealed() || now >= sealedUntil || (keysNeeded > 0 && keysBy.size >= keysNeeded)

    @Exclude
    fun keysMissing(): Int = (keysNeeded - keysBy.size).coerceAtLeast(0)

    @Exclude
    fun msUntilUnseal(now: Long = System.currentTimeMillis()): Long =
        (sealedUntil - now).coerceAtLeast(0L)

    // ── Téléphone arabe ───────────────────────────────────────────────────────

    @Exclude
    fun isChain() = postType == "chain"

    @Exclude
    fun chainIsFull(max: Int) = chainCount >= max

    /** Peut-on encore ajouter un maillon ? Une seule contribution par personne. */
    @Exclude
    fun canAddLink(uid: String, max: Int): Boolean =
        isChain() && !chainIsFull(max) && uid !in chainAuthors

    companion object {
        fun reactionFieldFor(emoji: String) = when (emoji) {
            "love" -> "likedBy"
            "fire" -> "fireBy"
            "lol" -> "lolBy"
            "shock" -> "shockBy"
            "eyes" -> "eyesBy"
            else -> "likedBy"
        }

        val REACTION_FIELDS = listOf("likedBy", "fireBy", "lolBy", "shockBy", "eyesBy")
    }
}

/** Ajoute ou retire [uid] selon [present], sans jamais créer de doublon. */
private fun List<String>.toggled(uid: String, present: Boolean): List<String> =
    if (present) {
        if (uid in this) this else this + uid
    } else {
        this.filterNot { it == uid }
    }
