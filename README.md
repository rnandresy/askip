# 🗣️ Askip

**Le réseau social des rumeurs du campus, sur Android.**

*Askip* — « à ce qu'il paraît ». Ici, une rumeur ne se like pas : **elle se
juge**. Le campus vote, les informateurs gagnent ou perdent leur réputation, et
l'anonymat est une fonction à part entière, pas un mode caché.

[![Vérification](https://github.com/rnandresy/askip/actions/workflows/verification.yml/badge.svg)](https://github.com/rnandresy/askip/actions/workflows/verification.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%C2%B7%20Firestore%20%C2%B7%20Functions-FFCA28?logo=firebase&logoColor=black)
![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
![Version](https://img.shields.io/badge/version-2.1.0-informational)
![Licence](https://img.shields.io/badge/licence-MIT-blue)

---

## ✨ Ce qui fait Askip

### Juger les rumeurs
- **Rumeur-mètre** 🧐 / 🚫 — chaque rumeur se vote *Crédible* ou *Bidon* ;
  aucun verdict n'est affiché sous 8 votes
- **Clout** — la réputation d'informateur monte ou baisse selon le verdict
- **Le Pari** 🎟 — miser sa réputation sur un verdict avant qu'il tombe
- **Droit de réponse** ⚖️ — une personne citée obtient une place épinglée en
  tête du post

### Faire circuler
- **Quatre fils** — 🔥 Ça chauffe · 🆕 Frais · 🎭 Confessions · 🏆 Légendes
- **Le Scoop** 💎 — une seule pépite par jour, qui pèse lourd dans le classement
- **Salons de tags** — #amphi, #exam, #drama… et une bande « ça circule »
- **Téléphone arabe** 📞 — une rumeur écrite à plusieurs mains
- **Capsule scellée** 🔒 — illisible avant une date, ou avant 10 clés données
- **Rumeurs éphémères** — elles disparaissent du fil après 24 h

### Anonymat et vie du campus
- **Masques anonymes** — un masque stable par rumeur, pour suivre une
  conversation sans révéler l'auteur
- **Sujet du jour** et **Météo du campus** 🌩 — de *Calme plat* à *Tempête*
- **Missions du jour, XP, niveaux** et **classement**

### Le socle social
Stories, commentaires, messagerie privée et de groupe, messages vocaux,
notifications push, succès, recherche et profils.

Le détail de chaque mécanique, et les raisons de chaque choix, sont dans
[AMELIORATIONS.md](AMELIORATIONS.md).

---

## 🛠️ Stack technique

| | |
|---|---|
| Application | Kotlin, Jetpack Compose, Material 3, Navigation Compose, Coil, Media3, DataStore |
| Données | Firebase Authentication, Cloud Firestore (règles et index versionnés) |
| Serveur | Cloud Functions (Node 20) : notifications push, purge des médias |
| Médias | Cloudinary |
| Qualité | GitHub Actions à chaque push : build debug et tests, build release (R8), analyse des URL Cloudinary, banc de 81 cas de règles Firestore sur émulateur |
| Cible | Android 8.0 et plus (minSdk 26), compileSdk 35 |

---

## 📁 Structure

```
app/                     application Android (com.rnandresy.lol)
functions/               Cloud Functions : notifications, purge des médias
firestore.rules          règles de sécurité Firestore
firestore.indexes.json   index Firestore
tools/                   compilation et tests sans Gradle, banc des règles
docs/attic/              code mis de côté, non compilé
licences/                licences des ressources tierces
.github/workflows/       vérification automatique
```

---

## 🚀 Lancer le projet

**Prérequis** : Android Studio, JDK 17, un projet Firebase et un compte
Cloudinary.

1. **Cloner le dépôt**
   ```bash
   git clone https://github.com/rnandresy/askip.git
   ```
2. **Configurer Firebase**
   - ajouter une application Android `com.rnandresy.lol` à ton projet Firebase ;
   - activer **Authentication** (e-mail) et **Cloud Firestore** ;
   - télécharger `google-services.json` et le placer dans `app/`.

   Ce fichier est propre à chaque projet Firebase : il n'est jamais versionné.
3. **Configurer Cloudinary** : créer un preset d'envoi *non signé*, puis
   renseigner `CLOUD_NAME` et `UPLOAD_PRESET` dans
   `app/src/main/java/com/rnandresy/lol/utils/CloudinaryUploader.kt`.
4. **Ouvrir le projet dans Android Studio** et lancer l'application.

Le déploiement des règles, des index et des Cloud Functions, les secrets
Cloudinary et le blocage des anciennes versions sont décrits pas à pas dans
[CONFIGURATION.md](CONFIGURATION.md).

---

## ✅ Vérifier

```bash
bash tools/verifier-compilation.sh      # compile tout le Kotlin sans Gradle
bash tools/lancer-tests.sh              # tests unitaires sans Gradle
bash tools/regles-firestore/lancer.sh   # banc des règles sur l'émulateur Firestore
```

À chaque push, GitHub Actions construit l'application et rejoue ces bancs. Le
build automatique utilise une configuration Firebase factice : il vérifie que
tout compile, sans se connecter à aucun projet.

---

## Auteur

**Christiano** — [github.com/rnandresy](https://github.com/rnandresy)

## Licence

Le code est distribué sous licence MIT, voir [LICENSE](LICENSE). La police
Sedgwick Ave reste sous sa propre licence (SIL Open Font License), voir
[licences/](licences/).
