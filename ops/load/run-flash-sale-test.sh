#!/usr/bin/env bash
# Flash-sale load test orchestration (Phase 9).
#
# 1. Seeds BUYERS load-test users (clones one registered user's bcrypt hash)
# 2. Restarts the backend with the auth rate limit raised (500 logins in setup)
# 3. Creates + activates a STOCK-unit flash sale as admin
# 4. Runs the k6 burst (one purchase attempt per user)
# 5. Verifies the accounting: Redis stock == 0, buyers == STOCK,
#    CONFIRMED reservations == STOCK, orders == STOCK  → zero oversell
#
# Usage: ./run-flash-sale-test.sh   (from anywhere; needs docker compose stack up)

set -euo pipefail
cd "$(dirname "$0")/../.."

BUYERS=${BUYERS:-500}
STOCK=${STOCK:-300}
BASE=${BASE_URL:-http://localhost:8080}
PASSWORD='LoadTest@123'

psql_cmd() { docker exec myshop-postgres psql -U myshop_user -d myshop -tA -c "$1"; }

echo "── 1/5 Raising auth rate limit for the login storm ──────────────────"
RATE_LIMIT_AUTH_PER_MIN=100000 RATE_LIMIT_API_PER_MIN=100000 docker compose up -d backend
until curl -sf "$BASE/actuator/health" >/dev/null; do sleep 2; done

echo "── 2/5 Seeding $BUYERS load-test users ──────────────────────────────"
# Register user 0 through the API (produces a valid bcrypt hash)...
curl -sf -X POST "$BASE/api/v1/auth/register" -H 'Content-Type: application/json' \
  -d "{\"fullName\":\"Load Tester\",\"email\":\"loadtest0@example.com\",\"password\":\"$PASSWORD\"}" >/dev/null \
  || echo "(loadtest0 already exists)"
# ...then clone its hash for the rest — 500 API registrations would be slow.
psql_cmd "INSERT INTO users (id, email, password_hash, full_name, role, is_active, created_at, updated_at)
          SELECT gen_random_uuid(), 'loadtest' || n || '@example.com',
                 (SELECT password_hash FROM users WHERE email='loadtest0@example.com'),
                 'Load Tester ' || n, 'USER', true, now(), now()
          FROM generate_series(1, $((BUYERS - 1))) n
          ON CONFLICT (email) DO NOTHING;" >/dev/null
echo "users ready: $(psql_cmd "SELECT count(*) FROM users WHERE email LIKE 'loadtest%'")"

echo "── 3/5 Creating + activating a $STOCK-unit flash sale ───────────────"
ADMIN_TOKEN=$(curl -sf -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"admin@myshop.com","password":"Admin@123"}' | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['accessToken'])")
PRODUCT_ID=$(psql_cmd "SELECT id FROM products WHERE is_active AND stock_quantity >= $STOCK ORDER BY stock_quantity DESC LIMIT 1")
[ -n "$PRODUCT_ID" ] || { echo "no product with enough stock — topping one up"; \
  psql_cmd "UPDATE products SET stock_quantity = $((STOCK + 50)) WHERE id = (SELECT id FROM products WHERE is_active LIMIT 1)"; \
  PRODUCT_ID=$(psql_cmd "SELECT id FROM products WHERE is_active AND stock_quantity >= $STOCK LIMIT 1"); }

SALE_ID=$(curl -sf -X POST "$BASE/api/v1/admin/flash-sales" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"productId\":\"$PRODUCT_ID\",\"salePrice\":9.99,\"totalStock\":$STOCK,
       \"startsAt\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",
       \"endsAt\":\"$(date -u -v+1H +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -d '+1 hour' +%Y-%m-%dT%H:%M:%SZ)\"}" \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['data']['id'])")
curl -sf -X POST "$BASE/api/v1/admin/flash-sales/$SALE_ID/activate" \
  -H "Authorization: Bearer $ADMIN_TOKEN" >/dev/null
echo "sale $SALE_ID ACTIVE with $STOCK units"

echo "── 4/5 k6 burst: $BUYERS buyers → $STOCK units ──────────────────────"
# k6 runs INSIDE the compose network hitting backend:8080 directly.
# Through the docker-published port, macOS/Docker port forwarding drops SYNs
# under a 500-connection burst (exact 1s TCP retransmit plateau in the tail)
# — an artifact of the harness, not the application. Measured server-side
# p99 was 223ms while the forwarded path showed 1.02s.
docker run --rm --network myshop-network \
  -v "$(pwd)/ops/load:/scripts:ro" \
  -e SALE_ID="$SALE_ID" -e BUYERS="$BUYERS" -e STOCK="$STOCK" \
  -e BASE_URL="http://backend:8080" \
  grafana/k6:0.52.0 run /scripts/flash-sale.js
K6_EXIT=$?

echo "── 5/5 Post-run accounting (waiting for the worker to drain) ────────"
for i in $(seq 1 30); do
  CONFIRMED=$(psql_cmd "SELECT count(*) FROM flash_sale_reservations WHERE sale_id='$SALE_ID' AND status='CONFIRMED'")
  [ "$CONFIRMED" -ge "$STOCK" ] && break
  sleep 2
done
REDIS_STOCK=$(docker exec myshop-redis redis-cli -a "$(grep REDIS_PASSWORD .env | cut -d= -f2)" GET "flash:$SALE_ID:stock" 2>/dev/null)
BUYERS_SET=$(docker exec myshop-redis redis-cli -a "$(grep REDIS_PASSWORD .env | cut -d= -f2)" SCARD "flash:$SALE_ID:buyers" 2>/dev/null)
ORDERS=$(psql_cmd "SELECT count(*) FROM flash_sale_reservations r JOIN orders o ON o.id = r.order_id WHERE r.sale_id='$SALE_ID'")

echo "redis remaining stock : $REDIS_STOCK   (expected 0)"
echo "redis buyers          : $BUYERS_SET   (expected $STOCK)"
echo "confirmed reservations: $CONFIRMED   (expected $STOCK)"
echo "materialized orders   : $ORDERS   (expected $STOCK)"

FAIL=0
[ "$REDIS_STOCK" = "0" ] || FAIL=1
[ "$BUYERS_SET" = "$STOCK" ] || FAIL=1
[ "$CONFIRMED" = "$STOCK" ] || FAIL=1
[ "$ORDERS" = "$STOCK" ] || FAIL=1

echo "── Restoring normal rate limits ──────────────────────────────────────"
docker compose up -d backend >/dev/null 2>&1

if [ $FAIL -eq 0 ] && [ ${K6_EXIT:-0} -eq 0 ]; then
  echo "RESULT: PASS — zero oversell, exact accounting, thresholds met"
else
  echo "RESULT: FAIL — see numbers above"
  exit 1
fi
