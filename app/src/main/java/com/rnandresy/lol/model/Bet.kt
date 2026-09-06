package com.rnandresy.lol.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * Un pari posé sur le verdict d'une rumeur, avant qu'il tombe.
 *
 * La cote est figée **au moment du pari** : c'est ce qui récompense le flair.
 * Miser « crédible » quand tout le monde crie au bidon rapporte gros ; suivre
 * la foule une fois le verdict évident ne rapporte presque rien.
 *
 * Un document par personne et par rumeur, dont l'id est l'UID : impossible de
 * parier deux fois sur la même rumeur.
 */
data class Bet(
    val userId: String = "",
    val postId: String = "",
    /** true = « je parie que c'est crédible », false = « je parie que c'est bidon ». */
    @get:PropertyName("onCredible")
    @set:PropertyName("onCredible")
    var onCredible: Boolean = true,
    /** Nombre de jetons misés. */
    val stake: Int = 1,
    /** Part des votes déjà de ton côté au moment du pari — sert à figer la cote. */
    val ratioAtBet: Float = 0.5f,
    /** Total de votes au moment du pari — moins il y en avait, plus c'était risqué. */
    val votesAtBet: Int = 0,
    /** Cote figée, calculée une fois puis jamais recalculée. */
    val odds: Float = 1f,
    // ── Règlement ─────────────────────────────────────────────────────────────
    @get:PropertyName("settled")
    @set:PropertyName("settled")
    var settled: Boolean = false,
    @get:PropertyName("won")
    @set:PropertyName("won")
    var won: Boolean = false,
    /** Clout effectivement gagné (positif) ou perdu (négatif) au règlement. */
    val payout: Long = 0L,
    val timestamp: Long = 0L
) {
    @Exclude
    fun sideLabel(): String = if (onCredible) "Crédible" else "Bidon"

    @Exclude
    fun sideEmoji(): String = if (onCredible) "✓" else "✕"

    /** Ce que ce pari rapporterait s'il était gagnant. */
    @Exclude
    fun potentialGain(): Long =
        (com.rnandresy.lol.utils.BET_BASE_REWARD * stake * odds).toLong()
}
