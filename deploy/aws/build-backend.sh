#!/usr/bin/env bash
set -euo pipefail
expected_account="$1"
image_tag="$2"
cloud-init status --wait
test -f /var/lib/sentimentscribe-bootstrap-complete
actual_account=$(aws sts get-caller-identity --query Account --output text --region ca-central-1)
if [ "$actual_account" != "$expected_account" ]; then
  echo 'Instance account mismatch; refusing build/push.' >&2
  exit 1
fi
registry="${expected_account}.dkr.ecr.ca-central-1.amazonaws.com"
cd /home/ubuntu/sentimentscribe/backend
docker build -t "$registry/sentimentscribe-backend:$image_tag" .
aws ecr get-login-password --region ca-central-1 | docker login --username AWS --password-stdin "$registry"
docker push "$registry/sentimentscribe-backend:$image_tag"
docker logout "$registry"
echo 'Backend image built and pushed successfully.'
