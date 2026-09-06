package com.rnandresy.lol.repository

import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Authentification seule : connexion, inscription, compte.
 * Tout ce qui touche au contenu de l'utilisateur vit dans [ProfileRepository].
 */
class AuthRepository {

    val isLoggedIn: Boolean get() = auth.currentUser != null
    val currentUid: String get() = auth.currentUser?.uid ?: ""
    val currentEmail: String get() = auth.currentUser?.email ?: ""

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /**
     * Crée le compte Auth **et** le profil Firestore associé.
     * Sans profil, l'utilisateur serait invisible partout dans l'app.
     */
    suspend fun register(email: String, password: String, username: String): String {
        val uid = auth.createUserWithEmailAndPassword(email, password).await()
            .user?.uid ?: error("UID null après inscription")
        ProfileRepository().createDefaultProfile(uid, username)
        return uid
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun resetPassword(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun updateEmail(newEmail: String, password: String) {
        val user = auth.currentUser ?: error("Non connecté")
        user.reauthenticate(
            EmailAuthProvider.getCredential(user.email ?: "", password)
        ).await()
        user.verifyBeforeUpdateEmail(newEmail).await()
    }

    suspend fun updatePassword(current: String, newPassword: String) {
        val user = auth.currentUser ?: error("Non connecté")
        user.reauthenticate(
            EmailAuthProvider.getCredential(user.email ?: "", current)
        ).await()
        user.updatePassword(newPassword).await()
    }

    /**
     * Vérifie le mot de passe sans rien détruire.
     *
     * À appeler **avant** d'effacer le contenu : sinon un mot de passe erroné
     * laisserait la personne sans ses posts et avec son compte toujours actif.
     */
    suspend fun reauthenticate(password: String) {
        val user = auth.currentUser ?: error("Non connecté")
        user.reauthenticate(
            EmailAuthProvider.getCredential(user.email ?: "", password)
        ).await()
    }

    /**
     * Supprime le compte Auth. Le contenu Firestore doit avoir été effacé
     * **avant** (voir [ProfileRepository.deleteAllUserContent]) : une fois le
     * compte supprimé, les règles Firestore refusent toute écriture.
     */
    suspend fun deleteAccount(password: String) {
        reauthenticate(password)
        auth.currentUser?.delete()?.await() ?: error("Non connecté")
    }
}
