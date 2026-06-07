plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false // Utilise maintenant la version 2.0.21 du TOML
    id("com.google.gms.google-services") version "4.4.1" apply false
    alias(libs.plugins.kotlin.compose) apply false // Ajout indispensable pour Kotlin 2.0+
}