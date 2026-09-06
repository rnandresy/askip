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
data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val lastTimestamp: Long = 0L,
    val unreadCounts: Map<String, Long> = emptyMap()
)