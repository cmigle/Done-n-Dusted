#!/usr/bin/env bash
# Surenka viena jar su React sasaja viduje (classpath:/static): API ir UI - tas pats serveris ir adresas.
# Naudojimas: scripts/package.sh   ->   target/done-n-dusted-<versija>.jar
# Diegimas i Azure App Service: az webapp deploy -g rg-done-n-dusted -n done-n-dusted --type jar --src-path target/done-n-dusted-<versija>.jar
set -euo pipefail
cd "$(dirname "$0")/.."

(cd frontend && npm ci && npm run build)
rm -rf src/main/resources/static
mkdir -p src/main/resources/static
cp -r frontend/dist/. src/main/resources/static/
./mvnw -B -ntp -DskipTests package
ls -1 target/*.jar
