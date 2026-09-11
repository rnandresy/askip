/**
 * Banc d'essai de l'analyse des URL Cloudinary.
 *
 *   node functions/test-medias.mjs
 *
 * C'est la seule partie risquée de la purge des médias : une erreur ici fait
 * demander la suppression du mauvais fichier, et une suppression ne se défait
 * pas. Le module analysé n'a aucune dépendance Firebase, donc ces cas tournent
 * avec un simple `node`, sans déployer et sans `npm install`.
 */

import { createRequire } from "node:module";
import assert from "node:assert/strict";

const require = createRequire(import.meta.url);
const { referenceCloudinary } = require("./cloudinary-url.js");

const CLOUD = "di6bq2h1d";
const BASE = `https://res.cloudinary.com/${CLOUD}`;

let joues = 0;
const echecs = [];

function cas(nom, fn) {
  joues++;
  try {
    fn();
    console.log(`    OK    ${nom}`);
  } catch (e) {
    echecs.push({ nom, e });
    console.log(`    ECHEC ${nom}`);
  }
}

const attendu = (url, valeur) =>
  assert.deepEqual(referenceCloudinary(url), valeur);

console.log("\n── Ce que l'app envoie vraiment ─────────────────────────────\n");

cas("une photo de rumeur", () =>
  attendu(`${BASE}/image/upload/v1712345678/askip/img_1712345678.jpg`, {
    cloud: CLOUD,
    type: "image",
    publicId: "askip/img_1712345678",
  })
);

cas("une vidéo", () =>
  attendu(`${BASE}/video/upload/v1712345678/askip/clip.mp4`, {
    cloud: CLOUD,
    type: "video",
    publicId: "askip/clip",
  })
);

cas("une note vocale part en `video`, pas en `raw`", () =>
  // `CloudinaryUploader.uploadAudio` vise volontairement le point d'entrée
  // vidéo. Demander la suppression en `raw` répondrait « not found ».
  attendu(`${BASE}/video/upload/v1712345678/askip/voice_1712345678.m4a`, {
    cloud: CLOUD,
    type: "video",
    publicId: "askip/voice_1712345678",
  })
);

cas("une pièce jointe garde son extension dans l'identifiant", () =>
  // Le piège classique de l'API : en `raw`, l'extension fait partie du
  // `public_id`. La retirer ferait échouer toutes les suppressions de fichier.
  attendu(`${BASE}/raw/upload/v1712345678/askip/rapport.pdf`, {
    cloud: CLOUD,
    type: "raw",
    publicId: "askip/rapport.pdf",
  })
);

console.log("\n── Ce que l'affichage ajoute par-dessus ─────────────────────\n");

cas("les transformations de livraison ne font pas partie de l'identifiant", () =>
  // Exactement ce qu'insère `utils/ImageUrl.kt` pour les vignettes.
  attendu(
    `${BASE}/image/upload/w_200,c_limit,f_auto,q_auto/v1712345678/askip/img_1.jpg`,
    { cloud: CLOUD, type: "image", publicId: "askip/img_1" }
  )
);

cas("sans numéro de version", () =>
  attendu(`${BASE}/image/upload/askip/img_1.jpg`, {
    cloud: CLOUD,
    type: "image",
    publicId: "askip/img_1",
  })
);

cas("dossiers imbriqués", () =>
  attendu(`${BASE}/image/upload/v1/askip/2026/09/img_1.jpg`, {
    cloud: CLOUD,
    type: "image",
    publicId: "askip/2026/09/img_1",
  })
);

cas("un point dans le nom ne coupe que l'extension", () =>
  attendu(`${BASE}/image/upload/v1/askip/mon.fichier.a.moi.jpg`, {
    cloud: CLOUD,
    type: "image",
    publicId: "askip/mon.fichier.a.moi",
  })
);

cas("une requête collée à la fin est ignorée", () =>
  attendu(`${BASE}/image/upload/v1/askip/img_1.jpg?_a=xyz`, {
    cloud: CLOUD,
    type: "image",
    publicId: "askip/img_1",
  })
);

console.log("\n── Ce qu'il ne faut surtout pas supprimer ───────────────────\n");

cas("une URL vide, nulle, ou qui n'est pas une chaîne", () => {
  attendu("", null);
  attendu(null, null);
  attendu(undefined, null);
  attendu(42, null);
  attendu({ url: "x" }, null);
});

cas("une URI locale pas encore envoyée", () =>
  attendu("content://media/external/images/media/1234", null)
);

cas("un lien externe", () =>
  attendu("https://example.com/photo.jpg", null)
);

cas("un hôte qui se fait passer pour Cloudinary", () =>
  // Sans ancre au début de l'URL, celle-ci passerait, et on demanderait la
  // suppression d'un identifiant choisi par quelqu'un d'autre.
  attendu(
    "https://ailleurs.example/res.cloudinary.com/x/image/upload/v1/secret.jpg",
    null
  )
);

cas("un type de ressource inconnu", () =>
  attendu(`${BASE}/sprite/upload/v1/askip/img_1.jpg`, null)
);

cas("un autre compte Cloudinary est reconnu mais identifié comme tel", () => {
  const r = referenceCloudinary(
    "https://res.cloudinary.com/quelquun-dautre/image/upload/v1/img_1.jpg"
  );
  assert.equal(r.cloud, "quelquun-dautre");
  assert.notEqual(r.cloud, CLOUD, "c'est `purger` qui doit alors l'écarter");
});

console.log(`\n${"─".repeat(62)}`);
if (echecs.length === 0) {
  console.log(`  ${joues} / ${joues} cas passent. L'analyse des URL tient.\n`);
} else {
  console.log(`  ${joues - echecs.length} / ${joues} cas passent. ${echecs.length} en échec :\n`);
  for (const { nom, e } of echecs) {
    console.log(`  · ${nom}`);
    console.log(`      ${String(e.message).split("\n")[0].slice(0, 200)}`);
  }
  console.log("");
}

process.exit(echecs.length === 0 ? 0 : 1);
