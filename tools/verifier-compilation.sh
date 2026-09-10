#!/usr/bin/env bash
#
# Compile tout le Kotlin de l'app sans passer par Gradle.
#
# Pourquoi : sur cette machine Gradle refuse de démarrer
# (« java.io.IOException: Unable to establish loopback connection »), quel que
# soit le JDK. On appelle donc le compilateur Kotlin directement, avec les
# dépendances déjà présentes dans le cache Gradle.
#
# ⚠️ LEÇON APPRISE — pourquoi ce script est plus compliqué qu'il n'y paraît.
# La première version ramassait *tous* les jars du cache. Le cache contenait
# material3 1.2.1 **et** 1.3.1 ; la 1.2.1 sortait en premier, et du code
# utilisant une API supprimée en 1.3 compilait ici mais échouait dans Android
# Studio. Un faux « ✓ » est pire que pas de vérification du tout.
# On ne garde donc qu'**une seule version par artefact**, la plus récente, et
# on signale les doublons pour que l'ambiguïté reste visible.
#
# Ce que ça vérifie   : syntaxe, types, imports, signatures, Compose.
# Ce que ça ne fait pas : ressources, manifeste, R8, APK, comportement à
#                         l'exécution. Pour ça, il faut Android Studio.
#
# Usage :  bash tools/verifier-compilation.sh
#
set -uo pipefail

PROJET="$(cd "$(dirname "$0")/.." && pwd)"
SORTIE="$PROJET/build/verif-kotlin"
DEPS="$SORTIE/deps"
KOTLIN="2.0.21"
ANDROID_API="35"

JAVA_BIN="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}/bin/java"
[ -x "$JAVA_BIN" ] || JAVA_BIN="java"

CACHE="$HOME/.gradle/caches/modules-2/files-2.1"
SDK="$HOME/AppData/Local/Android/Sdk"

# Les chemins Windows contenant un espace cassent l'argfile du compilateur :
# on bascule sur le nom court 8.3 (WINDOW~1) pour les éliminer.
court() { sed 's|^/c/Users/WINDOWS 11/|C:/Users/WINDOW~1/|; s|^/c/|C:/|'; }

mkdir -p "$SORTIE" "$DEPS"
SORTIE_WIN="$(echo "$SORTIE" | court)"

if [ ! -d "$CACHE" ]; then
    echo "✗ Cache Gradle introuvable. Lance une fois la build dans Android"
    echo "  Studio pour le peupler, puis relance ce script."
    exit 1
fi

# ── Une seule version par artefact ───────────────────────────────────────────
# Les chemins du cache ont la forme :
#   <cache>/<groupe>/<artefact>/<version>/<hash>/<fichier>
# On s'en sert pour regrouper, puis `sort -V` choisit la plus récente.
echo "→ Sélection des dépendances (une version par artefact)…"

find "$CACHE" -type f \( -name "*.jar" -o -name "*.aar" \) \
    ! -name "*sources*" ! -name "*javadoc*" 2>/dev/null |
while IFS= read -r fichier; do
    reste="${fichier#"$CACHE"/}"
    groupe="${reste%%/*}"; reste="${reste#*/}"
    artefact="${reste%%/*}"; reste="${reste#*/}"
    version="${reste%%/*}"
    printf '%s:%s\t%s\t%s\n' "$groupe" "$artefact" "$version" "$fichier"
done | sort -t $'\t' -k1,1 -k2,2V > "$SORTIE/toutes-versions.tsv"

# La dernière ligne de chaque groupe porte la version la plus haute.
awk -F'\t' '{ derniere[$1] = $0 } END { for (k in derniere) print derniere[k] }' \
    "$SORTIE/toutes-versions.tsv" > "$SORTIE/retenues.tsv"

# Doublons : plusieurs versions de la même chose traînent dans le cache.
DOUBLONS=$(cut -f1 "$SORTIE/toutes-versions.tsv" | uniq -d | wc -l)
if [ "$DOUBLONS" -gt 0 ]; then
    echo "  ℹ $DOUBLONS artefact(s) présents en plusieurs versions — la plus"
    echo "    récente est retenue. Détail : build/verif-kotlin/toutes-versions.tsv"
fi

# ── Écarter l'outillage de build ─────────────────────────────────────────────
# Le cache Gradle contient aussi le compilateur Kotlin, les plugins Gradle et
# l'AGP. Les laisser dans le classpath de l'app fait exploser le compilateur
# (« Backend Internal error »), parce qu'il se retrouve à compiler contre une
# seconde copie de lui-même.
# `kotlin-stdlib-jdk7/8` sont écartés aussi : ce sont des façades vides depuis
# Kotlin 1.8, et le cache en contient une version plus récente que le stdlib.
OUTILLAGE='^(com\.android\.tools|com\.android\.databinding:|com\.android:|com\.google\.gms:|com\.google\.testing\.platform:|org\.jetbrains\.intellij\.deps:|org\.jetbrains\.kotlin:(kotlin-(compiler|gradle-plugin|build|daemon|scripting|native|klib|util|tooling|android-extensions|parcelize|script-runtime|stdlib-jdk[78])|compose-compiler-gradle-plugin))'

grep -vE "$OUTILLAGE" "$SORTIE/retenues.tsv" > "$SORTIE/app-deps.tsv"
ECARTES=$(( $(wc -l < "$SORTIE/retenues.tsv") - $(wc -l < "$SORTIE/app-deps.tsv") ))
echo "  ℹ $ECARTES artefact(s) d'outillage écartés du classpath de l'app"

# ── Classpath ────────────────────────────────────────────────────────────────
echo "→ Extraction des AAR…"
: > "$SORTIE/cp-brut.txt"

while IFS=$'\t' read -r cle version fichier; do
    case "$fichier" in
        *.jar)
            echo "$fichier" >> "$SORTIE/cp-brut.txt"
            ;;
        *.aar)
            # Un AAR n'est pas utilisable tel quel : le bytecode est dans un
            # classes.jar à l'intérieur. On l'extrait une fois et on le garde.
            nom="$(echo "${cle}-${version}" | tr ':/' '__')"
            cible="$DEPS/$nom.jar"
            if [ ! -f "$cible" ]; then
                if unzip -p "$fichier" classes.jar > "$cible" 2>/dev/null \
                   && [ -s "$cible" ]; then
                    :
                else
                    # AAR sans bytecode (ressources seules) : rien à ajouter.
                    rm -f "$cible"
                    continue
                fi
            fi
            echo "$cible" >> "$SORTIE/cp-brut.txt"
            ;;
    esac
done < "$SORTIE/app-deps.tsv"

# Le framework Android : indispensable, on compile en -no-jdk.
echo "$SDK/platforms/android-$ANDROID_API/android.jar" >> "$SORTIE/cp-brut.txt"

court < "$SORTIE/cp-brut.txt" | tr '\n' ';' > "$SORTIE/cp.txt"

# Le compilateur et son plugin se cherchent dans la liste **non filtrée** :
# ils sont exclus du classpath de l'app, mais il les faut bien pour lancer
# la compilation.
outil() { cut -f3 "$SORTIE/retenues.tsv" | grep -F "/$1" | head -1; }

# Recherche par coordonnées « groupe:artefact » plutôt que par nom de fichier :
# la version retenue dépend du cache, on ne peut pas la coder en dur.
outil_par_cle() {
    awk -F'\t' -v cle="$1" '$1 == cle { print $3; exit }' "$SORTIE/retenues.tsv"
}

PLUGIN_COMPOSE="$(outil "kotlin-compose-compiler-plugin-embeddable-$KOTLIN.jar" | court)"
if [ -z "$PLUGIN_COMPOSE" ]; then
    echo "✗ Plugin Compose $KOTLIN introuvable dans le cache."
    exit 1
fi

# Le compilateur a besoin de son propre runtime, séparé de celui de l'app.
RUNTIME_COMPILATEUR=$({
    for motif in "kotlin-compiler-embeddable-$KOTLIN.jar" "kotlin-stdlib-$KOTLIN.jar" \
                 "kotlin-reflect-" "kotlin-script-runtime-" \
                 "kotlin-daemon-embeddable-" "trove4j-" \
                 "kotlinx-coroutines-core-jvm-"; do
        outil "$motif"
    done
    # Le générateur de bytecode annote les paramètres avec @Nullable :
    # sans ce jar, il plante en fin de compilation, pas au début.
    outil_par_cle "org.jetbrains:annotations"
} | court | tr '\n' ';')

# Déposé pour `lancer-tests.sh`, qui compile les sources de test avec le même
# compilateur. Le redécouvrir de son côté ferait deux endroits à corriger le
# jour où une version bouge.
echo "$RUNTIME_COMPILATEUR" > "$SORTIE/runtime-compilateur.txt"

# ── Compilation ──────────────────────────────────────────────────────────────
# ── Le `R` de remplacement ───────────────────────────────────────────────────
# `R` est généré par le processeur de ressources d'Android, qu'on ne lance pas
# ici. Sans lui, tout fichier qui référence une ressource échoue à compiler —
# et on perdrait le filet sur le reste du projet pour une raison qui n'est pas
# une vraie erreur. On fabrique donc un `R` minimal, aux identifiants factices,
# à partir des ressources réellement présentes.
{
    echo "package com.rnandresy.lol"
    echo
    echo "// Généré par tools/verifier-compilation.sh — jamais versionné."
    echo "object R {"
    for DOSSIER in font drawable raw; do
        [ -d "$PROJET/app/src/main/res/$DOSSIER" ] || continue
        echo "    object $DOSSIER {"
        for FICHIER in "$PROJET/app/src/main/res/$DOSSIER"/*; do
            [ -e "$FICHIER" ] || continue
            NOM=$(basename "$FICHIER"); NOM=${NOM%%.*}
            echo "        const val $NOM: Int = 0"
        done
        echo "    }"
    done
    echo "}"
} > "$SORTIE/R_stub.kt"

echo "→ Compilation…"
{
    echo "-classpath";   cat "$SORTIE/cp.txt"; echo
    echo "-jvm-target";  echo "17"
    echo "-Xplugin=$PLUGIN_COMPOSE"
    # -no-jdk : on compile contre android.jar, pas contre le JDK de la machine.
    # Ça évite aussi le plantage du compilateur sur les versions de Java récentes.
    echo "-no-jdk"
    echo "-no-reflect"
    echo "-d"; echo "$SORTIE_WIN/out"
    ( cd "$PROJET" && find app/src/main -name "*.kt" )
    # Le `R` de remplacement, décrit plus bas.
    echo "$SORTIE_WIN/R_stub.kt"
} > "$SORTIE/args.txt"

rm -rf "$SORTIE/out"
cd "$PROJET"
"$JAVA_BIN" -Xmx3g -cp "$RUNTIME_COMPILATEUR" \
    org.jetbrains.kotlin.cli.jvm.K2JVMCompiler "@$SORTIE/args.txt" \
    > "$SORTIE/compile.log" 2>&1
CODE=$?

ERREURS=$(grep -c "error:" "$SORTIE/compile.log" || true)
echo
if [ "$CODE" -eq 0 ] && [ "$ERREURS" -eq 0 ]; then
    echo "✓ Compilation réussie — $(find "$SORTIE/out" -name '*.class' | wc -l) classes."
    echo "  Warnings : $(grep -c 'warning:' "$SORTIE/compile.log" || true)"
    # Rappel des versions qui décident du succès ou de l'échec : si le résultat
    # diverge d'Android Studio, c'est presque toujours ici qu'il faut regarder.
    echo "  material3 : $(awk -F'\t' '$1=="androidx.compose.material3:material3-android"{print $2}' "$SORTIE/app-deps.tsv")" \
         "· compose-ui : $(awk -F'\t' '$1=="androidx.compose.ui:ui-android"{print $2}' "$SORTIE/app-deps.tsv")"
else
    echo "✗ $ERREURS erreur(s) :"
    echo
    grep -A2 "error:" "$SORTIE/compile.log"
fi
echo "  Journal : $SORTIE/compile.log"
exit "$CODE"
