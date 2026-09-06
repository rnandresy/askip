# Askip — refonte

Ce document décrit ce qui a changé, pourquoi, et ce qu'il reste à faire.

---

## 0. Audit de non-régression

Comparaison ligne à ligne de l'état actuel avec le dernier commit (`82e34b8`),
c'est-à-dire la version qui tournait chez les utilisateurs.

### Rien n'a disparu

| Vérification | Résultat |
| --- | --- |
| Fichiers d'écran | **48/48 présents.** Seul `FirebaseRepository.kt` a disparu — son contenu est redistribué dans 5 repositories, aucune fonction perdue |
| Membres publics d'`AskipViewModel` | **0 manquant** |
| Composables de l'interface | **0 manquant** |
| Champs des 11 modèles | **0 supprimé** (uniquement des ajouts) |
| Routes de navigation | **18/18 présentes**, + 4 nouvelles |
| Fonctions du repository | 60 → 97, toutes couvertes (certaines renommées ou fusionnées) |

Renommages : `addReaction`/`removeReaction` → `setReaction` (fusionnées),
`togglePin` → `setPinned`, `listenToStories` → `listenToActiveStories`,
`listenToPosts` → `listenToRecentPosts`, `createNotification*` →
`NotificationRepository.create*`, `markNotificationRead` → `markRead`.

### Cinq régressions trouvées et corrigées

L'audit a révélé cinq comportements que j'avais cassés sans le vouloir.

**1. `@everyone` n'était plus réservé aux admins.**
La version d'origine ne diffusait la mention à tout le campus que si l'auteur
était admin. Ma réécriture avait perdu ce garde-fou : n'importe qui pouvait
réveiller tout le monde. Rétabli.

**2. Le préfixe « Askip » avait disparu.**
Chaque rumeur s'ouvrait par « Askip … » — la signature de l'app. Je l'avais
supprimé sans le voir. Rétabli, avec une garde anti-doublon si le texte commence
déjà par « Askip ».

**3. La suppression de compte détruisait le contenu avant de vérifier le mot
de passe.** Un mot de passe erroné laissait la personne sans ses posts **et**
avec son compte toujours actif. La réauthentification passe maintenant en
premier.

**4. Seuls 40 profils étaient chargés.** J'avais mis la limite à `PAGE_SIZE_MEMBERS`.
Ces profils servent partout : résolution des pseudos et photos sur chaque post,
autocomplétion des mentions, annuaire des membres. Au-delà de 40 inscrits, des
gens disparaissaient de l'app. Limite portée à 500.

**5. L'historique était tronqué.** Conversations et commentaires étaient limités
à 120 et 100 éléments là où la version d'origine chargeait tout. Portés à 400 et
300. Surtout, **le fil ne montrait plus que les 120 rumeurs récentes** : la
pagination a été ajoutée, le pied de liste charge la suite quand on l'atteint,
et on remonte de nouveau jusqu'à la toute première rumeur.

### Changements de comportement assumés

- Les comptes **bannis** sont désormais écartés de l'annuaire et des mentions.
- Les confessions affichent un **masque stable** (« Le Corbeau #7B2 ») au lieu de
  « Quelqu'un 🎭 ». Les anciennes confessions en bénéficient aussi : le masque
  est calculé à l'affichage.
- Le fil est trié par **chaleur** dans l'onglet « ça chauffe », par date dans
  « frais » — l'ancien tri (épinglés puis date) correspond à l'onglet « frais ».

---

## 1. Le projet ne compilait plus

Avant toute amélioration, il fallait réparer. Voici ce qui était cassé.

| Problème | Effet |
| --- | --- |
| `AskipViewModel` vidé de ses 1 276 lignes, remplacé par des fabriques | Les 20 écrans appelaient une API supprimée |
| 5 nouveaux ViewModels référençant `FeedRepository`, `ProfileRepository`, `AuthRepository`, `NotificationRepository`, `MessageRepository`, `GroupRepository` | **Aucun de ces fichiers n'existait** |
| `class \`SessionViewModel.kt\`` et `class \`ProfileViewModel.kt\`` | Noms de classe littéraux, avec l'extension de fichier dedans |
| `AdminConfig.kt` amputé de `ADMIN_UID`, `ADMIN_BADGE_NAME`, `ENI_CLASSES`, `AVATAR_FRAMES`, `BADGE_COLORS` | Références mortes dans `ProfileScreen`, `EditProfileScreen`, `AppNotification` |
| `ADMIN_UIDS` réduit à un ensemble vide (UID en commentaire) | `isAdmin()` renvoyait toujours `false` — plus aucun admin |
| `CloudinaryUploader` renvoyant désormais `UploadResult` | Tous les appels attendaient encore une `String` |
| `AskipMessagingService` : `fromAdmin = TODO()` | **Plantage** à chaque notification push reçue |
| Service FCM absent du manifeste | Les push n'arrivaient jamais jusqu'à l'app |

### Ce qui a été fait

`FirebaseRepository` (657 lignes, tout mélangé) a été **découpé** — pas dupliqué — en
cinq repositories par domaine, exactement ceux que les nouveaux ViewModels attendaient :

```
repository/
├── Firestore.kt            instance partagée + écritures par lots
├── AuthRepository.kt       connexion, inscription, compte
├── ProfileRepository.kt    profils, badges, succès, réputation, classements
├── FeedRepository.kt       rumeurs, commentaires, stories, votes
├── MessagingRepository.kt  conversations, groupes
└── NotificationRepository.kt
```

`AskipViewModel` a été restauré **au-dessus de ces repositories**. Les écrans
continuent de fonctionner sans modification, et la couche données est enfin propre.

Les cinq ViewModels inachevés sont dans [`docs/attic/`](docs/attic/README.md), avec
la marche à suivre pour reprendre la migration écran par écran. Ils ne sont plus
compilés : ils visaient une API imaginaire (threads de commentaires, pagination
des anciens messages, épinglage de conversation) qui reste à écrire.

---

## 2. Ce qui rend l'app spécifiquement « rumeurs »

Une app de rumeurs qui se contente de likes est un fil d'actualité de plus.
Le principe retenu : **une rumeur, ça se juge.**

### Le Rumeur-mètre 🧐 / 🚫

Chaque rumeur porte deux boutons : *Crédible* et *Bidon*. En dessous de 8 votes,
l'app affiche « enquête en cours » sans barre — annoncer un verdict sur trois voix
serait exactement le travers qu'on veut combattre.

Au-delà, la rumeur bascule :

| Part de « crédible » | Verdict |
| --- | --- |
| ≥ 72 % | ✅ **Confirmée** |
| ≤ 28 % | ❌ **Démentie** |
| entre les deux | ⚔️ **Contestée** |

On ne vote pas sur sa propre rumeur.

### Le clout — la réputation d'informateur

Le verdict a des conséquences pour l'auteur :

| Événement | Clout |
| --- | --- |
| Quelqu'un te croit | **+3** |
| Quelqu'un te démonte | **−2** |
| On t'offre un Scoop | **+8** |

Le clout peut descendre, volontairement : sans risque, raconter n'importe quoi ne
coûterait rien. Le profil affiche aussi une **fiabilité** (part de rumeurs confirmées).

### Le Scoop 💎 — un par jour

Une seule pépite quotidienne par personne, qui pèse lourd dans le classement
« ça chauffe ». C'est la rareté qui donne du sens au geste : donner son Scoop est
un vrai choix. Il se recharge à minuit.

### « Ça chauffe » — le classement par chaleur

Un score façon gravité : l'engagement pousse vers le haut, le temps tire vers le bas.

```
chaleur = (réactions + 2·commentaires + 12·scoops + 1,5·votes) / (heures + 2)^1,45
```

Une rumeur d'hier doit être vraiment brûlante pour tenir tête à une rumeur de ce
matin. Le calcul se fait **côté client** sur les 120 rumeurs les plus récentes :
Firestore ne sait pas trier par un score décroissant sans Cloud Function, et cette
fenêtre suffit largement pour un campus — c'est instantané et ça ne coûte aucune
lecture supplémentaire.

### Quatre fils au lieu d'un

🔥 Ça chauffe · 🆕 Frais · 🎭 Confessions · 🏆 Légendes

L'onglet choisi est mémorisé entre deux lancements.

### Les salons de tags

Dix tags (#amphi, #couple, #prof, #exam, #drama, #wtf…), trois maximum par rumeur,
choisis dans l'éditeur ou écrits directement avec un `#`. Une bande **« ça circule »**
montre les tags qui montent — pondérés par la chaleur des rumeurs qui les portent,
pas seulement par leur nombre : trois rumeurs brûlantes battent dix posts morts.

### Les masques anonymes

Une confession signée « Quelqu'un » n'a pas d'identité, donc pas de suite possible.
Chaque auteur anonyme reçoit désormais un **masque stable par rumeur** :
« Le Corbeau #7B2 🦉 ». Même personne + même rumeur = même masque, toujours.
Même personne + autre rumeur = autre masque : impossible de recouper.

Sous une confession, **tout le monde commente masqué** — le choix n'est pas
proposé, il est imposé, sinon les commentateurs trahiraient le contexte.
Ailleurs, un bouton « commenter masqué » reste disponible.

### Missions du jour, XP et niveaux

Trois missions courtes, tirées de la date — les mêmes pour tout le campus,
renouvelées à minuit, sans aucune lecture serveur. On encaisse l'XP d'un bouton.

40 niveaux, de « Nouveau » à « Légende du campus ». Le bandeau de montée de niveau
n'apparaît qu'à une vraie montée : au premier chargement, le niveau est mémorisé
sans célébration, sinon chaque ouverture de l'app en déclencherait une fausse.

### Le sujet du jour

Une amorce quotidienne en tête de fil, identique pour tout le monde
(« Qui forme le couple le moins discret du campus ? »). C'est ce qui donne
une raison d'ouvrir l'app le matin même quand le fil est calme.

### Le classement

Trois façons d'être bon, volontairement : le clout récompense les rumeurs qu'on
croit, l'XP récompense la présence, la série récompense la régularité. Plus un
panthéon des rumeurs cultes. Personne n'est premier partout, donc tout le monde
a un classement à viser.

### Rumeurs éphémères

Une bascule dans l'éditeur : la rumeur disparaît du fil après 24 h.

---

## 2 bis. Ce qu'aucun réseau global ne peut faire

Facebook, Instagram et TikTok sont mondiaux et à identité obligatoire. Askip est
petit, fermé, et l'anonymat y est une primitive. Ces cinq mécaniques en découlent
directement — elles n'auraient aucun sens ailleurs.

### 🎟 Le Pari — miser sur un verdict avant qu'il tombe

Aucun réseau ne met la réputation en jeu sur la véracité d'une publication.
Ici, on parie sur ce que le campus finira par décider.

**La cote est figée au moment du pari**, à partir de deux choses :

```
cote = (1 / part des votes déjà de ton côté) × bonus de précocité
```

Miser « crédible » quand 20 % seulement y croient rapporte ×5. Suivre la foule
sur une rumeur déjà tranchée ne rapporte presque rien. Parier avant que le
campus se prononce vaut jusqu'à ×1,5 de plus. Plafond ×5.

**Les jetons plutôt que le clout.** On mise des jetons 🎟, pas de la réputation :
3 par jour, non achetables, rechargés à minuit. Le gain, lui, est du clout.

Ce choix résout trois problèmes d'un coup : un nouveau venu (0 clout) peut jouer
dès le premier jour, on ne peut pas spammer les paris, et chaque mise devient
un vrai choix parce qu'il n'y en a que trois.

Le règlement se fait **côté client, chacun pour son propre pari** — c'est ce qui
permet de s'en passer de Cloud Function tout en gardant des règles serrées :
le document d'un pari porte l'UID comme identifiant, et les règles n'autorisent
à le régler qu'une fois, par son propriétaire.

Nouveau classement : **🎟 Oracles**, au flair, à partir de 5 paris réglés — en
dessous, un « 100 % » ne veut rien dire.

### 🔒 La Capsule scellée — un texte réellement illisible

Une rumeur qu'on **ne peut pas** lire avant une date, ou avant que 10 personnes
aient donné leur clé 🔑.

Le point crucial : ce n'est pas un flou d'affichage. Le contenu vit dans une
sous-collection à part, et les règles Firestore refusent de le servir :

```
allow read: if resource.data.authorId == uid()
         || request.time.toMillis() >= resource.data.unsealAt
         || (clés réunies)
```

`request.time` est l'horloge du serveur. Masquer le texte côté app n'aurait rien
protégé — il suffisait d'ouvrir la base. Là, l'app n'a **jamais** le texte entre
les mains avant l'heure. Seul l'auteur peut relire sa capsule.

Le compte à rebours tourne à la seconde, et la barre de clés montre combien il
en manque pour forcer l'ouverture — ce qui crée un rendez-vous collectif.

### 📞 Le Téléphone arabe — une rumeur à plusieurs mains

Un format de publication qui n'existe nulle part : l'auteur écrit une phrase,
**six autres personnes** peuvent en ajouter une, **une seule chacune**.

Chaque maillon est décalé d'un cran vers la droite : on *voit* la rumeur
s'éloigner de son point de départ. C'est littéralement comment une rumeur
circule, transformé en objet.

Un maillon posé ne se réécrit pas (`allow update: if false`), sinon la chaîne
serait falsifiable a posteriori.

### ⚖️ Le Droit de réponse — une place garantie au sujet

Quand une rumeur te cite nommément, tu obtiens une place **épinglée en tête du
post**. Pas un commentaire noyé en bas, longtemps après.

Aucune plateforme ne donne ça à la personne visée par une publication : sa
version arrive toujours trop tard et trop bas. C'est aussi le contrepoids
responsable des mécaniques de pari — plus l'app rend le jugement ludique, plus
la personne jugée doit pouvoir répondre.

Le fil affiche un badge « droit de réponse exercé » : on ne lit pas une rumeur
sans savoir qu'elle a été contestée par la personne concernée.

### 🌩 La Météo du campus

Une jauge d'ambiance sur 24 h, en tête de fil, **identique pour tout le monde
au même moment** : Calme plat → Dégagé → Ça bruisse → Orageux → Tempête.

Elle pèse le volume, l'engagement et surtout la friction (part de rumeurs
contestées ou démenties) — parce qu'un campus calme avec 40 posts n'est pas
un campus en crise avec 40 posts.

Entièrement calculée en mémoire sur les rumeurs déjà chargées : zéro lecture
Firestore. Le fond pulse quand ça chauffe, sauf si « Réduire les animations »
est actif.

---

## 2 ter. Les trois actualités

L'accueil n'est plus un fil unique : c'est trois espaces, chacun avec sa propre
règle du jeu. Le sélecteur en haut change de **lieu** ; les onglets en dessous
(ça chauffe / frais / confessions / légendes) ne réordonnent que l'actualité
principale, et disparaissent ailleurs.

### 📰 Actualité — inchangée

Tout ce qui existait reste là, au même endroit : stories, sujet du jour, tags
tendance, tri par chaleur, capsules, chaînes, confessions. Rien n'a bougé.

### 🎙 Vocal — on ne lit pas, on écoute

Une actualité **sans une seule ligne de texte**. Ni post, ni commentaire.

- On publie avec un bouton micro, pas un champ de saisie. Appui pour parler,
  second appui pour envoyer — pas de maintien du doigt : tenir un bouton
  pendant deux minutes est intenable, et ça empêche de lire l'écran en parlant.
- Aucune légende n'est acceptée. Une actualité vocale qui tolérerait une phrase
  de résumé cesserait d'être vocale : on lirait le résumé et on n'écouterait
  jamais.
- **Sous une rumeur vocale, le champ texte disparaît des commentaires.** On
  répond par la voix ou on ne répond pas.
- Le lecteur occupe toute la largeur de la carte : la voix n'est pas une pièce
  jointe, elle *est* la publication.
- 2 min pour un post, 1 min pour une réponse. Au-delà, l'app envoie ce qui est
  enregistré plutôt que de tout jeter.

Correction au passage : la durée des notes vocales n'était jamais enregistrée
(`audioDuration` restait à 0 depuis toujours). Le lecteur affichait « 0:00 ».

### ⚖️ La Page de Vérité

Une page qui se prend au sérieux du début à la fin : majuscules, sceau,
vocabulaire de greffier, et **serment obligatoire** avant chaque publication.

> « Je jure sur l'honneur que ce que je m'apprête à publier est rigoureusement
> exact. »

Il faut cocher pour continuer. Ce petit geste engage — et c'est exactement ce
qui rend la suite savoureuse.

Car la page tient elle-même le compte. En tête, en permanence :

```
        TAUX DE PARJURE          87 %
  ████████████████████████░░░░
  34 parjure(s) et 5 serment(s) tenu(s) sur 39 jugé(s) — 61 au registre.

              Plus personne ne fait semblant.
```

Le taux se calcule sur les serments que **le campus** a démentis via le
Rumeur-mètre. La page ne se moque de personne : elle énonce, elle mesure. C'est
le chiffre qui fait la blague, pas un clin d'œil de l'app.

Chaque rumeur jugée reçoit un **tampon incliné** — `PARJURE` en rouge, ou
`SERMENT TENU` en vert. Et l'énoncé sous le compteur suit le taux :

| Taux | Énoncé |
| --- | --- |
| 0 % | « Aucun parjure constaté. C'est suspect. » |
| < 30 % | « Étonnamment honnête. Ça ne durera pas. » |
| < 60 % | « Un serment sur deux tient. C'est déjà ça. » |
| < 85 % | « La vérité est en difficulté. » |
| ≥ 85 % | « Plus personne ne fait semblant. » |

Aucune nouvelle règle Firestore ni aucun index : les deux actualités
s'appuient sur `postType` et sur le Rumeur-mètre qui existaient déjà.

---

## 2 quater. Refonte de l'interface — les bulles

Le fil était saturé. Avant la première rumeur, six bandeaux s'empilaient :
météo, sections, tri, sujet du jour, stories, tendances. Et chaque carte
alignait dix blocs. À dix rumeurs à l'écran, plus rien ne ressortait.

### Ce qui a été enlevé de la vue — sans être supprimé

| Avant, en permanence | Maintenant |
| --- | --- |
| Bandeau météo du campus | Une puce à droite du tri → feuille |
| Sélecteur de section (3 gros boutons) | Un segmenté glissant, une ligne |
| Onglets de tri (4 puces) | Une puce « 🔥 Ça chauffe » → feuille |
| Carte « sujet du jour » | Dans la feuille des réglages |
| Bande « ça circule » | Dans la feuille des réglages |
| 5 émojis de réaction par carte | Un cœur — appui long pour choisir |
| Rumeur-mètre déplié (5 lignes) | Une pastille « 72 % » → feuille |
| Panneau de pari déplié | Dans la même feuille |
| Bouton Scoop | Dans la même feuille |
| Tags sous chaque post | Dans la même feuille |
| 5 onglets en bas | 4 — les notifications passent en cloche |

Rien n'a disparu : tout demande un geste de plus, ce qui est le juste prix
d'une action qu'on ne fait pas à chaque rumeur. La carte est passée de dix
blocs à quatre — auteur, texte, média, une ligne d'actions.

### Le thème Sakura, par défaut

Beige chaud et blanc, rose de cerisier pour l'accent, or pâle pour l'éclat.
L'app parle de rumeurs : un fond crème désamorce le sujet mieux qu'un noir
agressif, et laisse respirer un fil dense.

Les huit autres thèmes restent disponibles. `Automatique` bascule désormais
entre Sakura le jour et Noir & Blanc la nuit.

### Les bulles

Chaque élément cliquable de l'app partage le même relief, en quatre couches
toujours dans cet ordre : **ombre portée** pour détacher du fond,
**remplissage**, **liseré** pour le contour, **reflet** en haut pour l'aspect
verre. Retirer une seule des quatre fait retomber le bouton à plat.

`BubbleButton`, `BubbleIconButton`, `BubbleChip`, `BubbleBadge`, `BubbleCard` —
quatre tons (principal, doux, fantôme, danger) et trois tailles.

**La lisibilité est calculée, pas devinée.** `readableOn()` choisit l'encre
noire ou blanche selon la luminance du fond : le rose de Sakura, le vert de
Matrice et le blanc de Noir & Blanc n'appellent pas le même texte, et un thème
ajouté demain sera traité correctement sans qu'on y pense.

### Le décor

- **Poussière d'étoiles** — scintillement lent, positions tirées une fois pour
  toutes à partir d'une graine fixe (sinon elle se redistribue à chaque
  recomposition).
- **Pétales de sakura** — chute, dérive sinusoïdale et rotation décorrélées ;
  ils s'estompent en entrant et en sortant du cadre.
- **Halo** derrière le logo.

Le tout est **entièrement coupé** si « Réduire les animations » est actif : un
décor n'a jamais le droit de gêner quelqu'un.

### Le toucher

L'ondulation Material, qui se propage depuis le point de contact, est remplacée
partout par un léger enfoncement au ressort (`TapArea`). Un seul ressort pour
toute l'app — c'est cette cohérence, plus que la durée, qui donne la sensation
« coulée ».

Les icônes passent en **`Icons.Rounded`**, le jeu le plus proche des symboles
d'iOS parmi ceux déjà présents dans le projet.

### Vérification

Chaque fonction déplacée garde un point d'entrée, contrôlé un par un : tri,
météo, sujet du jour, missions, classement, membres, tendances, verdict, pari,
Scoop, tags, épinglage, signalement, suppression, clé de capsule.

### L'extension à toute l'app

Le fil et la connexion ouvraient la voie ; les bulles couvrent maintenant les
douze écrans restants. Il ne subsiste **aucun bouton Material** dans l'app :
zéro `Button`, `OutlinedButton`, `FilledIconButton`, `IconButton`, `FilterChip`
ou `FloatingActionButton`. `AskipButton`, l'ancien bouton maison, délègue
désormais à `BubbleButton` — il ne peut plus diverger.

**Le profil** passe de neuf sections flottantes à cinq cartes : en-tête,
identité, réputation, activité, trophées, badges. La carte *réputation* affiche
le niveau, le clout, la fiabilité et le flair — quatre chiffres qui vivaient
dans l'écran des missions alors qu'ils disent le plus de quelqu'un dans une app
de rumeurs.

**L'édition du profil** range ses onze champs en quatre cartes — identité,
humeur, apparence, détails — et remplace ses deux menus déroulants par des
feuilles, plus faciles à viser au pouce.

**Les trophées** gagnent une carte de progression en tête et un sélecteur
`Tous / Obtenus / À faire` : on vient y voir ce qui reste à décrocher.

**Les réglages** montrent les thèmes au lieu de les nommer. Chaque pastille
porte les vraies couleurs du thème — fond, carte, accent — ce qu'un
« Nostalgique 🕯 » ne dit pas.

**La publication** rassemble ses six formats dans une table unique
(`FORMATS`) : titre d'écran, libellé de puce et règle du jeu au même endroit,
là où trois `when` séparés finissaient toujours par diverger. Salon et durée de
vie, deux réglages voisins jamais regroupés, partagent enfin une carte.

**Les messageries** adoptent une seule cible d'envoi qui bascule entre avion et
micro selon qu'il y a quelque chose à envoyer.

### Les derniers écrans

Restaient les listes et les composants partagés. Il n'existe plus dans l'app
**un seul contrôle Material** : ni `Button`, `OutlinedButton`, `IconButton`,
`FilledIconButton`, `FilterChip`, `FloatingActionButton`, `TabRow`, ni un
`Surface` cliquable. Tout passe par la bulle.

| Écran | Ce qui change |
| --- | --- |
| **Messages** | Les deux onglets Material deviennent le segmenté glissant du fil ; chaque conversation est une carte-bulle qui se soulève quand elle n'est pas lue ; le choix du correspondant passe d'une boîte de dialogue à une feuille |
| **Membres** | Des cartes espacées au lieu de lignes séparées d'un trait ; la série n'apparaît qu'à partir de deux jours — « 🔥 1 » n'est pas une série |
| **Classement** | Sa propre ligne se détache par le remplissage : dans une liste de trente, se retrouver ne doit pas demander de lire les pseudos |
| **Notifications** | Le relief dit ce que la couleur disait mal — une notification lue s'aplatit, une annonce admin non lue ressort en or |
| **Recherche** | Barre en pilule, filtres en puces qui n'apparaissent qu'une fois la requête tapée, résultats en cartes |
| **Trophées, réglages, publication…** | Déjà traités à la passe précédente |

Côté composants partagés, la bascule est plus profitable encore : `RumorMeter`,
`ScoopButton`, `ReactionButton`, `TagChip`, `VerdictChip`, `StreakChip`,
`HotBadge`, `SegmentedTabs`, `FeedSectionBar`, `VoiceComposer`,
`VoiceRecordFab` et le sélecteur de mentions sont utilisés par plusieurs
écrans — une seule correction les aligne tous.

Deux détails qui comptent :

- **Le Scoop déjà donné garde son relief.** C'est une décision qu'on a prise,
  pas un bouton grisé. Seul le Scoop indisponible s'efface.
- **Un salon sans action reste une étiquette.** `TagChip` ne met de ressort
  sous le doigt que s'il mène quelque part.

---

### Ce que l'audit a rattrapé

Quatre défauts réels, trouvés en comparant écran par écran à la version testée :

| Défaut | Effet | Corrigé en |
| --- | --- | --- |
| `vm.loading` n'était plus lu au profil | Deux appuis = deux envois de la même photo | `enabled = !busy` sur les quatre commandes photo |
| « Profil introuvable » s'affichait avant la fin de la lecture | Chaque profil distant clignotait sur une fausse erreur | `lookupDone` + chargement tant que la requête tourne |
| Ligne « Changer l'email » : seul le chevron réagissait | Cible de 32 dp au bout d'une ligne pleine largeur | Toute la ligne est tapable |
| Interrupteur « Annonces admin », verrouillé mais actif | Il virait au gris, donc se lisait « éteint » | Couleurs explicites pour l'état désactivé-coché |

Le contrôle est **automatisé** : `audit.sh` compare, pour chaque fichier
modifié, les appels au ViewModel présents dans la version testée et ceux
présents aujourd'hui. Aucun appel perdu. Une seconde passe fait la même chose
sur les libellés visibles ; les seuls écarts sont des reformulations
volontaires, chacune vérifiée à la main.

### Le build

`1112 classes, 0 erreur, 0 warning`. Les trente-trois dépréciations d'icônes
(`Icons.Filled.ArrowBack` et consorts) sont passées en `AutoMirrored`, ce qui
retourne aussi correctement les flèches en écriture droite-à-gauche.

---

## 3. Design

### Huit thèmes

Noir & Blanc · Néon · Nostalgique · **Sang d'encre** · **📰 Tabloïd** ·
**🟩 Matrice** · **Grand jour** (clair) · **Automatique** (suit le système).

Toute l'app était sombre. Une salle en plein soleil, un écran peu lumineux, ou
simplement une préférence : les thèmes clairs rendent l'app lisible partout.

**Tabloïd** est le plus juste pour le sujet : encre noire sur papier jauni,
rouge de une pour ce qui compte. On lit le campus comme un canard à scandale.
**Matrice** est un terminal phosphore, pour ceux qui veulent enquêter.

### Des tokens plutôt que des valeurs en dur

`Space`, `Radius`, `Motion` et une palette métier (`LocalAskipPalette`) pour ce que
Material ne couvre pas : couleurs de verdict, Scoop, chaleur, série.

### Mouvement et retour tactile

Rebond à ressort sur les réactions, le Scoop et les votes, vibration courte à
chaque geste, transitions de couleur sur les onglets, squelettes animés pendant
le chargement. Un écran vide donne l'impression que l'app est cassée ; un
squelette dit « ça arrive ».

Deux réglages **réellement câblés** dans **Paramètres → Confort** :

- **Vibrations** — tous les gestes passent par `rememberTapFeedback()`, couper
  l'option les coupe tous.
- **Réduire les animations** — supprime les rebonds. Le mouvement est un plaisir
  pour la plupart, une gêne réelle pour certaines personnes.

### Autres corrections d'interface

- **Un seul endroit** affiche erreurs et confirmations (snackbar global). Avant,
  chaque écran gérait les siennes et plusieurs n'en montraient aucune.
- L'anneau des stories **s'éteint une fois la story vue**.
- Pastilles sur les onglets : notifications, messages non lus, missions à encaisser.
- Menu « … » sur chaque rumeur : épingler, **signaler**, supprimer.
- Le sélecteur de thème défile — six choix ne tenaient plus sur une ligne.
- Descriptions d'accessibilité sur les réactions, votes et icônes.

---

## 4. Sécurité et coût

### `firestore.rules` — à déployer

Le code référençait ce fichier mais il n'existait pas. Il est maintenant écrit :
chacun n'écrit que ce qui le concerne, et pour les champs partagés (réactions,
verdicts, Scoop) les règles vérifient qu'on **ne s'ajoute ou ne se retire que
soi-même** d'un tableau.

```bash
firebase deploy --only firestore:rules,firestore:indexes
```

> ⚠️ Remplacer l'UID admin dans `firestore.rules` **et** dans
> `utils/AdminConfig.kt` — les deux doivent correspondre.

Ce que les règles verrouillent vraiment :

| Mécanique | Garantie appliquée par le serveur |
| --- | --- |
| Réactions, verdicts, Scoop, clés | On ne peut s'ajouter ou se retirer que **soi-même** d'un tableau |
| Capsule scellée | Contenu **illisible** avant `request.time >= unsealAt` |
| Maillon de chaîne | Un seul par personne, 7 max, **jamais réécrit** |
| Pari | Un seul par rumeur (l'UID est l'id du document), mise entre 1 et 3, réglé une seule fois et seulement par son propriétaire |
| Droit de réponse | Chacun n'écrit que la sienne (l'UID est l'id du document) |
| Signalements | Lisibles par l'admin uniquement |

**Compromis assumé et documenté** : sans Cloud Function, seul le client peut
incrémenter le clout de l'auteur d'une rumeur. Les règles restreignent cette
écriture aux trois champs `clout`, `confirmedRumors`, `debunkedRumors` et rien
d'autre, mais un client modifié pourrait en abuser. Le règlement des paris a la
même limite. À déplacer dans une Cloud Function dès le passage au plan Blaze.

**Le droit de réponse n'est pas vérifié côté serveur** : les règles ne savent pas
lire le contenu d'une rumeur pour confirmer qu'elle cite bien la personne. Le
contrôle est côté client, et l'app n'affiche une réponse que si la mention est
réellement présente — quelqu'un pourrait créer un document inutile, jamais
affiché.

### Lectures Firestore

- Le fil écoute **120 rumeurs** au lieu de la collection entière.
- Les messages et commentaires sont limités.
- Tri, tendances et panthéon se calculent en mémoire : zéro lecture en plus.

### Index composites

`firestore.indexes.json` déclare les six index nécessaires (confessions,
rumeurs d'un profil, salon de tag, deux index de groupe de collections pour la
suppression de compte, et un pour lire **tous mes paris en une seule écoute**
plutôt qu'un écouteur par carte visible).

---

## 5. Ce qui reste à faire

1. **Lancer l'app.** Le code **compile** (voir ci-dessous), mais rien n'a encore
   tourné sur un appareil : mise en page Compose, règles Firestore et index
   restent à valider en vrai.
2. Renseigner l'UID admin aux deux endroits cités plus haut.
3. Déployer règles et index.
4. L'onglet 🏆 Légendes classe les rumeurs **de la fenêtre chargée**, pas de toute
   l'histoire. Pour un vrai panthéon : stocker un `legendScore` sur le document et
   trier côté serveur.
5. Reprendre la migration des ViewModels — voir [`docs/attic/README.md`](docs/attic/README.md).
6. Les rumeurs éphémères disparaissent de l'affichage mais restent en base :
   prévoir un nettoyage (Cloud Function ou tâche planifiée).
7. Un pari ne se règle que lorsque son parieur ouvre l'app. Quelqu'un qui ne
   revient jamais garde un pari « en cours » indéfiniment — sans conséquence,
   mais une Cloud Function réglerait ça proprement.
8. L'index de groupe `bets` doit être déployé **avant** que quiconque parie,
   sinon l'écoute échoue en silence et les paris n'apparaissent pas.

---

## 6. Vérifier la compilation sans Gradle

Gradle refuse de démarrer sur cette machine
(`java.io.IOException: Unable to establish loopback connection`, quel que soit
le JDK). Le compilateur Kotlin, lui, tourne très bien si on l'appelle
directement avec les dépendances déjà présentes dans le cache Gradle :

```bash
bash tools/verifier-compilation.sh
```

**Résultat : 1208 classes, 0 erreur** (material3 1.3.1, compose-ui 1.7.5).
Les 39 warnings sont des icônes `Icons.Filled.*` dépréciées au profit des
variantes `AutoMirrored`, plus un `Modifier.menuAnchor()` déprécié dans
`EditProfileScreen` — tous préexistants et sans effet.

### Le faux positif qu'il a fallu corriger

La première version de ce script ramassait **tous** les jars du cache Gradle.
Le cache contenait material3 **1.2.1 et 1.3.1** ; la 1.2.1 sortait en premier,
et du code utilisant `PullToRefreshContainer` — API supprimée en 1.3 —
compilait ici avant d'échouer dans Android Studio.

Un « ✓ » faux est pire que pas de vérification. Le script ne garde donc plus
qu'**une version par artefact**, la plus récente, extraite des AAR versionnés,
et il écarte l'outillage de build du classpath de l'app. Il affiche désormais
les versions retenues à chaque exécution : si le résultat diverge d'Android
Studio, c'est là qu'il faut regarder en premier.

Correctif vérifié en réintroduisant volontairement l'API supprimée : le script
la rejette.

### Migration du pull-to-refresh

`PullToRefreshContainer` et l'ancien `rememberPullToRefreshState()` (avec
`isRefreshing`, `endRefresh()`, `nestedScrollConnection`) n'existent plus en
Material3 1.3. `FeedScreen` et `ConfessionsScreen` utilisent maintenant
`PullToRefreshBox`, qui prend l'état en paramètre et gère le geste,
l'indicateur et le `nestedScroll` lui-même — trois `LaunchedEffect` en moins.

Le script couvre syntaxe, types, imports, signatures et le plugin Compose.
Il ne couvre **pas** les ressources et `R`, la fusion du manifeste, R8, le
packaging APK, ni le comportement à l'exécution — Android Studio reste
nécessaire pour ça.

---

## 7. Configuration des services

Tout ce qui se passe dans les consoles Firebase et Cloudinary — et qui ne peut
pas être fait depuis le dépôt — est listé dans
[`CONFIGURATION.md`](CONFIGURATION.md), avec le test de recette à la fin.

Les **notifications push** sont désormais complètes : `functions/index.js`
écoute la collection `notifications` et envoie le message au bon appareil. Elle
efface les jetons périmés, ignore les comptes bannis, regroupe les messages
d'une même conversation, et laisse passer les annonces obligatoires même si
l'utilisateur a coupé le reste. Reste à la déployer (plan Blaze requis).

Au passage : les trois interrupteurs de notification ne vivaient qu'en local, un
serveur ne pouvait pas les lire. Ils sont maintenant recopiés sur le profil,
donc ils coupent réellement les push.

Le trou restant : **les médias supprimés restent sur Cloudinary**, faute de clé
secrète côté app.

---

## Repères

| Envie | Fichier |
| --- | --- |
| Régler l'équilibrage (XP, clout, cotes, jetons, seuils) | `utils/AskipConstants.kt` |
| Toucher au classement, aux masques, aux tags, à la météo, aux cotes | `utils/RumorEngine.kt` |
| Ajouter ou modifier des missions | `utils/Quests.kt` |
| Changer les couleurs, les thèmes ou l'espacement | `ui/theme/Theme.kt` |
| Modifier la carte de rumeur | `ui/feed/FeedScreen.kt` |
| Modifier le Rumeur-mètre, les réactions, les onglets | `ui/components/RumorComponents.kt` |
| Modifier paris, capsules, chaînes, droit de réponse, météo | `ui/components/RumorLabComponents.kt` |
| Durcir les garanties serveur | `firestore.rules` |
