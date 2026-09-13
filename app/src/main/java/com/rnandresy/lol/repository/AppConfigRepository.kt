package com.rnandresy.lol.repository

import android.util.Log
import com.rnandresy.lol.model.AppConfig
import com.rnandresy.lol.utils.COL_CONFIG
import com.rnandresy.lol.utils.DOC_APP_CONFIG
import kotlinx.coroutines.tasks.await

/**
 * Lecture de `config/app`.
 *
 * Deux réponses bien distinctes, et c'est tout l'intérêt de ce fichier :
 *
 *  · document absent → `AppConfig()` par défaut, soit « personne n'est trop
 *    vieux ». C'est un état voulu : le mécanisme n'est simplement pas armé, ou
 *    l'administrateur a supprimé le document pour tout débloquer.
 *  · échec (réseau coupé sans cache, règles non déployées, champ saisi comme
 *    texte) → `null`, que le ViewModel lit comme « ne rien changer ».
 *
 * Confondre les deux ferait l'un de deux dégâts : ne jamais pouvoir débloquer
 * en supprimant le document, ou bloquer quelqu'un sur une simple coupure.
 */
class AppConfigRepository {

    suspend fun lire(): AppConfig? = runCatching {
        val snap = db.collection(COL_CONFIG).document(DOC_APP_CONFIG).get().await()
        if (!snap.exists()) AppConfig() else snap.toObject(AppConfig::class.java)
    }.onFailure {
        // Visible dans `adb logcat -s AskipMaj`. Silencieux à l'écran : un
        // échec ici ne doit rien empêcher.
        Log.w("AskipMaj", "Configuration de l'app illisible : ${it.message}")
    }.getOrNull()
}
