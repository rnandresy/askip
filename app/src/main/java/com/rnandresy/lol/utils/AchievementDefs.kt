package com.rnandresy.lol.utils

data class AchievementDef(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val color: String,
    val rarity: String   // "commun" | "rare" | "épique" | "légendaire"
)

val ALL_ACHIEVEMENTS = listOf(
    // ── Poster ────────────────────────────────────────────────────────────────
    AchievementDef("first_post",     "📢", "Première Rumeur",     "Tu as lancé ta première rumeur sur le campus !",     "#E91E63", "commun"),
    AchievementDef("ten_posts",      "🎤", "Ragoteur Confirmé",   "10 posts. T'as des choses à dire, et c'est cool.",   "#9C27B0", "rare"),
    AchievementDef("twenty_five_p",  "📣", "Plume d'Or",          "25 posts. Tu animes le campus, continue !",          "#3F51B5", "épique"),
    AchievementDef("fifty_posts",    "🗣️","Grande Gueule",        "50 posts. La parole est libre, profites-en !",       "#5C6BC0", "légendaire"),

    // ── Commenter ─────────────────────────────────────────────────────────────
    AchievementDef("first_comment",  "💬", "Premier Mot",         "Tu as laissé ton premier commentaire.",              "#2196F3", "commun"),
    AchievementDef("commentator",    "🗨️","Bavard Sympa",         "20 commentaires. Tu gardes la conversation vivante.","#1976D2", "rare"),
    AchievementDef("deep_comment",   "🧠", "Penseur du Campus",   "50 commentaires. Tu as toujours quelque chose à dire.","#0D47A1","épique"),

    // ── Confession ────────────────────────────────────────────────────────────
    AchievementDef("confessor",      "🎭", "L'Anonyme",           "Tu as publié ta première confession. Courageux(se) !","#607D8B","commun"),
    AchievementDef("dark_confessor", "🕵️","Agent Double",         "5 confessions. Qui se cache derrière ce masque ?",   "#37474F", "rare"),

    // ── Sondage ───────────────────────────────────────────────────────────────
    AchievementDef("poll_creator",   "📊", "Sondeur",             "3 sondages créés. Tu veux savoir ce que pensent tes collègues !","#009688","commun"),
    AchievementDef("poll_master",    "🗳️","Démocrate du Campus",  "10 sondages. La parole est à tout le monde grâce à toi.","#00695C","rare"),

    // ── Story ─────────────────────────────────────────────────────────────────
    AchievementDef("first_story",    "📖", "Premier Épisode",     "Ta première story de 24h. Raconte ta vie !",          "#FF7043", "commun"),
    AchievementDef("storyteller",    "🎬", "Conteur",             "10 stories publiées. Tu as toujours une histoire à raconter.","#E64A19","rare"),

    // ── Social ────────────────────────────────────────────────────────────────
    AchievementDef("social",         "🤝", "Sociable",            "5 conversations démarrées. Tu brises la glace facilement !","#4CAF50","commun"),
    AchievementDef("social_plus",    "🌟", "Ambassadeur",         "20 conversations. Tu connais tout le monde !",        "#388E3C", "rare"),

    // ── Régularité ────────────────────────────────────────────────────────────
    AchievementDef("streak_3",       "🔥", "Sur une lancée",      "Actif 3 jours de suite. Bel élan !",                 "#FF6D00", "commun"),
    AchievementDef("streak_7",       "💥", "Semaine Complète",    "7 jours actifs d'affilée. Respect !",                "#D84315", "épique"),
    AchievementDef("streak_30",      "🏆", "Pilier du Campus",    "30 jours actifs. Tu fais partie des meubles !",      "#BF360C", "légendaire"),

    // ── Identité ──────────────────────────────────────────────────────────────
    AchievementDef("eni_pride",      "🎓", "Fier(e) de l'ENI",   "Badge ENI obtenu. Bienvenue dans la famille !",      "#1565C0", "commun"),
    AchievementDef("badge_maker",    "🏷️","Créateur de Badge",   "Tu as créé ton propre badge. Original !",            "#7C4DFF", "rare"),
    AchievementDef("mood_master",    "😊", "Expressif(ve)",       "Tu as défini ta première humeur du jour.",           "#F06292", "commun"),
)