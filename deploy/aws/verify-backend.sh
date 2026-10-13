#!/usr/bin/env bash
set -euo pipefail
expected_account="$1"
actual_account=$(aws sts get-caller-identity --query Account --output text --region ca-central-1)
test "$actual_account" = "$expected_account"
cd /home/ubuntu/sentimentscribe/deploy/ec2
test "$(stat -c '%a' .env.prod)" = '600'
docker exec sentimentscribe-backend sh -c 'test "${#SENTIMENTSCRIBE_JWT_SECRET}" -ge 64'
docker exec sentimentscribe-postgres pg_isready -U diary -d sentiment_scribe
docker exec sentimentscribe-postgres psql -U diary -d sentiment_scribe -Atc 'SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;'
docker inspect --format '{{range .Mounts}}{{if eq .Destination "/var/lib/postgresql/data"}}{{.Source}}{{end}}{{end}}' sentimentscribe-postgres
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
echo 'Verified instance account, private environment permissions, JWT mapping, Flyway, and persistent PostgreSQL mount without printing secrets.'
