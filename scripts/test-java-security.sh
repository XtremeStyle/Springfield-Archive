#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
javac -encoding UTF-8 -d "$TMP" \
  "$ROOT/backend/src/main/java/com/springfieldarchive/sync/AdminTokenVerifier.java" \
  "$ROOT/backend/src/main/java/com/springfieldarchive/thesimpsonsapi/HttpResponseSecurity.java" \
  "$ROOT/scripts/java-smoke/SecuritySmokeTest.java"
java -cp "$TMP" SecuritySmokeTest
