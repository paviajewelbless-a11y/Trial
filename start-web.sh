#!/usr/bin/env bash
set -e
echo "Starting FeedMill Ops Web Portal at http://localhost:5000 ..."
if command -v open >/dev/null 2>&1; then
  (sleep 1 && open "http://localhost:5000") &
elif command -v xdg-open >/dev/null 2>&1; then
  (sleep 1 && xdg-open "http://localhost:5000") &
fi

if command -v python3 >/dev/null 2>&1; then
  python3 -m http.server 5000 -d web
else
  npx --yes serve web -l 5000
fi
