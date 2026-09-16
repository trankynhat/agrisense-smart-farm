#!/usr/bin/env bash
# E2E smoke T3-5: demo seeder + replay → alerts (threshold) + AI analyze/explain.
set -euo pipefail
BASE=http://localhost:8080
fail() { echo "FAIL: $1"; exit 1; }
json() { grep -o "\"$1\":[^,}]*" | head -1 | sed "s/\"$1\"://; s/\"//g"; }
count() { tr ',' '\n' | grep -c '"id"' || true; }  # || true: 0 matches must not trip set -e/pipefail

echo "1. login demo seeded user"
TOK=$(curl -sf -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"demo@agrisense.dev","password":"demo1234"}' | json token)
[ -n "$TOK" ] || fail "demo login (seeder chưa chạy?)"
A="Authorization: Bearer $TOK"
echo "   demo ok"

echo "2. demo farm + sensors"
FARM=$(curl -sf "$BASE/farms" -H "$A" | json id)
[ -n "$FARM" ] || fail "no demo farm"
NS=$(curl -sf "$BASE/farms/$FARM/sensors" -H "$A" | count)
[ "$NS" -ge 5 ] || fail "expected 5 sensors, got $NS"
echo "   farm=$FARM sensors=$NS"

echo "3. replay historical → pipeline"
curl -sf -X POST "$BASE/farms/$FARM/replay?mode=historical&intervalMs=0" -H "$A" > /dev/null

echo "4. wait alerts (soil dips < 30 threshold)"
NA=0
for i in $(seq 1 25); do
  NA=$(curl -sf "$BASE/farms/$FARM/alerts" -H "$A" | count)
  [ "$NA" -gt 0 ] && break
  sleep 1
done
[ "$NA" -gt 0 ] || fail "no alerts generated"
echo "   ok $NA alerts"

echo "5. AI analyze (stub, no key)"
TXT=$(curl -sf -X POST "$BASE/farms/$FARM/analyze" -H "$A" | json text)
[ -n "$TXT" ] || fail "analyze empty"
echo "   analyze ok"

echo "6. AI explain first alert"
AID=$(curl -sf "$BASE/farms/$FARM/alerts" -H "$A" | json id)
EXP=$(curl -sf -X POST "$BASE/alerts/$AID/explain" -H "$A" | json text)
[ -n "$EXP" ] || fail "explain empty"
echo "   explain ok"

echo "7. resolve alert"
curl -sf -X POST "$BASE/alerts/$AID/resolve" -H "$A" | grep -q '"resolved":true' || fail "resolve"
echo "   resolved ok"

echo "ALL SMOKE-3 PASS ($NA alerts)"
