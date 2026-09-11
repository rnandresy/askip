# Ce qu'il reste à faire côté Firebase et Cloudinary

Tout ce qui suit se passe dans des consoles web : je n'y ai pas accès depuis
Android Studio, donc c'est à toi. J'ai préparé tout ce qui pouvait l'être dans
le dépôt.

Projet Firebase : **`kaiza-ac130`** · Cloud Cloudinary : **`di6bq2h1d`**

---

## 1. Firebase — à faire dans l'ordre

### 1.1 Vérifier que la base Firestore existe

Console → **Firestore Database**. Si l'écran propose « Créer une base de
données », c'est qu'elle n'existe pas encore :

- Mode : **production** (les règles du dépôt prennent le relais).
- Emplacement : le plus proche de Madagascar est `europe-west1` ou
  `asia-south1`. **Ce choix est définitif**, il ne se change plus après.

### 1.2 Activer la connexion par email

Console → **Authentication** → *Sign-in method* → activer **E-mail/Mot de passe**.

Sans ça, `login()` et `register()` échouent avec « operation not allowed ».
C'est le seul fournisseur utilisé par l'app.

### 1.3 Déployer les règles et les index

J'ai ajouté `firebase.json` et `.firebaserc` : la commande sait maintenant quoi
envoyer et vers quel projet.

```bash
firebase login
firebase deploy --only firestore:rules,firestore:indexes
```

> ⚠️ **À faire avant que quiconque parie.** L'écoute des paris passe par une
> requête de groupe de collections ; sans son index, elle échoue **en silence**
> et les tickets de pari n'apparaissent jamais. Les index mettent quelques
> minutes à se construire — la console affiche « Création… » puis « Activé ».

### 1.4 Confirmer l'UID admin

L'UID actuellement en place est `ckCTisQMWKWbnoElnUjO2x4vKxy2`, repris de ta
version précédente. Vérifie que c'est bien le tien :
console → **Authentication → Users**, colonne *User UID*.

Il doit être identique aux **deux** endroits, sinon l'app t'affichera comme
admin mais le serveur refusera tes actions :

| Fichier | Ligne |
| --- | --- |
| `app/src/main/java/com/rnandresy/lol/utils/AdminConfig.kt` | `const val ADMIN_UID` |
| `firestore.rules` | dans `function isAdmin()` |

Après modification de `firestore.rules`, redéployer (§1.3).

### 1.5 Tester les règles avant de les lâcher en production

Console → **Firestore → Règles → Terrain de jeu**. Le minimum à vérifier :

| Test | Attendu |
| --- | --- |
| Lire `posts/xxx` non authentifié | **refusé** |
| Créer un post avec `userId` ≠ le sien | **refusé** |
| Lire `posts/xxx/sealed/payload` avant l'heure | **refusé** |
| Lire cette même capsule après l'heure | autorisé |
| Écrire dans `reports` puis le relire sans être admin | écriture OK, lecture **refusée** |

---

## 2. Ce que je n'ai pas pu faire — et qui manque vraiment

### 2.1 Les notifications push — la fonction est écrite, reste à la déployer 🟢

Le maillon manquait : l'app enregistrait bien le jeton `fcmToken` et savait
recevoir un message, mais **personne ne les envoyait**. C'est maintenant le rôle
de `functions/index.js`.

Elle écoute les créations dans `notifications` et envoie le push au bon
appareil. Elle gère aussi ce qu'on oublie toujours : les jetons périmés (app
désinstallée) sont effacés du profil, les comptes bannis sont ignorés, les
messages d'une même conversation sont regroupés, et les notifications
obligatoires (annonces d'admin, `@everyone`) passent outre les préférences —
exactement comme le fait déjà l'app en local.

#### Déploiement

**1. Passer au plan Blaze.** Console → ⚙ → *Utilisation et facturation* →
*Modifier le forfait*. Une carte est demandée, mais le palier gratuit reste
offert : 2 millions d'appels de fonction par mois, très au-delà d'un campus.

**2. Aligner la région.** Ouvre `functions/index.js`, ligne ~28 :

```js
const REGION = "europe-west1";
```

Elle doit correspondre à l'emplacement de ta base Firestore (console →
Firestore → ⚙ → *Emplacement de la base de données*). Si les deux diffèrent, le
déploiement est refusé — c'est la seule erreur vraiment courante ici.

**3. Installer et déployer.**

```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

Le premier déploiement demande d'activer trois API Google (Cloud Build,
Artifact Registry, Eventarc) : réponds oui. Compte 3 à 5 minutes.

**4. Vérifier.** Depuis deux téléphones : ferme complètement l'app sur le
premier, envoie-lui un message depuis le second. La notification doit arriver.

En cas de silence :

```bash
firebase functions:log --only sendPushNotification
```

#### Les interrupteurs des réglages

Les trois interrupteurs (*Messages*, *Mentions*, *Nouveaux posts*) étaient
stockés **uniquement sur le téléphone**. Un serveur ne peut pas les lire : ils
n'auraient donc rien coupé du tout une fois l'app fermée.

L'app les recopie maintenant sur le profil à chaque changement, et à chaque
connexion pour les comptes existants. Rien à faire de ton côté.

#### Coût

Un post notifie tout le campus, donc **un appel de fonction par membre et par
post**. À 100 membres et 50 posts par jour : 5 000 appels/jour, soit 150 000 par
mois — pour un palier gratuit à 2 millions. Large.

### 2.2 Le coût des notifications à tout le campus 🟠

À chaque nouvelle rumeur, l'app écrit **une notification par utilisateur**.

```
100 membres × 50 rumeurs/jour = 5 000 écritures/jour
```

Le palier gratuit (Spark) plafonne à **20 000 écritures par jour**. Ça tient
jusqu'à environ 200 rumeurs par jour à 100 membres — au-delà, l'app cesse
d'écrire jusqu'au lendemain.

Si tu approches de cette limite, la première chose à couper est la notification
« nouveau post » envoyée à tout le monde (`notifyNewPost`) : les mentions et les
messages, eux, sont ciblés et coûtent bien moins.

Surveille dans **Firestore → Utilisation**.

---

## 3. Cloudinary

### 3.1 Vérifier le preset

Console Cloudinary → **Settings → Upload → Upload presets**.

Il faut un preset nommé exactement **`postaskip`**, en mode **Unsigned**.

C'est indispensable : l'app envoie uniquement `upload_preset` et le fichier,
sans signature. Si le preset est en *Signed*, tous les envois échouent avec
« Upload preset must be whitelisted for unsigned uploads ».

### 3.2 Vérifier les types acceptés

L'app envoie sur trois points d'entrée différents :

| Contenu | Point d'entrée Cloudinary |
| --- | --- |
| Photos | `/image/upload` |
| Vidéos | `/video/upload` |
| **Notes vocales** | `/video/upload` ← l'audio est un « video » chez Cloudinary |
| Fichiers joints | `/raw/upload` |

Si le preset restreint les formats (*Allowed formats*), assure-toi que `m4a`
passe — sinon **toute l'actualité vocale est morte**, ainsi que les vocaux du
chat.

### 3.3 Garde-fous recommandés dans le preset

Un preset non signé est public : quelqu'un qui décompile l'APK peut envoyer ce
qu'il veut sur ton compte. Ça ne se contourne pas sans serveur, mais ça se
limite :

- **Folder** : `askip/` — pour tout retrouver et tout purger d'un coup.
- **Max file size** : ~25 Mo, la même limite que l'app applique déjà.
- **Allowed formats** : `jpg,png,webp,mp4,m4a,pdf` plutôt que « tout ».
- **Unique filename** : activé.

### 3.4 La purge des médias — écrite, à armer en deux temps 🟢

Quand quelqu'un supprimait une rumeur ou son compte, l'app effaçait le
document Firestore — mais **le fichier restait sur Cloudinary, et son URL
restait publiquement accessible**. Sur une app qui parle de gens réels,
« j'ai supprimé mon post » doit vouloir dire quelque chose. Accessoirement le
stockage ne faisait que monter, et **l'audio et la vidéo consomment bien plus
que les photos** — c'est le premier plafond que tu atteindras avec l'actualité
vocale.

C'est fait, dans `functions/medias.js` : six déclencheurs, un par endroit où un
média peut mourir (rumeur, capsule, commentaire, message, message de groupe,
profil). Passer par la suppression du **document** plutôt que par le code de
l'app couvre tous les chemins d'un coup — suppression par son auteur, par
l'administration, ou en cascade lors d'une suppression de compte.

**Étape 1 — les clés.** Une suppression exige la clé secrète de l'API, qui n'a
rien à faire dans une app installée sur des téléphones. Elle vit donc côté
serveur :

```bash
firebase functions:secrets:set CLOUDINARY_API_KEY
firebase functions:secrets:set CLOUDINARY_API_SECRET
```

Les deux valeurs sont dans Cloudinary → **Settings → API Keys**. Sans elles le
déploiement est refusé, et c'est voulu.

```bash
cd functions && npm install
firebase deploy --only functions
```

**Étape 2 — passer du blanc au réel.** Par défaut,
`CLOUDINARY_SUPPRESSION_REELLE` vaut `false`. Dans cet état la fonction **journalise
ce qu'elle supprimerait sans rien supprimer**. Supprime une rumeur avec photo,
puis :

```bash
firebase functions:log --only purgeMediaRumeur
```

Tu dois lire une ligne `[à blanc] rumeur <id> — serait supprimé :
image/askip/img_…`. Vérifie que l'identifiant est bien celui de la photo de
cette rumeur-là. Quand tu es convaincu :

```bash
cp functions/.env.example functions/.env
# puis dans functions/.env : CLOUDINARY_SUPPRESSION_REELLE=true
firebase deploy --only functions
```

`functions/.env` n'est pas versionné (`functions/.gitignore`) : c'est le
fichier de ta machine, et aucun secret ne doit y figurer non plus.

Une suppression est irréversible, et rien de tout ceci n'a pu être essayé
avant d'atterrir chez toi : c'est la raison de ce détour.

**Ce que ça ne fait pas :** changer de photo de profil laisse encore l'ancienne
sur Cloudinary. La supprimer casserait les `userPhotoUrl` recopiés sur les
anciennes rumeurs et commentaires, que tous les écrans ne rafraîchissent pas
depuis le profil vivant. Ce cas demande d'être traité pour lui-même.

Surveille quand même **Dashboard → Usage** en début de mois.

---

## 4. Après le déploiement — le test qui vérifie tout

Dans cet ordre, avec un compte de test :

1. **Créer un compte** → le profil apparaît dans `Firestore → profiles`.
   *(Si ça échoue : §1.2)*
2. **Publier une rumeur avec une photo** → elle apparaît dans le fil et dans la
   Media Library Cloudinary. *(Si l'image manque : §3.1 ou §3.2)*
3. **Publier une rumeur vocale** 🎙 → autoriser le micro, parler 5 s, envoyer.
   *(Si l'envoi échoue : le format `m4a`, §3.2)*
4. **Commenter cette rumeur vocale** → seul le bouton micro doit s'afficher,
   aucun champ texte.
5. **Publier sur la Page de Vérité** ⚖️ → le serment doit s'imposer avant
   publication.
6. **Voter « bidon » 8 fois** avec des comptes différents → le tampon `PARJURE`
   apparaît et le taux de la page bouge.
7. **Parier** 🎟 sur une rumeur → le ticket doit s'afficher.
   *(S'il n'apparaît jamais : l'index de groupe `bets`, §1.3)*
8. **Sceller une capsule** 🔒 sur 1 h → depuis un **autre** compte, le contenu
   doit être illisible. *(S'il est visible : les règles ne sont pas déployées)*
9. **Fermer complètement l'app** sur un téléphone, lui envoyer un message depuis
   un autre → la notification doit arriver.
   *(Silence : `firebase functions:log --only sendPushNotification`, §2.1)*

---

## Résumé

| | Fait | À faire par toi |
| --- | --- | --- |
| Règles Firestore | ✅ écrites | ⬜ déployer |
| Index Firestore | ✅ écrits | ⬜ déployer **avant les paris** |
| `firebase.json` / `.firebaserc` | ✅ créés | — |
| Cloud Function d'envoi des push | ✅ écrite | ⬜ Blaze + région + déployer |
| Préférences de notification côté serveur | ✅ synchronisées | — |
| Base Firestore | — | ⬜ vérifier qu'elle existe |
| Auth e-mail | — | ⬜ activer |
| UID admin | ✅ en place | ⬜ confirmer que c'est le tien |
| Preset Cloudinary | — | ⬜ vérifier *unsigned* + `m4a` |
| Purge des médias | ✅ écrite (6 déclencheurs) | ⬜ secrets Cloudinary + déployer, puis armer (§3.4) |
