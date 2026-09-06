package com.rnandresy.lol.utils

/**
 * Demander à Cloudinary l'image à la taille où elle sera vue.
 *
 * Les envois sont déjà compressés à [MAX_IMAGE_DIMENSION] px de côté. Mais un
 * avatar s'affiche à 44 dp — environ 130 px — et téléchargeait jusqu'ici ces
 * 1440 px : cent vingt fois les pixels nécessaires, pour une vignette ronde.
 * Une liste de conversations en montre une dizaine d'un coup.
 *
 * Sur un campus en données mobiles, c'est le poste le plus lourd de l'app.
 * Cloudinary sait redimensionner à la livraison, il suffit de le lui demander
 * dans l'URL — rien à réécrire en base, rien à migrer.
 */

private const val MARQUE_IMAGE = "/image/upload/"

/**
 * Insère une transformation de largeur dans une URL Cloudinary.
 *
 * Trois réglages, et les trois comptent :
 *  - `c_limit` réduit sans jamais agrandir : une image plus petite que demandé
 *    est servie telle quelle, au lieu d'être étirée et de peser plus lourd.
 *  - `f_auto` livre du WebP ou de l'AVIF quand le téléphone les accepte.
 *  - `q_auto` laisse Cloudinary choisir la compression selon le contenu.
 *
 * Toute URL qui n'est pas une image Cloudinary — un lien externe, une URI
 * locale pas encore envoyée — ressort inchangée. C'est volontaire : cette
 * fonction améliore quand elle peut et ne casse jamais rien.
 */
fun imageAtWidth(url: String, widthPx: Int): String {
    if (url.isBlank() || widthPx <= 0) return url
    val debut = url.indexOf(MARQUE_IMAGE)
    if (debut < 0) return url

    val apres = debut + MARQUE_IMAGE.length
    // Déjà transformée : on n'empile pas une seconde consigne par-dessus.
    if (url.startsWith("w_", apres) || url.startsWith("c_", apres)) return url

    // Arrondi au palier de 100 px supérieur : deux avatars de 42 et 44 dp
    // partagent alors la même URL, donc le même fichier en cache.
    val palier = ((widthPx + 99) / 100) * 100
    return url.substring(0, apres) + "w_$palier,c_limit,f_auto,q_auto/" + url.substring(apres)
}
