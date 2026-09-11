/**
 * Lire un identifiant Cloudinary dans une URL.
 *
 * Isolé dans son propre fichier, **sans aucune dépendance Firebase**, pour une
 * raison précise : c'est la seule partie risquée de la purge des médias. Une
 * erreur d'analyse ici, et on demande la suppression du mauvais fichier. Seul,
 * ce fichier se teste avec un simple `node`, sans déployer quoi que ce soit et
 * sans installer les dépendances des fonctions — voir `test-medias.mjs`.
 */

// Ancré sur le début de l'URL, et pas seulement « contient ». Sans l'ancre,
// `https://ailleurs.example/res.cloudinary.com/x/image/upload/v1/a.jpg`
// passerait pour une URL Cloudinary, et on demanderait la suppression d'un
// identifiant soufflé par quelqu'un d'autre. Le nom du cloud est capturé pour
// que l'appelant vérifie que c'est bien le sien.
const MARQUE =
  /^https:\/\/res\.cloudinary\.com\/([^/]+)\/(image|video|raw)\/upload\/(.+)$/;

// `w_400,c_limit,f_auto,q_auto` — ce que `utils/ImageUrl.kt` insère à la
// livraison. Ces segments ne font pas partie de l'identifiant.
const TRANSFORMATION = /^[a-z]{1,3}_[^/,]+(,[a-z]{1,3}_[^/,]+)*$/;
const VERSION = /^v\d+$/;

/**
 * Extrait le `public_id` et le type de ressource d'une URL Cloudinary.
 *
 * Rend `null` pour tout le reste — lien externe, URI locale pas encore
 * envoyée, chaîne vide, valeur d'un autre type. Cette fonction ne devine
 * jamais : ce qu'elle ne reconnaît pas formellement n'est pas supprimé.
 *
 * Le type compte autant que l'identifiant : l'app envoie les images en
 * `image`, **les vidéos et les notes vocales en `video`** (voir
 * `CloudinaryUploader.uploadAudio`, qui vise volontairement le point d'entrée
 * vidéo), et les pièces jointes en `raw`. Se tromper de type fait répondre
 * « not found » à Cloudinary, sans rien supprimer.
 */
function referenceCloudinary(url) {
  if (typeof url !== "string" || !url) return null;

  const correspondance = url.match(MARQUE);
  if (!correspondance) return null;

  const cloud = correspondance[1];
  const type = correspondance[2];
  const reste = correspondance[3].split("?")[0].split("#")[0];

  const segments = reste.split("/").filter(Boolean);
  while (
    segments.length > 1 &&
    (TRANSFORMATION.test(segments[0]) || VERSION.test(segments[0]))
  ) {
    segments.shift();
  }
  if (!segments.length) return null;

  let publicId = segments.join("/");

  // Une ressource `raw` garde son extension dans son identifiant ; une image
  // et une vidéo, non. C'est le piège classique de cette API.
  if (type !== "raw") publicId = publicId.replace(/\.[^./]+$/, "");

  return publicId ? { cloud, type, publicId } : null;
}

module.exports = { referenceCloudinary };
