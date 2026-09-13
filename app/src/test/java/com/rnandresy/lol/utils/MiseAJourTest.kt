package com.rnandresy.lol.utils

import com.rnandresy.lol.model.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La décision de bloquer une version.
 *
 * C'est le seul endroit du mécanisme de mise à jour qui puisse enfermer tout
 * le campus dehors d'un coup. Les cas ci-dessous gardent surtout une promesse :
 * **dans le doute, on ne bloque pas.**
 */
class MiseAJourTest {

    private val lien = "https://exemple.test/askip.apk"

    // ── Désarmé par défaut ────────────────────────────────────────────────────

    @Test
    fun `sans configuration lue, personne n'est bloque`() {
        assertEquals(EtatMiseAJour.AJour, etatMiseAJour(installee = 3, config = null))
    }

    @Test
    fun `un document absent ne bloque personne`() {
        // `AppConfigRepository` rend `AppConfig()` pour un document absent.
        assertEquals(EtatMiseAJour.AJour, etatMiseAJour(installee = 1, config = AppConfig()))
    }

    @Test
    fun `une version installee illisible n'est jamais bloquee`() {
        val config = AppConfig(minVersionCode = 99, downloadUrl = lien)
        assertEquals(EtatMiseAJour.AJour, etatMiseAJour(installee = 0, config = config))
        assertEquals(EtatMiseAJour.AJour, etatMiseAJour(installee = -1, config = config))
    }

    // ── Blocage ───────────────────────────────────────────────────────────────

    @Test
    fun `en dessous de la version minimale, l'app se bloque`() {
        val etat = etatMiseAJour(
            installee = 2,
            config = AppConfig(minVersionCode = 3, downloadUrl = lien, message = "  Mets à jour  ")
        )
        assertEquals(EtatMiseAJour.Requise(2, 3, lien, "Mets à jour"), etat)
    }

    @Test
    fun `la version minimale elle-meme est acceptee`() {
        assertEquals(
            EtatMiseAJour.AJour,
            etatMiseAJour(installee = 3, config = AppConfig(minVersionCode = 3))
        )
    }

    @Test
    fun `un blocage ne s'ecarte pas`() {
        // Même si la personne a déjà fermé une annonce pour une version plus
        // récente, une version trop vieille reste bloquée.
        val etat = etatMiseAJour(
            installee = 2,
            config = AppConfig(minVersionCode = 3, latestVersionCode = 4),
            annonceFermee = 4
        )
        assertTrue(etat is EtatMiseAJour.Requise)
    }

    @Test
    fun `le blocage l'emporte sur l'annonce`() {
        val etat = etatMiseAJour(
            installee = 2,
            config = AppConfig(minVersionCode = 3, latestVersionCode = 5)
        )
        assertTrue(etat is EtatMiseAJour.Requise)
    }

    // ── Annonce ───────────────────────────────────────────────────────────────

    @Test
    fun `une version plus recente est annoncee sans bloquer`() {
        assertEquals(
            EtatMiseAJour.Disponible(5, lien, ""),
            etatMiseAJour(installee = 3, config = AppConfig(latestVersionCode = 5, downloadUrl = lien))
        )
    }

    @Test
    fun `une annonce fermee ne revient pas pour la meme version`() {
        assertEquals(
            EtatMiseAJour.AJour,
            etatMiseAJour(installee = 3, config = AppConfig(latestVersionCode = 5), annonceFermee = 5)
        )
    }

    @Test
    fun `une annonce fermee revient pour une version encore plus recente`() {
        assertEquals(
            EtatMiseAJour.Disponible(6, "", ""),
            etatMiseAJour(installee = 3, config = AppConfig(latestVersionCode = 6), annonceFermee = 5)
        )
    }

    @Test
    fun `a jour, rien n'est annonce`() {
        assertEquals(
            EtatMiseAJour.AJour,
            etatMiseAJour(installee = 5, config = AppConfig(minVersionCode = 3, latestVersionCode = 5))
        )
    }

    // ── Le lien ───────────────────────────────────────────────────────────────

    @Test
    fun `seuls les liens web sont gardes`() {
        val etat = etatMiseAJour(
            installee = 2,
            config = AppConfig(minVersionCode = 3, downloadUrl = "intent://quelquechose")
        )
        assertEquals("", (etat as EtatMiseAJour.Requise).lien)
    }

    @Test
    fun `les espaces saisis autour du lien sont retires`() {
        val etat = etatMiseAJour(
            installee = 2,
            config = AppConfig(minVersionCode = 3, downloadUrl = "  $lien  ")
        )
        assertEquals(lien, (etat as EtatMiseAJour.Requise).lien)
    }
}
