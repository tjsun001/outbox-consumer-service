name: Consumer Deploy (ECS Service)

on:
  workflow_dispatch:

permissions:
  id-token: write
  contents: read

concurrency:
  group: outbox-consumer-deploy
  cancel-in-progress: true

env:
  AWS_REGION: us-east-1

  # ECR
  ECR_REPO: outbox-consumer-service
  CONTAINER_NAME: outbox-consumer-service

  # ECS
  ECS_CLUSTER: mlops-poc-dev-ecs-cluster
  ECS_SERVICE: outbox-consumer-service
  TASKDEF_TEMPLATE: ecs/task-definition.json

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Ensure task definition template exists
        run: |
          set -euo pipefail
          test -f "${{ env.TASKDEF_TEMPLATE }}" || { echo "Missing ${{ env.TASKDEF_TEMPLATE }}"; exit 1; }

      - name: Configure AWS credentials (OIDC)
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_TO_ASSUME }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Login to Amazon ECR
        id: login-ecr
        uses: aws-actions/amazon-ecr-login@v2

      - name: Resolve latest image tag from ECR
        run: |
          set -euo pipefail

          TAG="$(aws ecr describe-images \
            --region "${{ env.AWS_REGION }}" \
            --repository-name "${{ env.ECR_REPO }}" \
            --query "reverse(sort_by(imageDetails,&imagePushedAt))[0].imageTags[0]" \
            --output text)"

          if [ -z "$TAG" ] || [ "$TAG" = "None" ]; then
            echo "ERROR: could not resolve latest image tag for repo: ${{ env.ECR_REPO }}"
            exit 1
          fi

          echo "IMAGE_TAG=$TAG" >> "$GITHUB_ENV"
          echo "Resolved IMAGE_TAG=$TAG"

      - name: Resolve digest and render taskdef (pinned)
        run: |
          set -euo pipefail

          REGISTRY="${{ steps.login-ecr.outputs.registry }}"
          TAG="${{ env.IMAGE_TAG }}"

          DIGEST="$(aws ecr describe-images \
            --region "${{ env.AWS_REGION }}" \
            --repository-name "${{ env.ECR_REPO }}" \
            --image-ids imageTag="$TAG" \
            --query 'imageDetails[0].imageDigest' \
            --output text)"

          if [ -z "$DIGEST" ] || [ "$DIGEST" = "None" ]; then
            echo "ERROR: could not resolve digest for ${{ env.ECR_REPO }}:$TAG"
            exit 1
          fi

          IMAGE_PINNED="$REGISTRY/${{ env.ECR_REPO }}@$DIGEST"
          echo "IMAGE_PINNED=$IMAGE_PINNED"

          OUT="/tmp/taskdef.consumer.pinned.json"
          jq --arg IMG "$IMAGE_PINNED" --arg NAME "${{ env.CONTAINER_NAME }}" \
            '(.containerDefinitions[] | select(.name==$NAME) | .image) = $IMG' \
            "${{ env.TASKDEF_TEMPLATE }}" > "$OUT"

          echo "TASKDEF_RENDERED=$OUT" >> "$GITHUB_ENV"

          # HARD GATE: verify pinned image set
          ACTUAL="$(jq -r --arg NAME "${{ env.CONTAINER_NAME }}" '.containerDefinitions[] | select(.name==$NAME) | .image' "$OUT")"
          echo "EXPECTED=$IMAGE_PINNED"
          echo "ACTUAL=$ACTUAL"
          if [ "$IMAGE_PINNED" != "$ACTUAL" ]; then
            echo "ERROR: pinned image mismatch in rendered task definition"
            exit 1
          fi

      - name: Register task definition
        run: |
          set -euo pipefail

          TD_ARN="$(aws ecs register-task-definition \
            --region "${{ env.AWS_REGION }}" \
            --cli-input-json "file://${{ env.TASKDEF_RENDERED }}" \
            --query 'taskDefinition.taskDefinitionArn' \
            --output text)"

          if [ -z "$TD_ARN" ] || [ "$TD_ARN" = "None" ]; then
            echo "ERROR: failed to register task definition"
            exit 1
          fi

          echo "TD_ARN=$TD_ARN" >> "$GITHUB_ENV"
          echo "Registered TD_ARN=$TD_ARN"

      - name: Update service and wait stable
        run: |
          set -euo pipefail

          aws ecs update-service \
            --region "${{ env.AWS_REGION }}" \
            --cluster "${{ env.ECS_CLUSTER }}" \
            --service "${{ env.ECS_SERVICE }}" \
            --task-definition "${{ env.TD_ARN }}" \
            --force-new-deployment

          aws ecs wait services-stable \
            --region "${{ env.AWS_REGION }}" \
            --cluster "${{ env.ECS_CLUSTER }}" \
            --services "${{ env.ECS_SERVICE }}"

          echo "✅ Consumer service deployed and stable (tag=${{ env.IMAGE_TAG }})"