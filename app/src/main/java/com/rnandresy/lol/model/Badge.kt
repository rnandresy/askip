package com.rnandresy.lol.model

data class Badge(
    val id: String = "",
    val name: String = "",          // stocké en lowercase
    val displayName: String = "",   // nom affiché avec casse originale
    val colorHex: String = "#7C4DFF",
    val createdBy: String = "",
    val createdAt: Long = 0L
)