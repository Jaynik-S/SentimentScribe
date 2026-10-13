#!/usr/bin/env bash
set -euo pipefail

assert_spotify_only_change() {
  local current="$1" candidate="$2" mode="${3:-}" key pattern
  [[ "$mode" == '' || "$mode" == '--defer-spotify' ]] || return 1
  for key in SPOTIFY_CLIENT_ID SPOTIFY_CLIENT_SECRET; do
    pattern="^${key}=[A-Za-z0-9._-]+$"
    if [ "$mode" = '--defer-spotify' ]; then pattern="^${key}=$"; fi
    if [ "$(grep -Ec "^${key}=" "$current")" != 1 ] || [ "$(grep -Ec "$pattern" "$candidate")" != 1 ] || [ "$(grep -Ec "^${key}=" "$candidate")" != 1 ]; then
      echo 'Spotify fields must be unique and shell-safe; refusing update.' >&2
      return 1
    fi
  done
  if ! cmp -s <(sed -E '/^SPOTIFY_CLIENT_(ID|SECRET)=/d' "$current") <(sed -E '/^SPOTIFY_CLIENT_(ID|SECRET)=/d' "$candidate"); then
    echo 'Non-Spotify settings differ; refusing update.' >&2
    return 1
  fi
}

apply_spotify_credentials() {
  local expected_account="$1" candidate="$2" mode="${3:-}" current backup
  [[ "$expected_account" =~ ^[0-9]{12}$ ]]
  [[ "$candidate" =~ ^/home/ubuntu/\.local/share/sentimentscribe-secrets/spotify-candidate-[0-9]{17}\.env$ ]]
  test "$(aws sts get-caller-identity --query Account --output text --region ca-central-1)" = "$expected_account"
  current='/home/ubuntu/sentimentscribe/deploy/ec2/.env.prod'
  test "$(stat -c '%a' "$current")" = 600
  test "$(stat -c '%a' "$candidate")" = 600
  exec 9>/home/ubuntu/.local/share/sentimentscribe-secrets/spotify-update.lock
  flock -n 9
  assert_spotify_only_change "$current" "$candidate" "$mode"
  cd /home/ubuntu/sentimentscribe/deploy/ec2
  sudo docker compose -f docker-compose.prod.yml --env-file "$candidate" config --quiet
  backup="/home/ubuntu/.local/share/sentimentscribe-secrets/before-spotify-$(date -u +%Y%m%d%H%M%S%N).env"
  test ! -e "$backup"
  cp --preserve=mode "$current" "$backup"
  mv -- "$candidate" "$current"
  sudo docker compose -f docker-compose.prod.yml --env-file "$current" up -d --no-deps --pull never backend
  for attempt in $(seq 1 90); do
    if sudo docker exec sentimentscribe-caddy wget -qO- http://backend:8080/api/health 2>/dev/null | grep -Eq '"status"[[:space:]]*:[[:space:]]*"ok"'; then
      echo 'Applied only Spotify credentials; backend health passed; database/JWT/TMDb settings preserved. Private rollback copy retained.'
      return 0
    fi
    sleep 5
  done
  echo 'Backend health failed after credential update. Private rollback copy retained; review before further changes.' >&2
  return 1
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  apply_spotify_credentials "$@"
fi
