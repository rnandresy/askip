package com.rnandresy.lol.model

import com.rnandresy.lol.utils.VERDICT_CONFIRMED_AT
import com.rnandresy.lol.utils.VERDICT_DEBUNKED_AT
import com.rnandresy.lol.utils.VERDICT_MIN_VOTES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Le Rumeur-mètre — le cœur du jeu.
 *
 * Ces fonctions décident si une rumeur est confirmée ou démentie, donc du
 * clout que son auteur gagne ou perd, donc du classement. Elles sont pures :
 * rien ne les empêchait d'être vérifiées, sinon qu'aucune JVM ne démarre sur
 * la machine de développement. La CI, elle, les joue.
 *
 * Les seuils ne sont volontairement **pas** recopiés en dur ici : chaque test
 * se calcule à partir des constantes d'`AskipConstants`. Régler l'équilibrage
 * là-bas ne doit pas faire tomber ces tests — seul un changement de
 * *mécanique* doit les faire tomber.
 */
class PostTest {

    /** Une rumeur qui a reçu [credible] votes « crédible » et [fake] « bidon ». */
    private fun voted(credible: Int, fake: Int, type: String = "normal") = Post(
        id = "p",
        postType = type,
        credibleBy = List(credible) { "credible$it" },
        fakeBy = List(fake) { "bidon$it" }
    )

    /**
     * Une rumeur largement au-dessus du minimum de votes, dont la part de
     * « crédible » vaut environ [ratio].
     */
    private fun atRatio(ratio: Float, type: String = "normal"): Post {
        val total = VERDICT_MIN_VOTES * 4
        val credible = (total * ratio).roundToInt()
        return voted(credible, total - credible, type)
    }

    // ── Le seuil de votes ─────────────────────────────────────────────────────

    @Test
    fun `sous le minimum de votes, le campus n'a pas tranche`() {
        // Même une unanimité ne suffit pas : c'est ce qui empêche trois amis
        // de faire confirmer n'importe quoi.
        for (votes in 0 until VERDICT_MIN_VOTES) {
            assertEquals(
                "avec $votes vote(s) unanimes, le verdict doit rester en enquête",
                Verdict.INVESTIGATING,
                voted(credible = votes, fake = 0).verdict()
            )
        }
    }

    @Test
    fun `au minimum de votes, le verdict tombe`() {
        assertEquals(
            Verdict.CONFIRMED,
            voted(credible = VERDICT_MIN_VOTES, fake = 0).verdict()
        )
        assertEquals(
            Verdict.DEBUNKED,
            voted(credible = 0, fake = VERDICT_MIN_VOTES).verdict()
        )
    }

    // ── Les trois verdicts ────────────────────────────────────────────────────

    @Test
    fun `nettement au-dessus du seuil, la rumeur est confirmee`() {
        val ratio = (VERDICT_CONFIRMED_AT + 1f) / 2f
        assertEquals(Verdict.CONFIRMED, atRatio(ratio).verdict())
    }

    @Test
    fun `nettement en dessous du seuil, la rumeur est dementie`() {
        assertEquals(Verdict.DEBUNKED, atRatio(VERDICT_DEBUNKED_AT / 2f).verdict())
    }

    @Test
    fun `entre les deux seuils, la rumeur est contestee`() {
        val ratio = (VERDICT_DEBUNKED_AT + VERDICT_CONFIRMED_AT) / 2f
        assertEquals(Verdict.CONTESTED, atRatio(ratio).verdict())
    }

    @Test
    fun `sans aucun vote, la credibilite est au milieu`() {
        // 0.5 et non 0 : une rumeur sur laquelle personne ne s'est prononcé
        // n'est pas une rumeur démentie.
        assertEquals(0.5f, Post().credibilityRatio(), 0.0001f)
        assertEquals(0, Post().verdictVotes())
    }

    // ── Un vote par personne, et un seul ──────────────────────────────────────

    @Test
    fun `changer d'avis retire le vote precedent`() {
        val apres = Post(id = "p")
            .withVerdictVote("moi", credible = true)
            .withVerdictVote("moi", credible = false)

        assertFalse("« moi » ne peut pas être des deux côtés", "moi" in apres.credibleBy)
        assertTrue("moi" in apres.fakeBy)
        assertEquals(1, apres.verdictVotes())
    }

    @Test
    fun `retirer son vote ne laisse rien derriere`() {
        val apres = Post(id = "p")
            .withVerdictVote("moi", credible = true)
            .withVerdictVote("moi", credible = null)

        assertEquals(0, apres.verdictVotes())
    }

    @Test
    fun `une seule reaction par personne, la nouvelle remplace l'ancienne`() {
        val apres = Post(id = "p")
            .withReaction("moi", "love")
            .withReaction("moi", "fire")

        assertFalse("moi" in apres.likedBy)
        assertTrue("moi" in apres.fireBy)
        assertEquals(1, apres.totalReactions())
        assertEquals("fire", apres.getUserReaction("moi"))
    }

    @Test
    fun `voter deux fois du meme cote ne compte qu'une fois`() {
        val apres = Post(id = "p")
            .withVerdictVote("moi", credible = true)
            .withVerdictVote("moi", credible = true)

        assertEquals(1, apres.credibleBy.size)
    }

    // ── La Page de Vérité ─────────────────────────────────────────────────────

    @Test
    fun `un parjure est un serment dementi, et rien d'autre`() {
        val ratioDementi = VERDICT_DEBUNKED_AT / 2f
        val ratioConfirme = (VERDICT_CONFIRMED_AT + 1f) / 2f

        assertTrue(
            "un serment démenti est un parjure",
            atRatio(ratioDementi, type = "truth").isPerjury()
        )
        assertFalse(
            "une rumeur ordinaire démentie n'est pas un parjure : elle n'a rien juré",
            atRatio(ratioDementi, type = "normal").isPerjury()
        )
        assertFalse(
            "un serment confirmé n'est pas un parjure",
            atRatio(ratioConfirme, type = "truth").isPerjury()
        )
        assertTrue(atRatio(ratioConfirme, type = "truth").isUpheld())
    }

    // ── Le classement « ça chauffe » ──────────────────────────────────────────

    @Test
    fun `a engagement egal, la rumeur la plus recente passe devant`() {
        val maintenant = 1_000_000_000_000L
        val heure = 3_600_000L
        val commun = Post(id = "p", commentCount = 10, likedBy = List(5) { "u$it" })

        val fraiche = commun.copy(timestamp = maintenant - heure)
        val vieille = commun.copy(timestamp = maintenant - 48 * heure)

        assertTrue(
            "la gravité doit tirer la vieille rumeur vers le bas",
            fraiche.hotScore(maintenant) > vieille.hotScore(maintenant)
        )
    }

    @Test
    fun `une rumeur epinglee passe devant tout le reste`() {
        val maintenant = 1_000_000_000_000L
        val epinglee = Post(id = "a", timestamp = maintenant - 90 * 3_600_000L)
            .apply { isPinned = true }
        val brulante = Post(
            id = "b",
            timestamp = maintenant,
            commentCount = 100,
            likedBy = List(50) { "u$it" }
        )

        assertTrue(epinglee.hotScore(maintenant) > brulante.hotScore(maintenant))
    }

    @Test
    fun `le pantheon ignore le temps`() {
        // `legendScore` ne prend pas de `now` : deux rumeurs identiques à des
        // dates différentes doivent valoir exactement pareil.
        val a = Post(id = "a", commentCount = 4, timestamp = 1L)
        val b = a.copy(id = "b", timestamp = 9_000_000_000_000L)

        assertEquals(a.legendScore(), b.legendScore(), 0.0001)
    }

    // ── Capsule scellée ───────────────────────────────────────────────────────

    @Test
    fun `une capsule s'ouvre a l'heure dite, ou avec assez de cles`() {
        val heureDite = 5_000L
        val capsule = Post(id = "p", sealedUntil = heureDite, keysNeeded = 3)

        assertFalse("avant l'heure et sans clé, elle reste fermée", capsule.isUnsealed(now = 1L))
        assertTrue("à l'heure dite, elle s'ouvre", capsule.isUnsealed(now = heureDite))
        assertTrue(
            "assez de clés l'ouvrent avant l'heure",
            capsule.copy(keysBy = listOf("a", "b", "c")).isUnsealed(now = 1L)
        )
        assertEquals(1, capsule.copy(keysBy = listOf("a", "b")).keysMissing())
    }

    @Test
    fun `une rumeur sans capsule est toujours lisible`() {
        assertTrue(Post(id = "p").isUnsealed(now = 0L))
        assertFalse(Post(id = "p").isSealed())
    }

    // ── Téléphone arabe ───────────────────────────────────────────────────────

    @Test
    fun `on n'ajoute qu'un seul maillon, et pas au-dela du maximum`() {
        val chaine = Post(
            id = "p",
            postType = "chain",
            chainCount = 2,
            chainAuthors = listOf("moi")
        )

        assertFalse("une deuxième contribution est refusée", chaine.canAddLink("moi", max = 7))
        assertTrue(chaine.canAddLink("toi", max = 7))
        assertFalse("chaîne pleine", chaine.copy(chainCount = 7).canAddLink("toi", max = 7))
        assertFalse(
            "une rumeur ordinaire n'est pas une chaîne",
            Post(id = "p").canAddLink("toi", max = 7)
        )
    }
}
