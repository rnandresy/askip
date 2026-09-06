# Attic — code mis de côté

Ce dossier n'est **pas** compilé. Il conserve du code inachevé pour référence.

## `viewmodel/` — le découpage d'AskipViewModel

Cinq ViewModels par domaine, écrits pour remplacer `AskipViewModel`.
Ils n'ont jamais compilé : ils appellent une API de repository qui n'existe pas
(`GroupRepository`, `MessageRepository`, `repo.listenToPost`, `setCommentReaction`,
`getOlderMessages`…), deux d'entre eux portent un nom de classe littéral avec
backticks (`` `SessionViewModel.kt` ``), et aucun écran ne les utilisait.

Ils sont sortis du source set pour que le projet compile à nouveau.

**L'intention reste bonne** et la couche données a été découpée dans ce sens :

| ViewModel de l'attic | Repository correspondant, déjà écrit |
| --- | --- |
| `SessionViewModel`   | `AuthRepository` + `ProfileRepository` |
| `FeedViewModel`      | `FeedRepository` |
| `ProfileViewModel`   | `ProfileRepository` |
| `CommentViewModel`   | `FeedRepository` (commentaires) |
| `MessagingViewModel` | `MessagingRepository` |

### Pour reprendre la migration

Écran par écran, sans big bang :

1. Choisir un écran (`ConfessionsScreen` est le plus simple).
2. Réécrire le ViewModel correspondant **contre l'API réelle** des repositories
   — ne pas repartir du fichier de l'attic tel quel, il vise une API imaginaire.
3. Brancher l'écran dessus, retirer les membres devenus inutiles d'`AskipViewModel`.
4. Recommencer.

Fonctions qui manquent encore côté repository pour aller au bout :
réponses en fil sous les commentaires, réactions sur les commentaires,
pagination des anciens messages, épinglage d'une conversation.
