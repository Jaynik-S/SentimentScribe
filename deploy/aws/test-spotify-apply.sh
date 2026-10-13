#!/usr/bin/env bash
set -euo pipefail
directory=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
if [ ! -f "$directory/apply-spotify-credentials.sh" ]; then
  echo 'FAIL: guarded remote Spotify update is not implemented.' >&2
  exit 1
fi
source "$directory/apply-spotify-credentials.sh"
umask 077
fixtures="$directory/.state/spotify-guard-test"
mkdir -p "$fixtures"
printf '%s\n' 'POSTGRES_PASSWORD=db-fixture' 'SENTIMENTSCRIBE_JWT_SECRET=jwt-fixture' 'TMDB_API_KEY=tmdb-fixture' 'SPOTIFY_CLIENT_ID=old-id' 'SPOTIFY_CLIENT_SECRET=old-secret' > "$fixtures/current.env"
printf '%s\n' 'POSTGRES_PASSWORD=db-fixture' 'SENTIMENTSCRIBE_JWT_SECRET=jwt-fixture' 'TMDB_API_KEY=tmdb-fixture' 'SPOTIFY_CLIENT_ID=new-id' 'SPOTIFY_CLIENT_SECRET=new-secret' > "$fixtures/candidate.env"
assert_spotify_only_change "$fixtures/current.env" "$fixtures/candidate.env"
sed 's/db-fixture/changed-db/' "$fixtures/candidate.env" > "$fixtures/invalid.env"
if assert_spotify_only_change "$fixtures/current.env" "$fixtures/invalid.env" 2>/dev/null; then
  echo 'FAIL: database-secret change was accepted.' >&2; exit 1
fi
sed '/^SPOTIFY_CLIENT_SECRET=/d' "$fixtures/candidate.env" > "$fixtures/invalid.env"
if assert_spotify_only_change "$fixtures/current.env" "$fixtures/invalid.env" 2>/dev/null; then
  echo 'FAIL: missing Spotify field was accepted.' >&2; exit 1
fi
printf '%s\n' 'SPOTIFY_CLIENT_ID=duplicate' >> "$fixtures/invalid.env"
if assert_spotify_only_change "$fixtures/current.env" "$fixtures/invalid.env" 2>/dev/null; then
  echo 'FAIL: duplicate Spotify field was accepted.' >&2; exit 1
fi
sed 's/new-secret/unsafe secret/' "$fixtures/candidate.env" > "$fixtures/invalid.env"
if assert_spotify_only_change "$fixtures/current.env" "$fixtures/invalid.env" 2>/dev/null; then
  echo 'FAIL: unsafe Spotify value was accepted.' >&2; exit 1
fi
sed -E 's/^(SPOTIFY_CLIENT_(ID|SECRET))=.*/\1=/' "$fixtures/candidate.env" > "$fixtures/deferred.env"
assert_spotify_only_change "$fixtures/current.env" "$fixtures/deferred.env" --defer-spotify
if assert_spotify_only_change "$fixtures/current.env" "$fixtures/deferred.env" 2>/dev/null; then
  echo 'FAIL: empty credentials accepted without explicit deferral.' >&2; exit 1
fi
if assert_spotify_only_change "$fixtures/current.env" "$fixtures/candidate.env" --defer-spotify 2>/dev/null; then
  echo 'FAIL: nonblank credentials accepted in deferred mode.' >&2; exit 1
fi
sed 's/db-fixture/changed-db/' "$fixtures/deferred.env" > "$fixtures/invalid.env"
if assert_spotify_only_change "$fixtures/current.env" "$fixtures/invalid.env" --defer-spotify 2>/dev/null; then
  echo 'FAIL: deferral changed database secrets.' >&2; exit 1
fi
sed 's/^SPOTIFY_CLIENT_ID=$/SPOTIFY_CLIENT_ID=partial/' "$fixtures/deferred.env" > "$fixtures/invalid.env"
if assert_spotify_only_change "$fixtures/current.env" "$fixtures/invalid.env" --defer-spotify 2>/dev/null; then
  echo 'FAIL: partial deferral accepted.' >&2; exit 1
fi
assert_spotify_only_change "$fixtures/deferred.env" "$fixtures/candidate.env"
echo 'PASS: remote Spotify-only guard supports explicit blank-pair deferral and reactivation; unrelated/partial/implicit blank changes rejected. Nonsecret fixtures only.'
