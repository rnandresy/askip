package com.rnandresy.lol.utils

const val ADMIN_UID = "00cGAlooriRpttoV2Jo8zLbTS7v2"
const val ADMIN_BADGE_NAME = "admin" // nom réservé, personne ne peut créer ce badge

val ENI_CLASSES = listOf(
    "CISCO 1ère année",
    "CISCO 2ème année",
    "CISCO 3ème année",
    "Développement 1A",
    "Développement 1B",
    "Développement 2A",
    "Développement 2B",
    "Réseau & Télécoms 1",
    "Réseau & Télécoms 2",
    "Multimédia 1",
    "Multimédia 2",
    "Gestion Comptable 1",
    "Gestion Comptable 2",
    "Secrétariat Bureautique 1",
    "Secrétariat Bureautique 2",
    "Commerce International 1",
    "Commerce International 2"
)

fun isAdmin(userId: String): Boolean = userId == ADMIN_UID