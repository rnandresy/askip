#!/usr/bin/env bash
#
# Audit de non-régression.
#
# Compare chaque fichier Kotlin modifié à sa version dans un commit de
# référence (HEAD par défaut) et signale ce qui a disparu :
#
#   1. les appels au ViewModel — un appel perdu, c'est une commande de
#      l'interface qui ne fait plus rien ;
#   2. les libellés visibles — un texte perdu, c'est peut-être un bouton
#      supprimé plutôt que renommé.
#
# La seconde passe est bavarde par nature : une reformulation volontaire
# ressort comme une perte. Elle est là pour donner une liste courte à
# relire, pas pour décider à votre place.
#
#   bash tools/audit-non-regression.sh            # contre HEAD
#   bash tools/audit-non-regression.sh 82e34b8    # contre un commit précis
#
set -u

REF="${1:-HEAD}"
RACINE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$RACINE" || exit 1

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fichiers=$(git diff --name-only "$REF" -- 'app/src/main/java' 2>/dev/null | grep '\.kt$')

if [ -z "$fichiers" ]; then
    echo "Aucun fichier Kotlin modifié depuis $REF."
    exit 0
fi

echo "→ Référence : $REF"
echo "→ $(echo "$fichiers" | wc -l) fichier(s) modifié(s)"
echo

# ── 1. Les appels au ViewModel ───────────────────────────────────────────────
echo "── Appels au ViewModel ──────────────────────────────────────────────"
perdus_vm=0

for f in $fichiers; do
    git show "$REF:$f" > "$TMP/avant.kt" 2>/dev/null || continue
    [ -s "$TMP/avant.kt" ] || continue
    [ -f "$f" ] || continue

    # `vm.x(...)` et `vm::x` désignent la même chose : on normalise.
    grep -oE 'vm(\.|::)[a-zA-Z][a-zA-Z0-9]*' "$TMP/avant.kt" \
        | sed 's/vm::/vm./' | sort -u > "$TMP/a.txt"
    grep -oE 'vm(\.|::)[a-zA-Z][a-zA-Z0-9]*' "$f" \
        | sed 's/vm::/vm./' | sort -u > "$TMP/b.txt"

    manque=$(comm -23 "$TMP/a.txt" "$TMP/b.txt")
    if [ -n "$manque" ]; then
        echo "  ! ${f#app/src/main/java/com/rnandresy/lol/}"
        echo "$manque" | sed 's/^/      /'
        perdus_vm=$((perdus_vm + 1))
    fi
done

[ "$perdus_vm" -eq 0 ] && echo "  ✓ Aucun appel perdu."
echo

# ── 2. Les libellés visibles ─────────────────────────────────────────────────
echo "── Libellés visibles (à relire, pas à corriger aveuglément) ─────────"
perdus_txt=0

for f in $fichiers; do
    git show "$REF:$f" > "$TMP/avant.kt" 2>/dev/null || continue
    [ -s "$TMP/avant.kt" ] || continue
    [ -f "$f" ] || continue

    # Chaînes littérales sans interpolation ni échappement, assez longues
    # pour être du texte plutôt qu'une clé technique.
    grep -oE '"[^"$\\]{5,45}"' "$TMP/avant.kt" | sort -u > "$TMP/a.txt"
    grep -oE '"[^"$\\]{5,45}"' "$f"            | sort -u > "$TMP/b.txt"

    manque=$(comm -23 "$TMP/a.txt" "$TMP/b.txt")
    if [ -n "$manque" ]; then
        echo "  · ${f#app/src/main/java/com/rnandresy/lol/}"
        echo "$manque" | sed 's/^/      /'
        perdus_txt=$((perdus_txt + 1))
    fi
done

[ "$perdus_txt" -eq 0 ] && echo "  ✓ Aucun libellé perdu."
echo

# ── Verdict ──────────────────────────────────────────────────────────────────
if [ "$perdus_vm" -eq 0 ]; then
    echo "✓ Aucune commande de l'interface n'a perdu son point d'entrée."
    exit 0
fi

echo "✗ $perdus_vm fichier(s) ont perdu un appel au ViewModel — à vérifier."
exit 1
