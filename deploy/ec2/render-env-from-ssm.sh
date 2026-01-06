#!/usr/bin/env bash
set -euo pipefail

SSM_PATH_PREFIX="${SSM_PATH_PREFIX:-/sentimentscribe/prod/}"
OUTPUT_FILE="${1:-.env.prod}"

if ! command -v aws >/dev/null 2>&1; then
  echo "ERROR: aws CLI not found. Install it on EC2 before running this script." >&2
  exit 1
fi

umask 077
tmp_file="$(mktemp)"

write_kv() {
  local key="$1"
  local value="$2"

  if [[ "$value" == *$'\n'* ]]; then
    echo "ERROR: SSM parameter '$key' contains a newline; refusing to write dotenv." >&2
    exit 1
  fi

  if [[ "$value" =~ [[:space:]\#\"] ]]; then
    value="${value//\\/\\\\}"
    value="${value//\"/\\\"}"
    printf '%s="%s"\n' "$key" "$value" >>"$tmp_file"
  else
    printf '%s=%s\n' "$key" "$value" >>"$tmp_file"
  fi
}

aws_output="$(
  aws ssm get-parameters-by-path \
    --path "$SSM_PATH_PREFIX" \
    --recursive \
    --with-decryption \
    --output text \
    --query 'Parameters[*].[Name,Value]'
)"

if [[ -n "${aws_output:-}" ]]; then
  while IFS=$'\t' read -r name value; do
    [[ -z "${name:-}" ]] && continue
    key="${name##*/}"
    write_kv "$key" "${value:-}"
  done <<<"$aws_output"
fi

chmod 600 "$tmp_file"
mv -f "$tmp_file" "$OUTPUT_FILE"
echo "Wrote $OUTPUT_FILE from SSM path $SSM_PATH_PREFIX"
