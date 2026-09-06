package com.rnandresy.lol.utils

/**
 * État générique d'un écran. Évite les combinaisons impossibles
 * (isLoading = true ET error != null ET data != null).
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val message: String = "") : UiState<Nothing>
    data class Error(val message: String, val retryable: Boolean = true) : UiState<Nothing>
}

/** Résultat d'une action ponctuelle (envoi, suppression, upload). */
sealed interface ActionState {
    data object Idle : ActionState
    data class Loading(val progress: Int = -1) : ActionState
    data class Success(val message: String = "") : ActionState
    data class Error(val message: String) : ActionState
}

/** Événement à consommer une seule fois (snackbar, navigation). */
class OneShot<T>(private val value: T) {
    private var consumed = false
    fun get(): T? = if (consumed) null else { consumed = true; value }
    fun peek(): T = value
}

/** Traduit une exception Firebase en message lisible en français. */
fun Throwable.toFrenchMessage(): String {
    val msg = message?.lowercase() ?: return "Une erreur est survenue"
    return when {
        "network"        in msg || "unavailable" in msg ->
            "Pas de connexion. Vérifiez votre réseau."
        "permission"     in msg || "denied"      in msg ->
            "Vous n'avez pas la permission d'effectuer cette action."
        "already in use" in msg || "already"     in msg ->
            "Cet email est déjà utilisé."
        "badly formatted" in msg || "invalid email" in msg ->
            "Format d'email invalide."
        "weak password"  in msg ->
            "Mot de passe trop faible (6 caractères minimum)."
        "no user"        in msg || "not found"   in msg ->
            "Compte introuvable."
        "wrong password" in msg || "credential"  in msg ->
            "Email ou mot de passe incorrect."
        "too many"       in msg ->
            "Trop de tentatives. Réessayez dans quelques minutes."
        "quota"          in msg ->
            "Limite atteinte. Réessayez plus tard."
        "cancelled"      in msg ->
            "Opération annulée."
        "deadline"       in msg || "timeout"     in msg ->
            "Délai dépassé. Connexion trop lente."
        else -> message ?: "Une erreur est survenue"
    }
}