#!/usr/bin/env bash
set -euo pipefail

: "${BRANCH:=main}"
: "${COMMIT_MSG:=Add consumer CI workflows + taskdef + deploy scripts}"

git rev-parse --is-inside-work-tree >/dev/null

# Show status so you can see what will be committed (no interaction required)
echo "== git status (porcelain) =="
git status --porcelain

if [[ -z "$(git status --porcelain)" ]]; then
  echo "Nothing to commit. Skipping commit/push."
  exit 0
fi

echo "== adding files =="
git add .github ecs scripts

echo "== committing =="
git commit -m "$COMMIT_MSG"

echo "== pushing to origin/$BRANCH =="
git push origin "$BRANCH"

echo "✅ commit + push complete"