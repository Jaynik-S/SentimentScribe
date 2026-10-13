#!/usr/bin/env bash
set -euo pipefail
expected_account="$1"
image_tag="$2"
actual_account=$(aws sts get-caller-identity --query Account --output text --region ca-central-1)
if [ "$actual_account" != "$expected_account" ]; then
  echo 'Instance account mismatch; refusing deployment.' >&2
  exit 1
fi
registry="${expected_account}.dkr.ecr.ca-central-1.amazonaws.com"
cd /home/ubuntu/sentimentscribe/deploy/ec2
chmod 600 .env.prod
compose=(docker compose -f docker-compose.prod.yml --env-file .env.prod)
"${compose[@]}" config --quiet
if ! "${compose[@]}" config --images | grep -Fxq "$registry/sentimentscribe-backend:$image_tag"; then
  echo 'Environment image does not match the verified account and release.' >&2
  exit 1
fi
aws ecr get-login-password --region ca-central-1 | docker login --username AWS --password-stdin "$registry"
"${compose[@]}" pull
"${compose[@]}" up -d
docker logout "$registry"
"${compose[@]}" ps
for attempt in $(seq 1 90); do
  if docker exec sentimentscribe-caddy wget -qO- http://backend:8080/api/health 2>/dev/null | grep -Eq '"status"[[:space:]]*:[[:space:]]*"ok"'; then
    echo 'Internal backend health verified; PostgreSQL, backend, and Caddy are running.'
    exit 0
  fi
  sleep 5
done
echo 'Backend did not become healthy. Inspect container status before proceeding.' >&2
exit 1
