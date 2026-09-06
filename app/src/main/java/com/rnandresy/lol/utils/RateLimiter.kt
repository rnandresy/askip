package com.rnandresy.lol.utils

/**
 * Anti-spam côté client. Empêche les envois trop rapprochés
 * avant même de toucher Firestore — économise de la data
 * et évite d'atteindre les quotas.
 *
 * ⚠️ Complémentaire aux règles Firestore, pas un remplacement.
 */
object RateLimiter {

    private val lastAction = mutableMapOf<String, Long>()
    private val hourlyLog  = mutableMapOf<String, MutableList<Long>>()

    /**
     * @return null si l'action est autorisée, sinon un message d'erreur.
     */
    fun check(action: String, minIntervalMs: Long): String? {
        val now  = System.currentTimeMillis()
        val last = lastAction[action] ?: 0L
        val diff = now - last

        if (diff < minIntervalMs) {
            val remaining = ((minIntervalMs - diff) / 1000).coerceAtLeast(1)
            return "Attendez $remaining seconde(s) avant de recommencer."
        }
        lastAction[action] = now
        return null
    }

    /** Limite horaire (ex. max 15 posts / heure). */
    fun checkHourly(action: String, maxPerHour: Int): String? {
        val now    = System.currentTimeMillis()
        val hourMs = 60 * 60 * 1000L
        val log    = hourlyLog.getOrPut(action) { mutableListOf() }

        log.removeAll { now - it > hourMs }

        if (log.size >= maxPerHour) {
            val oldest    = log.minOrNull() ?: now
            val remaining = ((hourMs - (now - oldest)) / 60000).coerceAtLeast(1)
            return "Limite atteinte. Réessayez dans $remaining minute(s)."
        }
        log.add(now)
        return null
    }

    fun reset(action: String) {
        lastAction.remove(action)
        hourlyLog.remove(action)
    }

    fun resetAll() {
        lastAction.clear()
        hourlyLog.clear()
    }
}