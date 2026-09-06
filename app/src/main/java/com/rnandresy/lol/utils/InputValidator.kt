package com.rnandresy.lol.utils

/**
 * Validation et nettoyage des entrées utilisateur.
 * Retourne null si valide, sinon le message d'erreur.
 */
object InputValidator {

    private val USERNAME_REGEX = Regex("^[a-zA-Z0-9_.]+$")
    private val EMAIL_REGEX    = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun username(value: String): String? {
        val v = value.trim()
        return when {
            v.isBlank()                      -> "Le pseudo est requis"
            v.length < MIN_USERNAME_LENGTH   -> "Minimum $MIN_USERNAME_LENGTH caractères"
            v.length > MAX_USERNAME_LENGTH   -> "Maximum $MAX_USERNAME_LENGTH caractères"
            !USERNAME_REGEX.matches(v)       -> "Lettres, chiffres, _ et . uniquement"
            v.startsWith(".") || v.endsWith(".") -> "Ne peut pas commencer/finir par un point"
            else -> null
        }
    }

    fun email(value: String): String? {
        val v = value.trim()
        return when {
            v.isBlank()               -> "L'email est requis"
            !EMAIL_REGEX.matches(v)   -> "Format d'email invalide"
            else -> null
        }
    }

    fun password(value: String): String? = when {
        value.isBlank()      -> "Le mot de passe est requis"
        value.length < 6     -> "Minimum 6 caractères"
        value.length > 128   -> "Mot de passe trop long"
        else -> null
    }

    fun postContent(value: String): String? {
        val v = value.trim()
        return when {
            v.isBlank()                  -> "Le contenu ne peut pas être vide"
            v.length > MAX_POST_LENGTH   -> "Maximum $MAX_POST_LENGTH caractères"
            else -> null
        }
    }

    fun comment(value: String): String? {
        val v = value.trim()
        return when {
            v.isBlank()                     -> "Le commentaire ne peut pas être vide"
            v.length > MAX_COMMENT_LENGTH   -> "Maximum $MAX_COMMENT_LENGTH caractères"
            else -> null
        }
    }

    /** Supprime les caractères de contrôle et normalise les espaces. */
    fun sanitize(value: String): String = value
        .replace(Regex("[\\p{Cntrl}&&[^\n\r\t]]"), "")
        .replace(Regex("\n{4,}"), "\n\n\n")
        .replace(Regex("[ \t]{3,}"), "  ")
        .trim()
}