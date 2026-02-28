#!/usr/bin/env bash
set -euo pipefail

: "${AWS_REGION:=us-east-1}"
: "${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID}"
: "${ECR_REPO:=outbox-consumer-service}"

: "${ECS_CLUSTER:=mlops-poc-dev-ecs-cluster}"
: "${ECS_SERVICE:=outbox-consumer-service}"
: "${CONTAINER_NAME:=outbox-consumer-service}"
: "${TASKDEF_TEMPLATE:=ecs/task-definition.json}"

: "${IMAGE_TAG:?Set IMAGE_TAG (e.g., sha-<12>)}"

ECR_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}"

DIGEST="$(
  aws ecr describe-images \
    --region "$AWS_REGION" \
    --repository-name "$ECR_REPO" \
    --image-ids imageTag="$IMAGE_TAG" \
    --query 'imageDetails[0].imageDigest' \
    --output text
)"

if [[ -z "$DIGEST" || "$DIGEST" == "None" ]]; then
  echo "ERROR: could not resolve digest for $ECR_REPO:$IMAGE_TAG"
  exit 1
fi

IMAGE_PINNED="${ECR_URI}@${DIGEST}"
echo "IMAGE_PINNED=$IMAGE_PINNED"

OUT_TD="/tmp/taskdef.consumer.pinned.json"
jq --arg IMG "$IMAGE_PINNED" --arg NAME "$CONTAINER_NAME" \
  '(.containerDefinitions[] | select(.name==$NAME) | .image) = $IMG' \
  "$TASKDEF_TEMPLATE" > "$OUT_TD"

TD_ARN="$(
  aws ecs register-task-definition \
    --region "$AWS_REGION" \
    --cli-input-json "file://$OUT_TD" \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text
)"

echo "TD_ARN=$TD_ARN"

aws ecs update-service \
  --region "$AWS_REGION" \
  --cluster "$ECS_CLUSTER" \
  --service "$ECS_SERVICE" \
  --task-definition "$TD_ARN" \
  --force-new-deployment

aws ecs wait services-stable \
  --region "$AWS_REGION" \
  --cluster "$ECS_CLUSTER" \
  --services "$ECS_SERVICE"

echo "✅ Consumer service deployed and stable"