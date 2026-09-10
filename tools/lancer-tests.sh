#!/usr/bin/env bash
#
# Joue les tests unitaires — ici, sur cette machine, sans Gradle.
#
#   bash tools/lancer-tests.sh
#
# On a longtemps cru qu'aucun travail JVM n'était possible sur ce poste, parce
# que `./gradlew` et l'émulateur Firestore échouent tous les deux sur
# `Unable to establish loopback connection`. Mais ce qui est bloqué, c'est
# l'ouverture d'une socket locale, pas la JVM : le compilateur Kotlin tourne
# déjà ici (voir `verifier-compilation.sh`), et JUnit n'écoute sur rien.
#
# Ce script compile donc `app/src/test` par-dessus les classes de l'app, puis
# lance JUnit dessus. Il rend un résultat en une poignée de secondes, sans
# attendre la CI.
#
# Ce qu'il ne couvre pas : tout ce qui touche Android pour de vrai. Les classes
# de l'app sont compilées contre `android.jar` mais exécutées sur le JDK du
# poste, donc un test qui appelle une API Android échouera sur
# `NoClassDefFoundError` — c'est le domaine des tests instrumentés, qui
# demandent un appareil. Ici, on vérifie la logique pure : le Rumeur-mètre, le
# tri du fil, les masques, la cote du Pari.

set -u

PROJET="$(cd "$(dirname "$0")/.." && pwd)"
SORTIE="$PROJET/build/verif-kotlin"
CACHE="$HOME/.gradle/caches/modules-2/files-2.1"

# Les chemins Windows contenant un espace cassent l'argfile du compilateur :
# on passe par le nom court 8.3. Le dossier du projet peut arriver sous les
# deux formes selon qui appelle, d'où les deux substitutions.
court() {
    sed -e 's|/c/Users/WINDOWS 11/|C:/Users/WINDOW~1/|g' \
        -e 's|C:/Users/WINDOWS 11/|C:/Users/WINDOW~1/|g'
}

JAVA_BIN="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}/bin/java"
[ -x "$JAVA_BIN" ] || JAVA_BIN="java"

# ── Les classes de l'app ─────────────────────────────────────────────────────
# `verifier-compilation.sh` produit le classpath et les classes compilées ;
# on ne refait pas son travail, on s'appuie dessus. Le relancer garantit aussi
# que les tests ne sont pas joués contre une version périmée de l'app.
echo "→ Compilation de l'app…"
if ! bash "$PROJET/tools/verifier-compilation.sh" > "$SORTIE/../compile-app.log" 2>&1; then
    echo "✗ L'app ne compile pas — les tests ne diraient rien d'utile."
    tail -20 "$SORTIE/../compile-app.log"
    exit 1
fi
echo "  ✓ app compilée"

# ── JUnit ────────────────────────────────────────────────────────────────────
JUNIT="$(find "$CACHE/junit" -name 'junit-*.jar' 2>/dev/null | grep -v -e sources -e javadoc | sort -V | tail -1)"
HAMCREST="$(find "$CACHE/org.hamcrest" -name 'hamcrest-core-*.jar' 2>/dev/null | grep -v -e sources -e javadoc | sort -V | tail -1)"

if [ -z "$JUNIT" ] || [ -z "$HAMCREST" ]; then
    echo "✗ JUnit introuvable dans le cache Gradle."
    echo "  Il y arrive au premier build Android Studio (testImplementation)."
    exit 1
fi

CP_APP="$(cat "$SORTIE/cp.txt")"
CP_TESTS="$CP_APP;$(echo "$SORTIE/out;$JUNIT;$HAMCREST" | court)"

# ── Compilation des tests ────────────────────────────────────────────────────
echo "→ Compilation des tests…"
{
    echo "-classpath"; echo "$CP_TESTS"
    echo "-jvm-target"; echo "17"
    # Même raison que dans verifier-compilation.sh : le compilateur 2.0.21
    # plante en lisant la version des JDK récents.
    echo "-no-jdk"
    echo "-no-reflect"
    echo "-d"; echo "$(echo "$SORTIE/out-tests" | court)"
    ( cd "$PROJET" && find app/src/test -name "*.kt" )
} > "$SORTIE/args-tests.txt"

rm -rf "$SORTIE/out-tests"
cd "$PROJET"
"$JAVA_BIN" -Xmx2g -cp "$(cat "$SORTIE/runtime-compilateur.txt")" \
    org.jetbrains.kotlin.cli.jvm.K2JVMCompiler "@$SORTIE/args-tests.txt" \
    > "$SORTIE/compile-tests.log" 2>&1
CODE=$?

ERREURS=$(grep -c "error:" "$SORTIE/compile-tests.log" 2>/dev/null || true)
if [ "$CODE" -ne 0 ] || [ "${ERREURS:-0}" -ne 0 ]; then
    echo "✗ Les tests ne compilent pas :"
    grep -A3 "error:" "$SORTIE/compile-tests.log" | head -40
    exit 1
fi
echo "  ✓ tests compilés"

# ── Exécution ────────────────────────────────────────────────────────────────
# Une classe par fichier `*Test.kt`, nommée d'après son chemin.
CLASSES=$(cd "$PROJET/app/src/test/java" && find . -name "*Test.kt" \
    | sed -e 's|^\./||' -e 's|\.kt$||' -e 's|/|.|g' | sort)

if [ -z "$CLASSES" ]; then
    echo "✗ Aucune classe de test trouvée sous app/src/test/java."
    exit 1
fi

echo "→ Exécution…"
echo "$CLASSES" | sed 's/^/    /'
echo

# Le classpath dépasse ce que la ligne de commande accepte : on passe par un
# argfile, que `java` comprend depuis la version 9.
{
    echo "-cp"
    echo "$CP_TESTS;$(echo "$SORTIE/out-tests" | court)"
} > "$SORTIE/args-run.txt"

# shellcheck disable=SC2086
"$JAVA_BIN" "@$SORTIE/args-run.txt" org.junit.runner.JUnitCore $CLASSES
