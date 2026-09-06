package com.rnandresy.lol

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.rnandresy.lol.utils.NotificationHelper

class AskipApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        // ── Cache offline persistant ──────────────────────────────────────────
        // L'app reste consultable sans connexion et consomme
        // beaucoup moins de data sur les écrans déjà visités.
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings {
            setLocalCacheSettings(
                persistentCacheSettings {
                    // 100 Mo de cache local — largement suffisant
                    setSizeBytes(100L * 1024 * 1024)
                }
            )
        }

        NotificationHelper(this).createChannels()
    }
}