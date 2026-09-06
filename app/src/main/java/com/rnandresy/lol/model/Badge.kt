package com.rnandresy.lol.model

import androidx.compose.runtime.Immutable

/**
 * Ces modèles sont des valeurs : une fois désérialisés par Firestore, plus
 * personne ne les modifie — l'app passe systématiquement par `copy()`. Les
 * quelques `var` ne sont là que pour `@set:PropertyName`, dont Firestore a
 * besoin pour écrire le champ.
 *
 * Sans le dire à Compose, une classe portant un `var` ou une `List` est jugée
 * *instable* : le compilateur ne peut plus sauter une carte dont les données
 * n'ont pas bougé, et le moindre changement d'état ailleurs redessine tout le
 * fil. `@Immutable` est la promesse qui débloque ça — et ici c'en est une
 * vraie.
 */
@Immutable
data class Badge(
    val id: String = "",
    val name: String = "",           // miniscule unique
    val displayName: String = "",
    val colorHex: String = "#7C4DFF",
    val createdBy: String = "",
    val createdAt: Long = 0L
)