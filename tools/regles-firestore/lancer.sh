#!/usr/bin/env bash
# Lance l'émulateur Firestore et rejoue le banc d'essai des règles.
# Rien ne touche le vrai projet : tout vit dans l'émulateur local.
set -e
PROJET="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$PROJET/tools/regles-firestore"
[ -d node_modules ] || npm install --silent --no-audit --no-fund
cd "$PROJET"
firebase emulators:exec --only firestore --project demo-askip-regles \
  "node tools/regles-firestore/test-regles.mjs"
