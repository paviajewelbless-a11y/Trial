#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "Deploying FeedMill Ops Web Portal to https://gen-lang-client-0293694787.web.app ..."
if command -v firebase >/dev/null 2>&1; then
  firebase login
  firebase deploy --only hosting --project gen-lang-client-0293694787
elif command -v npx >/dev/null 2>&1; then
  npx --yes firebase-tools login
  npx --yes firebase-tools deploy --only hosting --project gen-lang-client-0293694787
else
  echo "Install Node.js or Firebase CLI first: https://firebase.google.com/docs/cli"
  exit 1
fi
echo "Live at: https://gen-lang-client-0293694787.web.app"
