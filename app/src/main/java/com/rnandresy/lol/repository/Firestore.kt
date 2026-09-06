package com.rnandresy.lol.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Socle partagé par tous les repositories.
 *
 * Une seule instance Firestore, quelques helpers pour les écritures en lot :
 * Firestore plafonne un batch à 500 opérations, donc tout passe par [commitInChunks].
 */
internal val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
internal val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

/** Taille de lot volontairement sous la limite de 500 imposée par Firestore. */
internal const val BATCH_SIZE = 400

/** Applique [op] à chaque document, par paquets de [BATCH_SIZE]. */
internal suspend fun List<DocumentSnapshot>.commitInChunks(
    op: (com.google.firebase.firestore.WriteBatch, DocumentSnapshot) -> Unit
) {
    if (isEmpty()) return
    chunked(BATCH_SIZE).forEach { chunk ->
        val batch = db.batch()
        chunk.forEach { op(batch, it) }
        batch.commit().await()
    }
}

/** Supprime tous les documents d'une requête, par paquets. */
internal suspend fun Query.deleteAll() {
    runCatching {
        get().await().documents.commitInChunks { batch, doc -> batch.delete(doc.reference) }
    }
}
