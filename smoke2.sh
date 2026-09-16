#!/usr/bin/env bash
# E2E smoke Tuần 2: sensor CRUD + Redis Streams replay pipeline + readings query.
set -euo pipefail
BASE=http://localhost:8080
EMAIL="smoke2_$(date +%s)@test.com"
PASS="password123"
fail() { echo "FAIL: $1"; exit 1; }
json() { grep -o "\"$1\":[^,}]*" | head -1 | sed "s/\"$1\"://; s/\"//g"; }

echo "1. register + create farm"
TOK=$(curl -sf -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" | json token)
[ -n "$TOK" ] || fail "no token"
AUTH="Authorization: Bearer $TOK"
FARM=$(curl -sf -X POST "$BASE/farms" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"name":"Farm S","location":"Da Lat","cropType":"Tomato"}' | json id)
[ -n "$FARM" ] || fail "no farm id"
echo "   farm=$FARM"

echo "2. create sensors (soil_moisture, temperature)"
SOIL=$(curl -sf -X POST "$BASE/farms/$FARM/sensors" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"type":"soil_moisture","unit":"%","thresholdMin":30,"thresholdMax":90}' | json id)
curl -sf -X POST "$BASE/farms/$FARM/sensors" -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"type":"temperature","unit":"C","thresholdMin":10,"thresholdMax":38}' > /dev/null
[ -n "$SOIL" ] || fail "no soil sensor id"
echo "   soil sensor=$SOIL"

echo "3. list sensors => expect 2"
N=$(curl -sf "$BASE/farms/$FARM/sensors" -H "$AUTH" | grep -o '"id"' | wc -l)
[ "$N" -eq 2 ] || fail "expected 2 sensors, got $N"
echo "   ok 2 sensors"

echo "4. replay historical (stream -> consumer -> postgres)"
curl -sf -X POST "$BASE/farms/$FARM/replay?mode=historical" -H "$AUTH" > /dev/null || fail "replay start"

echo "5. poll readings for soil sensor (7d range) => expect >0"
COUNT=0
for i in $(seq 1 20); do
  COUNT=$(curl -sf "$BASE/sensors/$SOIL/readings?range=7d" -H "$AUTH" | grep -o '"value"' | wc -l)
  [ "$COUNT" -gt 0 ] && break
  sleep 1
done
[ "$COUNT" -gt 0 ] || fail "no readings persisted after replay"
echo "   ok $COUNT readings persisted"

echo "6. ownership: other user cannot read this sensor => expect 404"
TOK2=$(curl -sf -X POST "$BASE/auth/register" -H 'Content-Type: application/json' \
  -d "{\"email\":\"other_$(date +%s)@test.com\",\"password\":\"$PASS\"}" | json token)
CODE=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/sensors/$SOIL/readings" -H "Authorization: Bearer $TOK2")
[ "$CODE" = "404" ] || fail "expected 404 for other user, got $CODE"
echo "   ok 404"

echo "ALL SMOKE-2 PASS ($COUNT readings)"
