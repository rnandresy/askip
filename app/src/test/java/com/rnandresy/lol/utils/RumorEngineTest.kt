package com.rnandresy.lol.utils

import com.rnandresy.lol.model.Post
import com.rnandresy.lol.model.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le moteur de la rumeur.
 *
 * `RumorEngine` se dit « volontairement sans état et sans réseau » : c'est
 * exactement ce qui le rend vérifiable. Ce qu'on protège ici, ce sont les
 * promesses faites au joueur — le masque qu'on ne peut pas recouper, les trois
 * actualités qui ne se mélangent jamais, la cote du Pari plafonnée à ×5.
 */
class RumorEngineTest {

    private fun post(
        id: String = "p",
        userId: String = "u",
        type: String = "normal",
        content: String = "",
        audio: String = "",
        timestamp: Long = 0L,
        expiresAt: Long = 0L,
        pinned: Boolean = false,
        anonymous: Boolean = false,
        credible: Int = 0,
        fake: Int = 0,
        tags: List<String> = emptyList()
    ) = Post(
        id = id,
        userId = userId,
        postType = type,
        content = content,
        audioUrl = audio,
        timestamp = timestamp,
        expiresAt = expiresAt,
        tags = tags,
        credibleBy = List(credible) { "credible$it" },
        fakeBy = List(fake) { "bidon$it" }
    ).apply {
        isPinned = pinned
        isAnonymous = anonymous
    }

    // ── Les masques anonymes ──────────────────────────────────────────────────

    @Test
    fun `le masque ne bouge jamais pour un couple donne`() {
        // La promesse du produit : « Le Corbeau #7B2 » doit rester le même
        // personnage tout au long d'un fil de commentaires.
        repeat(5) {
            assertEquals(
                RumorEngine.anonAlias("u1", "p1"),
                RumorEngine.anonAlias("u1", "p1")
            )
        }
    }

    @Test
    fun `le masque change d'une rumeur a l'autre, donc on ne recoupe pas`() {
        assertNotEquals(
            RumorEngine.anonAlias("u1", "p1"),
            RumorEngine.anonAlias("u1", "p2")
        )

        // Et pas seulement pour ce couple-là : sur cinquante rumeurs, une même
        // personne doit porter cinquante masques différents.
        val masques = (1..50).map { RumorEngine.anonAlias("u1", "post$it") }.toSet()
        assertEquals("un même auteur ne doit pas réutiliser un masque", 50, masques.size)
    }

    @Test
    fun `deux personnes sous la meme rumeur ne portent pas le meme masque`() {
        val masques = (1..50).map { RumorEngine.anonAlias("user$it", "p1") }.toSet()
        assertEquals(50, masques.size)
    }

    @Test
    fun `le masque garde toujours la meme forme`() {
        // Un nom, un dièse, trois hexadécimales. L'interface compte dessus pour
        // que les listes serrées ne débordent pas.
        val forme = Regex("^.+ #[0-9A-F]{3}$")
        (1..200).forEach { i ->
            val alias = RumorEngine.anonAlias("u$i", "p$i")
            assertTrue("« $alias » ne suit pas la forme attendue", forme.matches(alias))
        }
    }

    @Test
    fun `sans auteur, il n'y a pas de masque a donner`() {
        assertEquals("Quelqu'un", RumorEngine.anonAlias("", "p1"))
    }

    @Test
    fun `l'ordre de la liste des masques fait partie du contrat`() {
        // Réordonner MASKS ou toucher au calcul du hachage changerait le
        // pseudonyme de tout le monde d'un coup, y compris sur les rumeurs
        // déjà publiées. Cette valeur est là pour que ça ne passe pas inaperçu.
        assertEquals("Le Hibou #CF2", RumorEngine.anonAlias("u1", "p1"))
    }

    // ── Les tags ──────────────────────────────────────────────────────────────

    @Test
    fun `les tags sont ramasses en minuscules, sans doublon et plafonnes`() {
        val tags = RumorEngine.extractTags("#Soiree #SOIREE #Amphi #Promo #Encore #DeTrop")
        assertEquals(listOf("soiree", "amphi", "promo"), tags)
        assertEquals(MAX_TAGS_PER_POST, tags.size)
    }

    @Test
    fun `un dièse seul ou trop court n'est pas un tag`() {
        assertTrue(RumorEngine.extractTags("# #a rien ici").isEmpty())
    }

    @Test
    fun `les tags choisis et ceux ecrits fusionnent sans doublon`() {
        val tags = RumorEngine.mergeTags(listOf("Soiree"), "encore une #soiree de #promo")
        assertEquals(listOf("soiree", "promo"), tags)
    }

    // ── Les trois actualités ──────────────────────────────────────────────────

    @Test
    fun `les trois actualites ne se melangent jamais`() {
        val ordinaire = post(id = "a", timestamp = 3)
        val vocale = post(id = "b", type = "voice", audio = "http://x.m4a", timestamp = 2)
        val serment = post(id = "c", type = "truth", timestamp = 1)
        val tout = listOf(ordinaire, vocale, serment)

        val principale = RumorEngine.applySection(tout, FeedSection.MAIN, FeedTab.FRESH, now = 10)
        assertEquals(
            "l'actualité principale ne montre ni vocal ni serment",
            listOf("a"),
            principale.map { it.id }
        )

        val vocal = RumorEngine.applySection(tout, FeedSection.VOICE, FeedTab.FRESH, now = 10)
        assertEquals(listOf("b"), vocal.map { it.id })

        val verite = RumorEngine.applySection(tout, FeedSection.TRUTH, FeedTab.FRESH, now = 10)
        assertEquals(listOf("c"), verite.map { it.id })
    }

    @Test
    fun `une rumeur vocale sans audio n'est pas une rumeur vocale`() {
        // `isVoice()` exige les deux : le type et le fichier. Sans ça, une
        // rumeur vocale ratée disparaîtrait de toutes les actualités.
        val bancale = post(id = "a", type = "voice", audio = "")
        val vocal = RumorEngine.applySection(
            listOf(bancale), FeedSection.VOICE, FeedTab.FRESH, now = 10
        )
        assertTrue(vocal.isEmpty())
    }

    @Test
    fun `les confessions ne remontent pas dans les fils signes`() {
        val signee = post(id = "a", timestamp = 1)
        val confession = post(id = "b", timestamp = 2, anonymous = true)
        val tout = listOf(signee, confession)

        assertEquals(listOf("a"), RumorEngine.applyTab(tout, FeedTab.HOT, now = 10).map { it.id })
        assertEquals(listOf("a"), RumorEngine.applyTab(tout, FeedTab.FRESH, now = 10).map { it.id })
        assertEquals(
            listOf("b"),
            RumorEngine.applyTab(tout, FeedTab.CONFESSIONS, now = 10).map { it.id }
        )
    }

    // ── Le tri ────────────────────────────────────────────────────────────────

    @Test
    fun `une rumeur expiree ne s'affiche plus nulle part`() {
        val maintenant = 1_000L
        val expiree = post(id = "mort", timestamp = 1, expiresAt = maintenant - 1)
        val vivante = post(id = "vif", timestamp = 1)
        val tout = listOf(expiree, vivante)

        assertEquals(listOf("vif"), RumorEngine.sortHot(tout, maintenant).map { it.id })
        assertEquals(listOf("vif"), RumorEngine.sortFresh(tout, maintenant).map { it.id })
        assertEquals(
            listOf("vif"),
            RumorEngine.applyTab(tout, FeedTab.LEGENDS, maintenant).map { it.id }
        )
    }

    @Test
    fun `les epinglees restent en tete, meme vieilles`() {
        val maintenant = 100_000_000L
        val epinglee = post(id = "pin", timestamp = 1, pinned = true)
        val recente = post(id = "neuf", timestamp = maintenant)

        assertEquals(
            listOf("pin", "neuf"),
            RumorEngine.sortFresh(listOf(recente, epinglee), maintenant).map { it.id }
        )
    }

    // ── La Page de Vérité ─────────────────────────────────────────────────────

    @Test
    fun `le registre ne compte que les serments, et seulement ceux qui sont tranches`() {
        val registre = RumorEngine.truthLedger(
            listOf(
                post(id = "a", type = "truth", credible = 0, fake = VERDICT_MIN_VOTES),
                post(id = "b", type = "truth", credible = VERDICT_MIN_VOTES, fake = 0),
                post(id = "c", type = "truth"),                       // pas encore tranché
                post(id = "d", credible = 0, fake = VERDICT_MIN_VOTES) // pas un serment
            )
        )

        assertEquals("trois serments prêtés", 3, registre.sworn)
        assertEquals("deux tranchés", 2, registre.judged)
        assertEquals(1, registre.perjuries)
        assertEquals(1, registre.upheld)
        assertEquals(0.5f, registre.perjuryRate, 0.0001f)
    }

    @Test
    fun `une page vierge ne divise pas par zero`() {
        val vide = RumorEngine.truthLedger(emptyList())
        assertEquals(0f, vide.perjuryRate, 0.0001f)
        assertTrue(vide.statement.isNotBlank())
    }

    // ── Le Pari ───────────────────────────────────────────────────────────────

    @Test
    fun `la cote ne depasse jamais le plafond annonce`() {
        // Le pire cas possible : tout le monde contre soi, et personne n'a
        // encore voté. Le produit promet ×5, pas davantage.
        assertEquals(BET_MAX_ODDS, RumorEngine.betOdds(ratioForSide = 0f, votes = 0), 0.0001f)
        assertEquals(BET_MAX_ODDS, RumorEngine.betOdds(ratioForSide = 0.01f, votes = 0), 0.0001f)
    }

    @Test
    fun `suivre la foule sur une rumeur deja jugee ne rapporte presque rien`() {
        assertEquals(
            1f,
            RumorEngine.betOdds(ratioForSide = 1f, votes = BET_EARLY_VOTES),
            0.0001f
        )
    }

    @Test
    fun `aller contre la foule rapporte plus que la suivre`() {
        val contre = RumorEngine.betOdds(ratioForSide = 0.3f, votes = BET_EARLY_VOTES)
        val avec = RumorEngine.betOdds(ratioForSide = 0.7f, votes = BET_EARLY_VOTES)
        assertTrue("$contre devrait dépasser $avec", contre > avec)
    }

    @Test
    fun `parier tot rapporte plus que parier tard`() {
        val tot = RumorEngine.betOdds(ratioForSide = 0.5f, votes = 0)
        val tard = RumorEngine.betOdds(ratioForSide = 0.5f, votes = BET_EARLY_VOTES)
        assertTrue("$tot devrait dépasser $tard", tot > tard)
    }

    @Test
    fun `la cote reste toujours dans ses bornes`() {
        for (ratio in 0..100) {
            for (votes in listOf(0, 1, 7, BET_EARLY_VOTES, BET_EARLY_VOTES * 10)) {
                val cote = RumorEngine.betOdds(ratio / 100f, votes)
                assertTrue("cote $cote hors bornes", cote in 1f..BET_MAX_ODDS)
            }
        }
    }

    @Test
    fun `sans aucun vote, les deux camps sont a egalite`() {
        assertEquals(0.5f, RumorEngine.sideRatio(0, 0, onCredible = true), 0.0001f)
        assertEquals(0.5f, RumorEngine.sideRatio(0, 0, onCredible = false), 0.0001f)
    }

    @Test
    fun `la part d'un camp se lit des deux cotes`() {
        assertEquals(0.75f, RumorEngine.sideRatio(3, 1, onCredible = true), 0.0001f)
        assertEquals(0.25f, RumorEngine.sideRatio(3, 1, onCredible = false), 0.0001f)
    }

    // ── La Météo du campus ────────────────────────────────────────────────────

    @Test
    fun `un campus muet est un campus calme`() {
        val meteo = RumorEngine.campusWeather(emptyList(), now = 1_000_000L)
        assertEquals(RumorEngine.Weather.CALM, meteo.weather)
        assertEquals(0, meteo.postsToday)
        assertEquals(0f, meteo.dramaRatio, 0.0001f)
    }

    @Test
    fun `la meteo ne regarde que les dernieres 24 heures`() {
        val maintenant = 100_000_000L
        val jour = 86_400_000L
        val meteo = RumorEngine.campusWeather(
            listOf(
                post(id = "hier", timestamp = maintenant - jour - 1),
                post(id = "aujourd hui", timestamp = maintenant - 1)
            ),
            now = maintenant
        )
        assertEquals(1, meteo.postsToday)
    }

    @Test
    fun `plus ca se dispute, plus le temps se gate`() {
        val maintenant = 100_000_000L
        val calme = List(6) { post(id = "c$it", timestamp = maintenant, credible = 20, fake = 0) }
        val dispute = List(6) {
            post(id = "d$it", timestamp = maintenant, credible = 10, fake = 10)
        }

        val friction = RumorEngine.campusWeather(dispute, maintenant).dramaRatio
        assertTrue(
            "des rumeurs contestées doivent faire monter la friction",
            friction > RumorEngine.campusWeather(calme, maintenant).dramaRatio
        )
        assertEquals(Verdict.CONTESTED, dispute.first().verdict())
    }

    // ── Le droit de réponse ───────────────────────────────────────────────────

    @Test
    fun `on reconnait une personne citee malgre la casse et la ponctuation`() {
        assertTrue(RumorEngine.mentions("paraît que @Christiano a tout vu", "christiano"))
        assertTrue(RumorEngine.mentions("@christiano, sérieux ?", "Christiano"))
        assertTrue(RumorEngine.mentions("et @christiano !", "christiano"))
    }

    @Test
    fun `une citation partielle ne compte pas`() {
        assertFalse(RumorEngine.mentions("@christianoX a tout vu", "christiano"))
        assertFalse(RumorEngine.mentions("christiano sans arobase", "christiano"))
        assertFalse(RumorEngine.mentions("@christiano", ""))
    }

    // ── Le Scoop ──────────────────────────────────────────────────────────────

    @Test
    fun `le Scoop se recharge le lendemain, pas le jour meme`() {
        assertFalse("déjà donné aujourd'hui", RumorEngine.canScoop(RumorEngine.today()))
        assertTrue("hier ne compte plus", RumorEngine.canScoop(RumorEngine.yesterday()))
        assertTrue("jamais donné", RumorEngine.canScoop(""))
    }

    @Test
    fun `la recharge du Scoop est toujours a venir, jamais negative`() {
        assertTrue(RumorEngine.msUntilScoopReset() >= 0L)
    }

    // ── Le sujet du jour ──────────────────────────────────────────────────────

    @Test
    fun `le sujet du jour est le meme pour tout le monde a un instant donne`() {
        assertEquals(RumorEngine.dailyPrompt(), RumorEngine.dailyPrompt())
        assertTrue(RumorEngine.dailyPrompt().isNotBlank())
    }
}
