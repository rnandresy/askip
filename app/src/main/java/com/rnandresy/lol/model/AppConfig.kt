package com.rnandresy.lol.model

import androidx.compose.runtime.Immutable

/**
 * Le document Firestore `config/app`, écrit à la main dans la console.
 *
 * Les valeurs par défaut sont celles d'un document absent : zéro partout, donc
 * aucune version n'est jamais trop vieille. C'est ce qui rend le mécanisme
 * inoffensif tant que personne ne l'a configuré.
 *
 * Les deux numéros sont des `Long` parce que Firestore range tous ses nombres
 * entiers ainsi. Saisis dans la console comme **texte** plutôt que comme
 * nombre, ils font échouer la lecture — et `AppConfigRepository` traite un
 * échec comme « ne bloquer personne ».
 */
@Immutable
data class AppConfig(
    /** En dessous de ce `versionCode`, l'app refuse de s'ouvrir. */
    val minVersionCode: Long = 0L,
    /** En dessous de celui-ci, elle propose la mise à jour sans rien imposer. */
    val latestVersionCode: Long = 0L,
    /** Où télécharger la nouvelle version. Vide : demander à l'administrateur. */
    val downloadUrl: String = "",
    /** Texte facultatif qui remplace celui de l'app. */
    val message: String = ""
)
