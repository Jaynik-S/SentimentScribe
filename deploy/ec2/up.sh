#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE="${ENV_FILE:-.env.prod}"
COMPOSE_FILE="docker-compose.prod.yml"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "$ENV_FILE not found; attempting to render from SSM..." >&2
  ./render-env-from-ssm.sh "$ENV_FILE"
fi

if [[ -z "${ECR_REGISTRY:-}" ]] && ! grep -qE '^ECR_REGISTRY=' "$ENV_FILE"; then
  echo "ERROR: ECR_REGISTRY is required (export it or add it to $ENV_FILE)." >&2
  echo "Hint: start from ../../.env.example and copy values into $ENV_FILE." >&2
  exit 1
fi

docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" pull
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
