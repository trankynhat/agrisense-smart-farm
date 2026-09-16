#!/usr/bin/env bash
# E2E smoke: register → login → create farm → list → verify auth rejection.
# ponytail: bash+curl script, không phải test framework. Đủ cho Tuần 1; JUnit test add sau.
set -euo pipefail
BASE=http://localhost:8080
EMAIL="smoke$(date +%s)@test.com"
PASS="password123"
fail() { echo "FAIL: $1"; exit 1; }

echo "1. register"
TOK=$(curl -sf -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
[ -n "$TOK" ] || fail "no token from register"
echo "   token ok"

echo "2. no-token /farms => expect 401"
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/farms")
[ "$CODE" = "401" ] || fail "expected 401, got $CODE"
echo "   401 ok"

echo "3. create farm"
curl -sf -X POST "$BASE/farms" -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' \
  -d '{"name":"Farm A","location":"Da Lat","cropType":"Tomato"}' > /dev/null || fail "create farm"
echo "   created"

echo "4. list farms => expect Farm A"
curl -sf "$BASE/farms" -H "Authorization: Bearer $TOK" | grep -q "Farm A" || fail "farm not listed"
echo "   listed"

echo "5. login existing user"
curl -sf -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | grep -q '"token"' || fail "login"
echo "   login ok"

echo "ALL SMOKE PASS"
