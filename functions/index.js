/**
 * Askip — envoi des notifications push.
 *
 * L'app écrit déjà un document dans `notifications` à chaque mention, message
 * ou nouvelle rumeur, et enregistre le jeton FCM de chaque appareil sur le
 * profil. Il manquait le maillon du milieu : quelqu'un pour transformer ce
 * document en notification qui arrive **quand l'app est fermée**.
 *
 * C'est ce que fait ce fichier, et rien d'autre.
 *
 * Déploiement :
 *   cd functions && npm install
 *   firebase deploy --only functions
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const { logger } = require("firebase-functions");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

// ⚠️ À ALIGNER SUR L'EMPLACEMENT DE TA BASE FIRESTORE.
// Un déclencheur Firestore doit vivre dans la même région que la base, sinon
// le déploiement est refusé. La région est visible dans la console
// (Firestore → ⚙ → Emplacement de la base de données).
const REGION = "europe-west1";

setGlobalOptions({ region: REGION, maxInstances: 10 });

initializeApp();
const db = getFirestore();

// ── Canaux Android ───────────────────────────────────────────────────────────
// Doivent correspondre exactement à ceux créés dans NotificationHelper.kt :
// c'est le canal qui décide du son, de la vibration et de l'importance.
const CHANNELS = {
  messages: "askip_messages",
  posts: "askip_posts",
  mentions: "askip_mentions",
  admin: "askip_admin",
};

/**
 * Certaines notifications ne se coupent pas.
 *
 * L'app applique déjà cette règle localement (`AppNotification.isMandatory`) :
 * on la répète ici, sinon le serveur enverrait ce que le téléphone a choisi
 * d'ignorer, et inversement.
 */
function isMandatory(type, fromAdmin) {
  return (
    type === "new_post_admin" ||
    type === "mention_everyone" ||
    (type === "mention" && fromAdmin) ||
    (type === "message" && fromAdmin)
  );
}

/** La personne a-t-elle demandé à recevoir ce type de notification ? */
function wantsIt(profile, type, fromAdmin) {
  if (isMandatory(type, fromAdmin)) return true;

  // Champs absents = notification acceptée. Un profil créé avant cette
  // fonction ne doit pas se retrouver muet par accident.
  const on = (field) => profile[field] !== false;

  switch (type) {
    case "message":
      return on("notifyMessages");
    case "mention":
      return on("notifyMentions");
    case "new_post":
      return on("notifyPosts");
    default:
      return true;
  }
}

function channelFor(type, fromAdmin) {
  if (fromAdmin && type !== "new_post") return CHANNELS.admin;
  switch (type) {
    case "new_post":
    case "new_post_admin":
      return type === "new_post_admin" ? CHANNELS.admin : CHANNELS.posts;
    case "mention":
    case "mention_everyone":
      return CHANNELS.mentions;
    default:
      return CHANNELS.messages;
  }
}

/**
 * Le texte affiché dans la barre de notification quand l'app est fermée.
 *
 * Quand l'app est au premier plan, c'est elle qui compose la phrase à partir
 * du pseudo brut (voir `data` plus bas) : les deux formulations coexistent
 * volontairement, chacune pour son contexte.
 */
function trayTitle(type, fromUsername, fromAdmin) {
  const who = fromUsername || "Quelqu'un";
  switch (type) {
    case "mention_everyone":
      return `📢 ${who} s'adresse à tout le campus`;
    case "mention":
      return fromAdmin ? `👑 ${who} t'a mentionné` : `${who} t'a mentionné`;
    case "new_post_admin":
      return `👑 ${who} a publié une annonce`;
    case "new_post":
      return `${who} a lancé une rumeur`;
    default:
      return fromAdmin ? `👑 ${who}` : who;
  }
}

/**
 * Regroupe les notifications d'une même conversation ou d'une même rumeur.
 * Sans ça, dix messages d'affilée empilent dix lignes dans la barre.
 */
function collapseKey(notif) {
  if (notif.conversationId) return `conv_${notif.conversationId}`;
  if (notif.postId) return `post_${notif.postId}`;
  return "askip";
}

/**
 * Oublie un jeton que Firebase déclare mort.
 *
 * Un jeton devient invalide quand l'app est désinstallée ou ses données
 * effacées. Sans ce nettoyage, on réessaierait à chaque notification pour
 * quelqu'un qui n'est plus là.
 */
async function forgetToken(uid, token) {
  const ref = db.collection("profiles").doc(uid);
  const snap = await ref.get();
  if (snap.exists && snap.get("fcmToken") === token) {
    await ref.update({ fcmToken: "" });
    logger.info(`Jeton périmé retiré du profil ${uid}`);
  }
}

exports.sendPushNotification = onDocumentCreated(
  "notifications/{notifId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return;

    const notif = snap.data();
    const targetUid = notif.targetUserId;
    const type = notif.type || "message";
    const fromAdmin = notif.fromIsAdmin === true;

    if (!targetUid) return;

    // On ne se notifie pas soi-même. L'app filtre déjà l'expéditeur lors des
    // envois groupés, mais une garde ici coûte moins qu'un bug visible.
    if (targetUid === notif.fromUserId) return;

    const profileSnap = await db.collection("profiles").doc(targetUid).get();
    if (!profileSnap.exists) return;

    const profile = profileSnap.data();
    if (profile.isBanned === true) return;

    const token = profile.fcmToken;
    if (!token) return; // Jamais connecté depuis la mise à jour, ou désinstallé.

    if (!wantsIt(profile, type, fromAdmin)) return;

    const body = (notif.content || "").slice(0, 200);
    const fromUsername = notif.fromUsername || "Quelqu'un";

    const message = {
      token,

      // Affiché par le système quand l'app est fermée ou en arrière-plan.
      notification: {
        title: trayTitle(type, fromUsername, fromAdmin),
        body,
      },

      // Lu par AskipMessagingService quand l'app est au premier plan.
      // `title` y est le pseudo brut : c'est l'app qui compose la phrase et
      // choisit le style, doré pour l'administration.
      data: {
        title: fromUsername,
        body,
        type,
        fromUserId: notif.fromUserId || "",
        fromIsAdmin: String(fromAdmin),
        postId: notif.postId || "",
        conversationId: notif.conversationId || "",
      },

      android: {
        priority: isMandatory(type, fromAdmin) ? "high" : "normal",
        collapseKey: collapseKey(notif),
        notification: {
          channelId: channelFor(type, fromAdmin),
          // Le badge se met à jour tout seul depuis l'app : inutile ici.
          sound: "default",
        },
      },
    };

    try {
      await getMessaging().send(message);
    } catch (err) {
      const code = err.errorInfo?.code || err.code || "";
      if (
        code.includes("registration-token-not-registered") ||
        code.includes("invalid-argument") ||
        code.includes("invalid-registration-token")
      ) {
        await forgetToken(targetUid, token);
        return;
      }
      // Toute autre erreur est réelle : on la remonte pour la voir dans les logs.
      logger.error(`Envoi échoué vers ${targetUid}`, err);
      throw err;
    }
  }
);
