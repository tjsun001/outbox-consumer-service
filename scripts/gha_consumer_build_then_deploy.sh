#!/usr/bin/env bash
set -euo pipefail

: "${BRANCH:=main}"

# workflow file names (match your repo)
: "${WF_BUILD:=consumer-build-push.yml}"
: "${WF_DEPLOY:=consumer-deploy-service.yml}"

command -v gh >/dev/null || { echo "ERROR: gh CLI not found. Install with: brew install gh"; exit 1; }

# Ensure we are on the right branch locally
CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "$CURRENT_BRANCH" != "$BRANCH" ]]; then
  echo "ERROR: You are on branch '$CURRENT_BRANCH' but BRANCH is '$BRANCH'"
  echo "Switch branches or set BRANCH=<branch>"
  exit 1
fi

# Use current HEAD to compute the tag (same as workflows)
SHA="$(git rev-parse HEAD)"
IMAGE_TAG="sha-${SHA:0:12}"

echo "BRANCH=$BRANCH"
echo "SHA=$SHA"
echo "IMAGE_TAG=$IMAGE_TAG"
echo

echo "== Triggering build workflow: $WF_BUILD =="
gh workflow run "$WF_BUILD" --ref "$BRANCH"

echo "== Waiting for build workflow to complete =="
# Wait for the most recent run of this workflow on this branch
gh run watch --workflow "$WF_BUILD" --branch "$BRANCH" --exit-status

echo "✅ Build workflow succeeded."

echo
echo "== Triggering deploy workflow: $WF_DEPLOY (image_tag=$IMAGE_TAG) =="
gh workflow run "$WF_DEPLOY" --ref "$BRANCH" -f image_tag="$IMAGE_TAG"

echo "== Waiting for deploy workflow to complete =="
gh run watch --workflow "$WF_DEPLOY" --branch "$BRANCH" --exit-status

echo "✅ Deploy workflow succeeded."