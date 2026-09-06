/**
 * Banc d'essai des règles Firestore.
 *
 * Les règles sont la seule barrière entre les données de l'app et n'importe
 * qui : elles méritent d'être vérifiées autrement qu'à la lecture. Chaque cas
 * ci-dessous rejoue une écriture que l'app fait vraiment, ou une écriture
 * qu'elle ne fera jamais et qui doit être refusée.
 *
 *   bash tools/regles-firestore/lancer.sh
 *
 * Tout tourne dans l'émulateur local : aucun accès au vrai projet, aucune
 * donnée réelle touchée.
 */

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} from '@firebase/rules-unit-testing';
import {
  doc, getDoc, setDoc, updateDoc, deleteDoc,
  collection, collectionGroup, query, where, getDocs,
  arrayUnion, arrayRemove, increment, deleteField,
} from 'firebase/firestore';

const ICI = dirname(fileURLToPath(import.meta.url));
const REGLES = join(ICI, '..', '..', 'firestore.rules');

// L'UID inscrit en dur dans les règles. Le changer ici sans le changer
// là-bas ferait passer les tests sur une app qui ne marche pas.
const ADMIN = 'ckCTisQMWKWbnoElnUjO2x4vKxy2';

const env = await initializeTestEnvironment({
  projectId: 'demo-askip-regles',
  firestore: {
    rules: readFileSync(REGLES, 'utf8'),
    host: '127.0.0.1',
    port: 8080,
  },
});

// ── Petit harnais ───────────────────────────────────────────────────────────

const cas = [];
/** @param {string} section @param {string} nom @param {() => Promise<void>} fn */
const test = (section, nom, fn) => cas.push({ section, nom, fn });

const db = (uid) => env.authenticatedContext(uid).firestore();
const anonyme = () => env.unauthenticatedContext().firestore();

/** Écrit sans passer par les règles, pour préparer le terrain. */
const semer = (fn) => env.withSecurityRulesDisabled((ctx) => fn(ctx.firestore()));

// ── Jeux de données ─────────────────────────────────────────────────────────

const profil = (uid, extra = {}) => ({
  userId: uid, username: uid, age: 0, bio: '',
  classeENI: '', relationshipStatus: '', photoUrl: '', coverUrl: '',
  themeColor: '#7C4DFF', avatarFrame: 'none', moodEmoji: '', moodText: '',
  badgeIds: [], customBadgeName: '', customBadgeColor: '#7C4DFF',
  postsCount: 0, commentsCount: 0, confessionsCount: 0, storiesCount: 0,
  pollsCount: 0, convsStarted: 0, streak: 0, bestStreak: 0, lastActiveDate: '',
  clout: 0, xp: 0, confirmedRumors: 0, debunkedRumors: 0,
  lastScoopDate: '', lastBetDate: '', betsToday: 0, betsWon: 0, betsLost: 0,
  fcmToken: '', notifyMessages: true, notifyMentions: true, notifyPosts: true,
  hasBadgeENI: false, isAdmin: false, isBanned: false,
  ...extra,
});

/** Exactement la charge utile de `createPostWithMedia`. */
const rumeur = (uid, extra = {}) => ({
  userId: uid, username: uid, userPhotoUrl: '',
  content: 'Askip il se passe un truc', postType: 'normal', isAnonymous: false,
  imageUrl: '', videoUrl: '', audioUrl: '', audioDuration: 0,
  fileUrl: '', fileName: '',
  pollOption1: '', pollOption2: '', pollVotes1: 0, pollVotes2: 0, pollVoters: [],
  likedBy: [], fireBy: [], lolBy: [], shockBy: [], eyesBy: [],
  credibleBy: [], fakeBy: [], scoopBy: [],
  tags: [], commentCount: 0, isPinned: false, isEdited: false, expiresAt: 0,
  sealedUntil: 0, keysNeeded: 0, keysBy: [],
  chainCount: 0, chainAuthors: [], chainLastContent: '', chainLastAuthor: '',
  repliedBy: [], timestamp: Date.now(),
  ...extra,
});

const message = (uid, extra = {}) => ({
  id: 'm1', senderId: uid, senderUsername: uid, content: 'salut',
  mediaUrl: '', mediaType: '', mediaName: '', timestamp: Date.now(), ...extra,
});

// ════════════════════════════════════════════════════════════════════════════
//  Profils
// ════════════════════════════════════════════════════════════════════════════

test('Profils', "on crée son propre profil", async () => {
  await assertSucceeds(setDoc(doc(db('alice'), 'profiles/alice'), profil('alice')));
});

test('Profils', "on ne crée pas celui d'un autre", async () => {
  await assertFails(setDoc(doc(db('alice'), 'profiles/bob'), profil('bob')));
});

test('Profils', "on ne se déclare pas admin", async () => {
  await assertFails(
    setDoc(doc(db('alice'), 'profiles/alice'), profil('alice', { isAdmin: true })));
});

test('Profils', "on modifie sa bio", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertSucceeds(updateDoc(doc(db('alice'), 'profiles/alice'), { bio: 'coucou' }));
});

test('Profils', "on ne se donne pas isAdmin après coup", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertFails(updateDoc(doc(db('alice'), 'profiles/alice'), { isAdmin: true }));
});

test('Profils', "badge ENI accordé avec une classe", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertSucceeds(updateDoc(doc(db('alice'), 'profiles/alice'),
    { classeENI: 'IG 3ème année', hasBadgeENI: true }));
});

test('Profils', "badge ENI refusé sans classe", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertFails(updateDoc(doc(db('alice'), 'profiles/alice'), { hasBadgeENI: true }));
});

test('Profils', "un tiers ajoute du clout", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice')),
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
  ]));
  await assertSucceeds(updateDoc(doc(db('bob'), 'profiles/alice'), { clout: increment(5) }));
});

test('Profils', "un tiers ne touche pas au pseudo", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice')),
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
  ]));
  await assertFails(updateDoc(doc(db('bob'), 'profiles/alice'), { username: 'piraté' }));
});

test('Profils', "un tiers RETIRE un badge supprimé", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice', { badgeIds: ['b1', 'b2'] })),
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
  ]));
  await assertSucceeds(
    updateDoc(doc(db('bob'), 'profiles/alice'), { badgeIds: arrayRemove('b1') }));
});

test('Profils', "un tiers n'AJOUTE pas un badge", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice', { badgeIds: [] })),
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
  ]));
  await assertFails(
    updateDoc(doc(db('bob'), 'profiles/alice'), { badgeIds: arrayUnion('admin') }));
});

// Le piège qui a bloqué le compte le plus ancien de l'app.
//
// `isBanned` n'a été ajouté à `createDefaultProfile` qu'en cours de route :
// les profils d'avant ne l'ont pas. Lire `.data.isBanned` sur un champ absent
// ne renvoie pas faux, ça fait ÉCHOUER la règle — donc `canWrite()` échoue,
// donc toute écriture est refusée. Les lectures, elles, continuaient de
// passer : d'où un fil qui s'affiche et un bouton « Publier » sans effet.
test('Profils', "un profil ancien, sans le champ isBanned, écrit quand même", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/ancien'), {
    userId: 'ancien', username: 'ancien', bio: '', clout: 0, xp: 0,
    // ni isBanned, ni isAdmin : le profil est d'avant ces champs
  }));
  await assertSucceeds(setDoc(doc(db('ancien'), 'posts/p1'), rumeur('ancien')));
});

test('Profils', "un banni n'écrit plus", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/mallory'), profil('mallory', { isBanned: true })));
  await assertFails(
    setDoc(doc(db('mallory'), 'posts/p9'), rumeur('mallory')));
});

test('Profils', "l'admin bannit", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice')),
    setDoc(doc(d, `profiles/${ADMIN}`), profil(ADMIN)),
  ]));
  await assertSucceeds(updateDoc(doc(db(ADMIN), 'profiles/alice'), { isBanned: true }));
});

// ════════════════════════════════════════════════════════════════════════════
//  Rumeurs
// ════════════════════════════════════════════════════════════════════════════

test('Rumeurs', "on publie la sienne", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertSucceeds(setDoc(doc(db('alice'), 'posts/p1'), rumeur('alice')));
});

test('Rumeurs', "on ne publie pas au nom d'un autre", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertFails(setDoc(doc(db('alice'), 'posts/p1'), rumeur('bob')));
});

test('Rumeurs', "une rumeur neuve n'arrive pas avec des votes", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertFails(
    setDoc(doc(db('alice'), 'posts/p1'), rumeur('alice', { credibleBy: ['x', 'y', 'z'] })));
});

test('Rumeurs', "l'auteur corrige son texte", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertSucceeds(updateDoc(doc(db('alice'), 'posts/p1'),
    { content: 'Askip autre chose', tags: ['drama'], isEdited: true }));
});

test('Rumeurs', "l'auteur répercute son nouveau pseudo  [correctif]", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertSucceeds(updateDoc(doc(db('alice'), 'posts/p1'), { username: 'alice2' }));
});

test('Rumeurs', "un tiers ne corrige pas le texte", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertFails(updateDoc(doc(db('bob'), 'posts/p1'), { content: 'faux' }));
});

test('Rumeurs', "un tiers ne renomme pas l'auteur", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertFails(updateDoc(doc(db('bob'), 'posts/p1'), { username: 'sale type' }));
});

test('Rumeurs', "réagir en s'ajoutant soi-même", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertSucceeds(updateDoc(doc(db('bob'), 'posts/p1'), { likedBy: arrayUnion('bob') }));
});

test('Rumeurs', "réagir au nom d'un autre est refusé", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertFails(updateDoc(doc(db('bob'), 'posts/p1'), { likedBy: arrayUnion('carole') }));
});

test('Rumeurs', "trancher : crédible", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertSucceeds(updateDoc(doc(db('bob'), 'posts/p1'), {
    credibleBy: arrayUnion('bob'), fakeBy: arrayRemove('bob'),
  }));
});

test('Rumeurs', "épingler est refusé au commun", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertFails(updateDoc(doc(db('bob'), 'posts/p1'), { isPinned: true }));
});

test('Rumeurs', "l'admin épingle", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertSucceeds(updateDoc(doc(db(ADMIN), 'posts/p1'), { isPinned: true }));
});

test('Rumeurs', "commentCount monte d'un cran", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertSucceeds(
    updateDoc(doc(db('bob'), 'posts/p1'), { commentCount: increment(1) }));
});

test('Rumeurs', "commentCount ne bondit pas de 50  [durcissement]", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertFails(
    updateDoc(doc(db('bob'), 'posts/p1'), { commentCount: increment(50) }));
});

test('Rumeurs', "on vote au sondage", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice', { postType: 'poll' })));
  await assertSucceeds(updateDoc(doc(db('bob'), 'posts/p1'), {
    pollVoters: arrayUnion('bob'), pollVotes1: increment(1),
  }));
});

test('Rumeurs', "on ne vote pas deux fois  [durcissement]", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'),
    rumeur('alice', { postType: 'poll', pollVoters: ['bob'], pollVotes1: 1 })));
  await assertFails(updateDoc(doc(db('bob'), 'posts/p1'), {
    pollVoters: arrayUnion('bob'), pollVotes1: increment(1),
  }));
});

test('Rumeurs', "l'auteur supprime la sienne", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertSucceeds(deleteDoc(doc(db('alice'), 'posts/p1')));
});

test('Rumeurs', "un tiers ne supprime pas", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertFails(deleteDoc(doc(db('bob'), 'posts/p1')));
});

// ════════════════════════════════════════════════════════════════════════════
//  Commentaires
// ════════════════════════════════════════════════════════════════════════════

const commentaire = (uid) => ({
  id: 'c1', userId: uid, username: uid, userPhotoUrl: '',
  content: 'bien vu', isAnonymous: false, likedBy: [], timestamp: Date.now(),
});

test('Commentaires', "on commente", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertSucceeds(
    setDoc(doc(db('bob'), 'posts/p1/comments/c1'), commentaire('bob')));
});

test('Commentaires', "on aime celui d'un autre", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/comments/c1'), commentaire('bob')),
  ]));
  await assertSucceeds(updateDoc(doc(db('carole'), 'posts/p1/comments/c1'),
    { likedBy: arrayUnion('carole') }));
});

test('Commentaires', "on ne réécrit pas celui d'un autre", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/comments/c1'), commentaire('bob')),
  ]));
  await assertFails(updateDoc(doc(db('carole'), 'posts/p1/comments/c1'),
    { content: 'propos inventés' }));
});

test('Commentaires', "l'auteur de la rumeur fait le ménage chez lui  [correctif]", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/comments/c1'), commentaire('bob')),
  ]));
  await assertSucceeds(deleteDoc(doc(db('alice'), 'posts/p1/comments/c1')));
});

test('Commentaires', "un passant n'efface pas celui d'un autre", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/comments/c1'), commentaire('bob')),
  ]));
  await assertFails(deleteDoc(doc(db('carole'), 'posts/p1/comments/c1')));
});

// ════════════════════════════════════════════════════════════════════════════
//  Capsule scellée
// ════════════════════════════════════════════════════════════════════════════

const capsule = (extra = {}) => ({
  authorId: 'alice', postId: 'p1', content: 'le secret', imageUrl: '',
  unsealAt: Date.now() + 3_600_000, keysNeeded: 10, ...extra,
});

test('Capsule', "avant l'heure, personne ne lit", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice', { postType: 'sealed' })),
    setDoc(doc(d, 'posts/p1/sealed/payload'), capsule()),
  ]));
  await assertFails(getDoc(doc(db('bob'), 'posts/p1/sealed/payload')));
});

test('Capsule', "l'auteur relit la sienne", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice', { postType: 'sealed' })),
    setDoc(doc(d, 'posts/p1/sealed/payload'), capsule()),
  ]));
  await assertSucceeds(getDoc(doc(db('alice'), 'posts/p1/sealed/payload')));
});

test('Capsule', "l'heure passée, tout le monde lit", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice', { postType: 'sealed' })),
    setDoc(doc(d, 'posts/p1/sealed/payload'), capsule({ unsealAt: Date.now() - 1000 })),
  ]));
  await assertSucceeds(getDoc(doc(db('bob'), 'posts/p1/sealed/payload')));
});

test('Capsule', "assez de clés, elle s'ouvre", async () => {
  const dix = ['u1','u2','u3','u4','u5','u6','u7','u8','u9','u10'];
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice', { postType: 'sealed', keysBy: dix })),
    setDoc(doc(d, 'posts/p1/sealed/payload'), capsule()),
  ]));
  await assertSucceeds(getDoc(doc(db('bob'), 'posts/p1/sealed/payload')));
});

test('Capsule', "une capsule ne se réécrit pas", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice', { postType: 'sealed' })),
    setDoc(doc(d, 'posts/p1/sealed/payload'), capsule({ unsealAt: Date.now() - 1000 })),
  ]));
  await assertFails(
    updateDoc(doc(db('alice'), 'posts/p1/sealed/payload'), { content: 'changé' }));
});

// ════════════════════════════════════════════════════════════════════════════
//  Téléphone arabe
// ════════════════════════════════════════════════════════════════════════════

const maillon = (uid) => ({
  id: 'l1', userId: uid, username: uid, content: 'et puis…', timestamp: Date.now(),
});

test('Chaîne', "on ajoute un maillon", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'),
    rumeur('alice', { postType: 'chain', chainCount: 1, chainAuthors: ['alice'] })));
  await assertSucceeds(setDoc(doc(db('bob'), 'posts/p1/links/l1'), maillon('bob')));
});

test('Chaîne', "un seul maillon par personne", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'),
    rumeur('alice', { postType: 'chain', chainCount: 2, chainAuthors: ['alice', 'bob'] })));
  await assertFails(setDoc(doc(db('bob'), 'posts/p1/links/l2'), maillon('bob')));
});

test('Chaîne', "sept maillons et pas un de plus", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice', {
    postType: 'chain', chainCount: 7,
    chainAuthors: ['a','b','c','d','e','f','g'],
  })));
  await assertFails(setDoc(doc(db('bob'), 'posts/p1/links/l8'), maillon('bob')));
});

// ════════════════════════════════════════════════════════════════════════════
//  Paris
// ════════════════════════════════════════════════════════════════════════════

const pari = (uid, extra = {}) => ({
  userId: uid, postId: 'p1', onCredible: true, stake: 2,
  ratioAtBet: 0.5, votesAtBet: 8, odds: 1.8,
  settled: false, won: false, payout: 0, timestamp: Date.now(), ...extra,
});

test('Paris', "on parie pour soi", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
  ]));
  await assertSucceeds(setDoc(doc(db('bob'), 'posts/p1/bets/bob'), pari('bob')));
});

test('Paris', "on ne parie pas pour un autre", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertFails(setDoc(doc(db('bob'), 'posts/p1/bets/carole'), pari('carole')));
});

test('Paris', "la mise plafonne à trois jetons", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertFails(setDoc(doc(db('bob'), 'posts/p1/bets/bob'), pari('bob', { stake: 9 })));
});

test('Paris', "on règle son pari une fois", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/bets/bob'), pari('bob')),
  ]));
  await assertSucceeds(updateDoc(doc(db('bob'), 'posts/p1/bets/bob'),
    { settled: true, won: true, payout: 36 }));
});

test('Paris', "un pari réglé ne se rejoue pas", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/bets/bob'), pari('bob', { settled: true })),
  ]));
  await assertFails(updateDoc(doc(db('bob'), 'posts/p1/bets/bob'), { payout: 9999 }));
});

// ════════════════════════════════════════════════════════════════════════════
//  Droit de réponse
// ════════════════════════════════════════════════════════════════════════════

const reponse = (uid) => ({
  userId: uid, username: uid, userPhotoUrl: '', postId: 'p1',
  content: 'ce n\'est pas moi', timestamp: Date.now(),
});

test('Réponse', "on pose la sienne", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertSucceeds(setDoc(doc(db('bob'), 'posts/p1/replies/bob'), reponse('bob')));
});

test('Réponse', "on ne pose pas celle d'un autre", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertFails(setDoc(doc(db('bob'), 'posts/p1/replies/carole'), reponse('carole')));
});

test('Réponse', "on retire la sienne  [correctif]", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/replies/bob'), reponse('bob')),
  ]));
  await assertSucceeds(deleteDoc(doc(db('bob'), 'posts/p1/replies/bob')));
});

test('Réponse', "on ne retire pas celle d'un autre", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/replies/bob'), reponse('bob')),
  ]));
  await assertFails(deleteDoc(doc(db('carole'), 'posts/p1/replies/bob')));
});

// ════════════════════════════════════════════════════════════════════════════
//  Stories
// ════════════════════════════════════════════════════════════════════════════

const story = (uid) => ({
  id: 's1', userId: uid, username: uid, userPhotoUrl: '',
  content: 'ma journée', emoji: 'star', backgroundColor: '#7C3AED',
  timestamp: Date.now(), expiresAt: Date.now() + 86_400_000,
});

test('Stories', "on publie la sienne", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertSucceeds(setDoc(doc(db('alice'), 'stories/s1'), story('alice')));
});

test('Stories', "l'auteur répercute son pseudo  [correctif]", async () => {
  await semer((d) => setDoc(doc(d, 'stories/s1'), story('alice')));
  await assertSucceeds(updateDoc(doc(db('alice'), 'stories/s1'), { username: 'alice2' }));
});

test('Stories', "un tiers ne renomme pas", async () => {
  await semer((d) => setDoc(doc(d, 'stories/s1'), story('alice')));
  await assertFails(updateDoc(doc(db('bob'), 'stories/s1'), { username: 'moqué' }));
});

// ════════════════════════════════════════════════════════════════════════════
//  Conversations
// ════════════════════════════════════════════════════════════════════════════

const conv = { id: 'alice_bob', participants: ['alice', 'bob'],
  participantNames: { alice: 'alice', bob: 'bob' },
  lastMessage: '', lastSenderId: '', lastTimestamp: 0,
  unreadCounts: { alice: 0, bob: 0 } };

test('Conversations', "on lit une conversation qui n'existe pas encore  [correctif]", async () => {
  await assertSucceeds(getDoc(doc(db('alice'), 'conversations/alice_bob')));
});

test('Conversations', "on la crée", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertSucceeds(setDoc(doc(db('alice'), 'conversations/alice_bob'), conv));
});

test('Conversations', "un étranger ne lit pas celle des autres", async () => {
  await semer((d) => setDoc(doc(d, 'conversations/alice_bob'), conv));
  await assertFails(getDoc(doc(db('mallory'), 'conversations/alice_bob')));
});

test('Conversations', "un participant met à jour l'aperçu", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice')),
    setDoc(doc(d, 'conversations/alice_bob'), conv),
  ]));
  await assertSucceeds(updateDoc(doc(db('alice'), 'conversations/alice_bob'), {
    lastMessage: 'salut', lastSenderId: 'alice', lastTimestamp: Date.now(),
    'unreadCounts.bob': increment(1),
  }));
});

test('Conversations', "on ne réécrit pas la liste des présents  [durcissement]", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice')),
    setDoc(doc(d, 'conversations/alice_bob'), conv),
  ]));
  await assertFails(updateDoc(doc(db('alice'), 'conversations/alice_bob'),
    { participants: ['alice'] }));
});

// ════════════════════════════════════════════════════════════════════════════
//  Messages privés
// ════════════════════════════════════════════════════════════════════════════

test('Messages', "un participant envoie", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice')),
    setDoc(doc(d, 'conversations/alice_bob'), conv),
  ]));
  await assertSucceeds(
    setDoc(doc(db('alice'), 'conversations/alice_bob/messages/m1'), message('alice')));
});

test('Messages', "un étranger n'envoie pas", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/mallory'), profil('mallory')),
    setDoc(doc(d, 'conversations/alice_bob'), conv),
  ]));
  await assertFails(
    setDoc(doc(db('mallory'), 'conversations/alice_bob/messages/m2'), message('mallory')));
});

test('Messages', "on réagit au message d'un autre  [bug signalé]", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
    setDoc(doc(d, 'conversations/alice_bob'), conv),
    setDoc(doc(d, 'conversations/alice_bob/messages/m1'), message('alice')),
  ]));
  await assertSucceeds(updateDoc(
    doc(db('bob'), 'conversations/alice_bob/messages/m1'), { 'reactions.bob': 'love' }));
});

test('Messages', "on retire sa réaction, la dernière du lot", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
    setDoc(doc(d, 'conversations/alice_bob'), conv),
    setDoc(doc(d, 'conversations/alice_bob/messages/m1'),
      message('alice', { reactions: { bob: 'love' } })),
  ]));
  await assertSucceeds(updateDoc(
    doc(db('bob'), 'conversations/alice_bob/messages/m1'),
    { 'reactions.bob': deleteField() }));
});

test('Messages', "on ne touche pas à la réaction d'un autre", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
    setDoc(doc(d, 'conversations/alice_bob'), conv),
    setDoc(doc(d, 'conversations/alice_bob/messages/m1'),
      message('alice', { reactions: { alice: 'fire' } })),
  ]));
  await assertFails(updateDoc(
    doc(db('bob'), 'conversations/alice_bob/messages/m1'),
    { 'reactions.alice': deleteField() }));
});

test('Messages', "un pseudo renommé se répercute", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice')),
    setDoc(doc(d, 'conversations/alice_bob'), conv),
    setDoc(doc(d, 'conversations/alice_bob/messages/m1'), message('alice')),
  ]));
  await assertSucceeds(updateDoc(
    doc(db('alice'), 'conversations/alice_bob/messages/m1'),
    { senderUsername: 'alice2' }));
});

test('Messages', "on n'en réécrit pas le contenu", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
    setDoc(doc(d, 'conversations/alice_bob'), conv),
    setDoc(doc(d, 'conversations/alice_bob/messages/m1'), message('alice')),
  ]));
  await assertFails(updateDoc(
    doc(db('bob'), 'conversations/alice_bob/messages/m1'), { content: 'propos inventés' }));
});

// ════════════════════════════════════════════════════════════════════════════
//  Groupes
// ════════════════════════════════════════════════════════════════════════════

const groupe = { id: 'g1', name: 'Les IG3', description: '', emoji: 'veil',
  createdBy: 'alice', createdByUsername: 'alice',
  members: ['alice', 'bob'], memberNames: { alice: 'alice', bob: 'bob' },
  memberPhotos: {}, lastMessage: '', lastSenderId: '', lastSenderUsername: '',
  lastTimestamp: 0, timestamp: Date.now() };

test('Groupes', "on crée un groupe dont on fait partie", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertSucceeds(setDoc(doc(db('alice'), 'groups/g1'), groupe));
});

test('Groupes', "un membre quitte le groupe", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
    setDoc(doc(d, 'groups/g1'), groupe),
  ]));
  await assertSucceeds(
    updateDoc(doc(db('bob'), 'groups/g1'), { members: arrayRemove('bob') }));
});

test('Groupes', "un membre ne se déclare pas fondateur  [durcissement]", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
    setDoc(doc(d, 'groups/g1'), groupe),
  ]));
  await assertFails(updateDoc(doc(db('bob'), 'groups/g1'), { createdBy: 'bob' }));
});

test('Groupes', "un non-membre ne lit pas les messages", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'groups/g1'), groupe),
    setDoc(doc(d, 'groups/g1/messages/m1'), message('alice')),
  ]));
  await assertFails(getDoc(doc(db('mallory'), 'groups/g1/messages/m1')));
});

test('Groupes', "un membre réagit à un message", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
    setDoc(doc(d, 'groups/g1'), groupe),
    setDoc(doc(d, 'groups/g1/messages/m1'), message('alice')),
  ]));
  await assertSucceeds(
    updateDoc(doc(db('bob'), 'groups/g1/messages/m1'), { 'reactions.bob': 'fire' }));
});

// ════════════════════════════════════════════════════════════════════════════
//  Notifications
// ════════════════════════════════════════════════════════════════════════════

const notif = (extra = {}) => ({
  id: 'n1', type: 'mention', fromUserId: 'alice', fromUsername: 'alice',
  fromIsAdmin: false, targetUserId: 'bob', postId: 'p1', conversationId: '',
  content: 'coucou', isRead: false, timestamp: Date.now(), ...extra,
});

test('Notifications', "on en crée une pour quelqu'un", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertSucceeds(setDoc(doc(db('alice'), 'notifications/n1'), notif()));
});

test('Notifications', "on n'en crée pas au nom d'un autre", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/mallory'), profil('mallory')));
  await assertFails(
    setDoc(doc(db('mallory'), 'notifications/n1'), notif({ fromUserId: 'alice' })));
});

test('Notifications', "le destinataire marque comme lu  [champ isRead]", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/bob'), profil('bob')),
    setDoc(doc(d, 'notifications/n1'), notif()),
  ]));
  await assertSucceeds(updateDoc(doc(db('bob'), 'notifications/n1'), { isRead: true }));
});

test('Notifications', "un passant ne marque pas", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/mallory'), profil('mallory')),
    setDoc(doc(d, 'notifications/n1'), notif()),
  ]));
  await assertFails(updateDoc(doc(db('mallory'), 'notifications/n1'), { isRead: true }));
});

test('Notifications', "l'expéditeur relit ce qu'il a envoyé  [correctif]", async () => {
  await semer((d) => setDoc(doc(d, 'notifications/n1'), notif()));
  await assertSucceeds(getDocs(query(
    collection(db('alice'), 'notifications'), where('fromUserId', '==', 'alice'))));
});

test('Notifications', "l'expéditeur corrige son pseudo  [correctif]", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice')),
    setDoc(doc(d, 'notifications/n1'), notif()),
  ]));
  await assertSucceeds(
    updateDoc(doc(db('alice'), 'notifications/n1'), { fromUsername: 'alice2' }));
});

test('Notifications', "l'expéditeur n'en change pas le texte", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'profiles/alice'), profil('alice')),
    setDoc(doc(d, 'notifications/n1'), notif()),
  ]));
  await assertFails(
    updateDoc(doc(db('alice'), 'notifications/n1'), { content: 'autre chose' }));
});

test('Notifications', "on ne lit pas celles des autres", async () => {
  await semer((d) => setDoc(doc(d, 'notifications/n1'), notif()));
  await assertFails(getDocs(query(
    collection(db('mallory'), 'notifications'), where('targetUserId', '==', 'bob'))));
});

// ════════════════════════════════════════════════════════════════════════════
//  Requêtes de groupe de collections
// ════════════════════════════════════════════════════════════════════════════

test('Groupe de collections', "mes commentaires, partout  [correctif]", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/comments/c1'), commentaire('bob')),
  ]));
  await assertSucceeds(getDocs(query(
    collectionGroup(db('bob'), 'comments'), where('userId', '==', 'bob'))));
});

test('Groupe de collections', "pas ceux des autres", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/comments/c1'), commentaire('bob')),
  ]));
  await assertFails(getDocs(query(
    collectionGroup(db('mallory'), 'comments'), where('userId', '==', 'bob'))));
});

test('Groupe de collections', "mes messages, privés et de groupe  [correctif]", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'conversations/alice_bob'), conv),
    setDoc(doc(d, 'conversations/alice_bob/messages/m1'), message('alice')),
    setDoc(doc(d, 'groups/g1'), groupe),
    setDoc(doc(d, 'groups/g1/messages/m2'), message('alice', { id: 'm2' })),
  ]));
  const snap = await assertSucceeds(getDocs(query(
    collectionGroup(db('alice'), 'messages'), where('senderId', '==', 'alice'))));
  if (snap.size !== 2) throw new Error(`2 messages attendus, ${snap.size} trouvés`);
});

test('Groupe de collections', "pas ceux des autres", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'conversations/alice_bob'), conv),
    setDoc(doc(d, 'conversations/alice_bob/messages/m1'), message('alice')),
  ]));
  await assertFails(getDocs(query(
    collectionGroup(db('mallory'), 'messages'), where('senderId', '==', 'alice'))));
});

test('Groupe de collections', "mes paris, toutes rumeurs confondues", async () => {
  await semer((d) => Promise.all([
    setDoc(doc(d, 'posts/p1'), rumeur('alice')),
    setDoc(doc(d, 'posts/p1/bets/bob'), pari('bob')),
  ]));
  await assertSucceeds(getDocs(query(
    collectionGroup(db('bob'), 'bets'), where('userId', '==', 'bob'))));
});

// ════════════════════════════════════════════════════════════════════════════
//  Badges, signalements, et le reste
// ════════════════════════════════════════════════════════════════════════════

test('Badges', "on crée son badge", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/alice'), profil('alice')));
  await assertSucceeds(setDoc(doc(db('alice'), 'badges/b1'), {
    id: 'b1', name: 'ig3', displayName: 'IG3', colorHex: '#7C4DFF',
    createdBy: 'alice', createdAt: Date.now(),
  }));
});

test('Badges', "« admin » est réservé", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/mallory'), profil('mallory')));
  await assertFails(setDoc(doc(db('mallory'), 'badges/b2'), {
    id: 'b2', name: 'admin', displayName: 'Admin', colorHex: '#000',
    createdBy: 'mallory', createdAt: Date.now(),
  }));
});

test('Signalements', "on signale", async () => {
  await semer((d) => setDoc(doc(d, 'profiles/bob'), profil('bob')));
  await assertSucceeds(setDoc(doc(db('bob'), 'reports/r1'), {
    id: 'r1', postId: 'p1', reporterId: 'bob', reason: 'harcèlement',
    details: '', handled: false, timestamp: Date.now(),
  }));
});

test('Signalements', "personne ne lit la pile, sauf l'admin", async () => {
  await semer((d) => setDoc(doc(d, 'reports/r1'), { reporterId: 'bob' }));
  await assertFails(getDoc(doc(db('bob'), 'reports/r1')));
  await assertSucceeds(getDoc(doc(db(ADMIN), 'reports/r1')));
});

test('Divers', "déconnecté, on ne lit rien", async () => {
  await semer((d) => setDoc(doc(d, 'posts/p1'), rumeur('alice')));
  await assertFails(getDoc(doc(anonyme(), 'posts/p1')));
});

test('Divers', "une collection inconnue est fermée", async () => {
  await assertFails(setDoc(doc(db('mallory'), 'nimporte/quoi'), { x: 1 }));
});

// ── Exécution ───────────────────────────────────────────────────────────────

let reussis = 0;
const echecs = [];
let sectionCourante = '';

for (const { section, nom, fn } of cas) {
  if (section !== sectionCourante) {
    sectionCourante = section;
    console.log(`\n  ${section}`);
  }
  await env.clearFirestore();
  try {
    await fn();
    reussis++;
    console.log(`    OK    ${nom}`);
  } catch (e) {
    echecs.push({ section, nom, e });
    console.log(`    ECHEC ${nom}`);
  }
}

console.log(`\n${'─'.repeat(72)}`);
if (echecs.length === 0) {
  console.log(`  ${reussis} / ${cas.length} cas passent. Les règles font ce qu'elles disent.`);
} else {
  console.log(`  ${reussis} / ${cas.length} cas passent. ${echecs.length} en échec :\n`);
  for (const { section, nom, e } of echecs) {
    console.log(`  · [${section}] ${nom}`);
    console.log(`      ${String(e.message).split('\n')[0].slice(0, 160)}`);
  }
}
console.log('');

await env.cleanup();
process.exit(echecs.length === 0 ? 0 : 1);
