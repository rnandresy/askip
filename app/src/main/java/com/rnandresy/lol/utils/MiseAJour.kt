package com.rnandresy.lol.utils

import com.rnandresy.lol.model.AppConfig

/**
 * Ce que l'app doit faire de sa propre version.
 *
 * Isolé du ViewModel et sans Android : c'est la seule décision du mécanisme
 * qui puisse enfermer tout le campus dehors, elle mérite d'être vérifiée par
 * des tests (`MiseAJourTest`).
 */
sealed class EtatMiseAJour {

    /** Rien à signaler. Aussi l'état de tout échec : on ne bloque pas à l'aveugle. */
    data object AJour : EtatMiseAJour()

    /** Une version plus récente existe ; celle-ci reste utilisable. */
    data class Disponible(
        val versionCode: Long,
        val lien: String,
        val message: String
    ) : EtatMiseAJour()

    /** Cette version est trop vieille : l'app ne s'ouvre plus. */
    data class Requise(
        val versionInstallee: Long,
        val versionMinimale: Long,
        val lien: String,
        val message: String
    ) : EtatMiseAJour()
}

/**
 * Compare la version installée à la configuration.
 *
 * - [installee] à zéro ou moins signifie « illisible » : jamais de blocage.
 * - La version minimale elle-même est acceptée : `minVersionCode = 3` bloque
 *   la 2, pas la 3.
 * - [annonceFermee] est le numéro d'une annonce déjà écartée par la personne.
 *   Elle ne revient pas pour ce numéro, mais revient pour une version plus
 *   récente. Un blocage, lui, ne s'écarte pas.
 * - Le lien n'est gardé que s'il est en `http` ou `https`. Il est saisi à la
 *   main dans la console, et c'est une intention Android qui l'ouvre : autant
 *   ne pas ouvrir n'importe quel schéma.
 */
fun etatMiseAJour(
    installee: Long,
    config: AppConfig?,
    annonceFermee: Long = 0L
): EtatMiseAJour {
    if (config == null || installee <= 0L) return EtatMiseAJour.AJour

    val lien = config.downloadUrl.trim()
        .takeIf { it.startsWith("https://") || it.startsWith("http://") }
        .orEmpty()
    val message = config.message.trim()

    if (config.minVersionCode > installee) {
        return EtatMiseAJour.Requise(installee, config.minVersionCode, lien, message)
    }
    if (config.latestVersionCode > installee && config.latestVersionCode > annonceFermee) {
        return EtatMiseAJour.Disponible(config.latestVersionCode, lien, message)
    }
    return EtatMiseAJour.AJour
}
