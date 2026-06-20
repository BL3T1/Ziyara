#!/bin/sh
# Ziyara — Nginx Proxy Manager auto-initialiser
#
# Runs once on first boot and configures all proxy hosts via the NPM API.
# Idempotent: if proxy hosts already exist it exits without changes.
# If authentication fails (custom password already set) it exits cleanly.
#
# Proxy hosts configured:
#   app.ziyara.local        → company-portmode:80   (WebSocket enabled)
#   partners.ziyara.local   → provider-portmode:80  (WebSocket enabled)
#   www.ziyara.local        → landing-portmode:80   (WebSocket enabled)
#   api.ziyara.local        → backend:8080          (WebSocket enabled)
#   grafana.ziyara.local    → grafana:3000
#   prometheus.ziyara.local → prometheus:9090
#   pgadmin.ziyara.local    → pgadmin:80
#
# On the VPS: replace *.ziyara.local with your real domain names in the NPM UI.

NPM_URL="http://nginx-proxy-manager:81"
ADMIN_EMAIL="${NPM_ADMIN_EMAIL:-admin@example.com}"
ADMIN_PASS="${NPM_ADMIN_PASS:-changeme}"

# ── Wait for NPM to be ready (up to 2 minutes) ──────────────────────────────
echo "[npm-init] Waiting for Nginx Proxy Manager to be ready..."
ATTEMPTS=0
until curl -sf "$NPM_URL/" >/dev/null 2>&1; do
  ATTEMPTS=$((ATTEMPTS + 1))
  if [ "$ATTEMPTS" -ge 40 ]; then
    echo "[npm-init] Timed out. Configure proxy hosts manually at $NPM_URL"
    exit 0
  fi
  sleep 3
done
echo "[npm-init] NPM is up."

# ── Authenticate ─────────────────────────────────────────────────────────────
echo "[npm-init] Authenticating as $ADMIN_EMAIL..."
TOKEN=$(curl -sf -X POST "$NPM_URL/api/tokens" \
  -H "Content-Type: application/json" \
  -d "{\"identity\":\"$ADMIN_EMAIL\",\"secret\":\"$ADMIN_PASS\"}" \
  | grep -o '"token":"[^"]*"' | cut -d'"' -f4 || true)

if [ -z "$TOKEN" ]; then
  echo "[npm-init] Authentication failed — NPM may already have a custom password."
  echo "[npm-init] Manage proxy hosts manually at $NPM_URL"
  exit 0
fi

# ── Idempotency check ────────────────────────────────────────────────────────
HOST_COUNT=$(curl -sf -H "Authorization: Bearer $TOKEN" \
  "$NPM_URL/api/nginx/proxy-hosts" | grep -c '"id"' || true)

if [ "${HOST_COUNT:-0}" -gt "0" ]; then
  echo "[npm-init] Already configured ($HOST_COUNT proxy host(s) found). Skipping."
  exit 0
fi

# ── Create proxy hosts ───────────────────────────────────────────────────────
add() {
  DOMAIN=$1
  HOST=$2
  PORT=$3
  WS=$4

  curl -sf -X POST "$NPM_URL/api/nginx/proxy-hosts" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"domain_names\": [\"$DOMAIN\"],
      \"forward_scheme\": \"http\",
      \"forward_host\": \"$HOST\",
      \"forward_port\": $PORT,
      \"access_list_id\": \"0\",
      \"certificate_id\": 0,
      \"ssl_forced\": false,
      \"block_exploits\": true,
      \"websocket_support\": $WS,
      \"http2_support\": false,
      \"locations\": [],
      \"advanced_config\": \"\"
    }" >/dev/null 2>&1 || true

  printf "[npm-init]   %-30s -> %s:%s\n" "$DOMAIN" "$HOST" "$PORT"
}

echo "[npm-init] Creating proxy hosts..."

# App surfaces — each frontend's nginx.conf already routes /api/ and /api/v1/ws
# to backend:8080 internally, so NPM only needs to forward to port 80.
add "app.ziyara.local"         "company-portmode"  80   "true"
add "partners.ziyara.local"    "provider-portmode" 80   "true"
add "www.ziyara.local"         "landing-portmode"  80   "true"

# API direct — for mobile apps, external integrations, Swagger
add "api.ziyara.local"         "backend"           8080 "true"

# Monitoring — only available when --profile monitoring is active
add "grafana.ziyara.local"     "grafana"           3000 "false"
add "prometheus.ziyara.local"  "prometheus"        9090 "false"

# Admin
add "pgadmin.ziyara.local"     "pgadmin"           80   "false"

echo ""
echo "[npm-init] All proxy hosts created."
echo "[npm-init] Admin UI : $NPM_URL"
echo "[npm-init] Login    : $ADMIN_EMAIL / $ADMIN_PASS"
echo "[npm-init] IMPORTANT: Change the default NPM password immediately at $NPM_URL"
echo ""
echo "[npm-init] For local testing add to /etc/hosts (Windows: C:\\Windows\\System32\\drivers\\etc\\hosts):"
echo "[npm-init]   127.0.0.1  app.ziyara.local partners.ziyara.local www.ziyara.local"
echo "[npm-init]   127.0.0.1  api.ziyara.local grafana.ziyara.local prometheus.ziyara.local pgadmin.ziyara.local"
echo ""
echo "[npm-init] On the VPS: replace *.ziyara.local with real domains in the NPM UI,"
echo "[npm-init]             then issue Let's Encrypt certificates per proxy host."
