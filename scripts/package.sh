#!/usr/bin/env bash
# Builds one jar with the React UI inside (classpath:/static): the API and the UI share one server and address.
# Usage: scripts/package.sh   ->   target/done-n-dusted-<version>.jar
# Deploy to Azure App Service: az webapp deploy -g rg-done-n-dusted -n done-n-dusted --type jar --src-path target/done-n-dusted-<version>.jar
set -euo pipefail
cd "$(dirname "$0")/.."

(cd frontend && npm ci && npm run build)
rm -rf src/main/resources/static
mkdir -p src/main/resources/static
cp -r frontend/dist/. src/main/resources/static/
# clean: otherwise target/classes keeps assets from earlier builds and they end up in the jar
./mvnw -B -ntp -DskipTests clean package
ls -1 target/*.jar
