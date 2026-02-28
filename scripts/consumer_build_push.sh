#!/usr/bin/env bash
set -euo pipefail

: "${AWS_REGION:=us-east-1}"
: "${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID (e.g., 080967118593)}"
: "${ECR_REPO:=outbox-consumer-service}"

if [[ -z "${IMAGE_TAG:-}" ]]; then
  IMAGE_TAG="sha-$(git rev-parse --short=12 HEAD)"
fi

ECR_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}"
IMAGE_URI="${ECR_URI}:${IMAGE_TAG}"

echo "IMAGE_URI=$IMAGE_URI"

aws ecr get-login-password --region "$AWS_REGION" \
| docker login --username AWS --password-stdin "$ECR_URI"

docker buildx build \
  --platform linux/amd64 \
  -t "$IMAGE_URI" \
  --push \
  .

echo "PUSHED: $IMAGE_URI"
echo "IMAGE_TAG=$IMAGE_TAG"