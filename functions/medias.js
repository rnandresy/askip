/**
 * Askip — suppression des médias Cloudinary devenus orphelins.
 *
 * Jusqu'ici, effacer une rumeur effaçait son document Firestore et ses
 * sous-collections, mais laissait la photo, la vidéo ou la note vocale sur
 * Cloudinary — **et son URL restait publiquement accessible**. Sur une app qui
 * parle de gens réels, « j'ai supprimé mon post » doit vouloir dire quelque
 * chose. Accessoirement, le stockage ne faisait que monter, et l'audio de la
 * nouvelle actualité vocale consomme bien plus que les photos.
 *
 * Supprimer chez Cloudinary exige la clé secrète de l'API, qui n'a rien à
 * faire dans une app installée sur des téléphones. D'où ce fichier : le
 * secret vit ici, côté serveur, et la suppression suit le document.
 *
 * ── Avant de déployer ───────────────────────────────────────────────────────
 *
 *   firebase functions:secrets:set CLOUDINARY_API_KEY
 *   firebase functions:secrets:set CLOUDINARY_API_SECRET
 *
 * Les deux valeurs se lisent dans la console Cloudinary → Settings → API Keys.
 * Sans elles, le déploiement est refusé — c'est voulu : mieux vaut un déploi-
 * ement qui échoue qu'une fonction qui tourne sans pouvoir rien faire.
 *
 * ── Ça ne supprime rien tant que tu ne l'as pas décidé ──────────────────────
 *
 * `CLOUDINARY_SUPPRESSION_REELLE` vaut `false` dans `functions/.env`. Dans cet
 * état, la fonction **journalise ce qu'elle supprimerait sans rien supprimer**.
 * Une suppression est irréversible et rien de tout ceci n'a pu être essayé
 * ailleurs qu'ici : regarde les journaux quelques jours
 * (`firebase functions:log`), vérifie que les identifiants annoncés sont bien
 * ceux que tu attends, puis passe la valeur à `true` et redéploie.
 */

const { onDocumentDeleted } = require("firebase-functions/v2/firestore");
const { defineBoolean, defineSecret } = require("firebase-functions/params");
const { logger } = require("firebase-functions");
const crypto = require("node:crypto");
const { referenceCloudinary } = require("./cloudinary-url");

// Public, et déjà présent dans l'APK : ce n'en est pas moins le même compte.
const CLOUD_NAME = "di6bq2h1d";

const API_KEY = defineSecret("CLOUDINARY_API_KEY");
const API_SECRET = defineSecret("CLOUDINARY_API_SECRET");

const POUR_DE_VRAI = defineBoolean("CLOUDINARY_SUPPRESSION_REELLE", {
  default: false,
  description:
    "false = journalise sans supprimer. Passer à true une fois les journaux relus.",
});

// ── Supprimer ────────────────────────────────────────────────────────────────

async function detruire({ type, publicId }) {
  const timestamp = Math.floor(Date.now() / 1000);

  // Cloudinary signe en SHA-1 les paramètres triés, suivis du secret.
  const signature = crypto
    .createHash("sha1")
    .update(`public_id=${publicId}&timestamp=${timestamp}${API_SECRET.value()}`)
    .digest("hex");

  const reponse = await fetch(
    `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/${type}/destroy`,
    {
      method: "POST",
      body: new URLSearchParams({
        public_id: publicId,
        api_key: API_KEY.value(),
        timestamp: String(timestamp),
        signature,
      }),
    }
  );

  const resultat = await reponse.json().catch(() => ({}));

  if (resultat.result === "ok") {
    logger.info(`Supprimé : ${type}/${publicId}`);
    return;
  }

  // « not found » n'est pas une erreur : le fichier avait déjà disparu, ou la
  // suppression a été rejouée. On le dit sans lever d'exception, sinon la
  // fonction serait retentée indéfiniment pour rien.
  if (resultat.result === "not found") {
    logger.info(`Déjà absent : ${type}/${publicId}`);
    return;
  }

  logger.error(
    `Suppression refusée pour ${type}/${publicId}`,
    { statut: reponse.status, resultat }
  );
}

/**
 * Supprime les médias listés dans [champs] du document effacé.
 *
 * ⚠️ [champs] est une liste **blanche**, et elle doit le rester. Ne jamais
 * parcourir tous les champs d'un document à la recherche d'URL : une rumeur et
 * un commentaire portent `userPhotoUrl`, qui n'est pas leur média mais une
 * copie de l'avatar de leur auteur. Le supprimer avec la rumeur effacerait la
 * photo de profil de quelqu'un — partout, et pour de bon.
 */
async function purger(snapshot, champs, quoi) {
  if (!snapshot) return;

  const donnees = snapshot.data() || {};
  const references = champs
    .map((champ) => referenceCloudinary(donnees[champ]))
    .filter(Boolean)
    // Un média hébergé sur un autre compte Cloudinary que le nôtre : on ne
    // demande pas sa suppression, elle viserait un identifiant homonyme chez
    // nous. Ça ne devrait jamais arriver ; c'est justement pour ça qu'on le
    // dit plutôt que de l'ignorer en silence.
    .filter((reference) => {
      if (reference.cloud === CLOUD_NAME) return true;
      logger.warn(
        `Média ignoré, cloud étranger « ${reference.cloud} » : ${reference.publicId}`
      );
      return false;
    });

  if (!references.length) return;

  const liste = references.map((r) => `${r.type}/${r.publicId}`).join(", ");

  if (!POUR_DE_VRAI.value()) {
    logger.info(`[à blanc] ${quoi} — serait supprimé : ${liste}`);
    return;
  }

  logger.info(`${quoi} — suppression de : ${liste}`);
  for (const reference of references) {
    // En série et non en parallèle : un document ne porte que quelques
    // médias, et supprimer un compte en efface déjà des centaines à la file.
    // Inutile d'ajouter de la charge sur l'API.
    await detruire(reference);
  }
}

// ── Les déclencheurs ─────────────────────────────────────────────────────────
// Un par endroit où un média peut mourir. Passer par le document plutôt que
// par le code de l'app couvre tous les chemins d'un coup : suppression par son
// auteur, par l'administration, ou en cascade lors d'une suppression de compte.

const AVEC_SECRETS = { secrets: [API_KEY, API_SECRET] };

exports.purgeMediaRumeur = onDocumentDeleted(
  { document: "posts/{postId}", ...AVEC_SECRETS },
  (event) =>
    purger(
      event.data,
      ["imageUrl", "videoUrl", "audioUrl", "fileUrl"],
      `rumeur ${event.params.postId}`
    )
);

exports.purgeMediaCapsule = onDocumentDeleted(
  { document: "posts/{postId}/sealed/{capsuleId}", ...AVEC_SECRETS },
  (event) =>
    purger(event.data, ["imageUrl"], `capsule de ${event.params.postId}`)
);

exports.purgeMediaCommentaire = onDocumentDeleted(
  { document: "posts/{postId}/comments/{commentId}", ...AVEC_SECRETS },
  (event) =>
    purger(event.data, ["audioUrl"], `commentaire ${event.params.commentId}`)
);

exports.purgeMediaMessage = onDocumentDeleted(
  { document: "conversations/{convId}/messages/{messageId}", ...AVEC_SECRETS },
  (event) =>
    purger(event.data, ["mediaUrl"], `message ${event.params.messageId}`)
);

exports.purgeMediaMessageGroupe = onDocumentDeleted(
  { document: "groups/{groupId}/messages/{messageId}", ...AVEC_SECRETS },
  (event) =>
    purger(event.data, ["mediaUrl"], `message de groupe ${event.params.messageId}`)
);

/**
 * Profil supprimé : l'avatar et la bannière partent avec.
 *
 * Volontairement limité à la **suppression** du profil. Changer de photo
 * laisse elle aussi l'ancienne sur Cloudinary, mais la supprimer casserait les
 * `userPhotoUrl` recopiés sur les anciennes rumeurs et commentaires — le fil
 * les rafraîchit depuis le profil vivant, pas tous les écrans. Ce cas-là
 * demande d'être traité pour lui-même, pas ajouté ici en passant.
 */
exports.purgeMediaProfil = onDocumentDeleted(
  { document: "profiles/{uid}", ...AVEC_SECRETS },
  (event) =>
    purger(event.data, ["photoUrl", "coverUrl"], `profil ${event.params.uid}`)
);

