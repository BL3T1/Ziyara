#!/usr/bin/env sh
# Generates a local CA and a server TLS certificate with IP SAN.
# Run once before the first `docker compose --profile ipmode up --build`.
#
# Outputs (all in this directory):
#   ca.key        — CA private key   (keep secret, never commit)
#   ca.crt        — CA certificate   (install on clients / Android raw resource)
#   server.key    — Server private key (never commit)
#   server.crt    — Server certificate signed by the CA
#
# Usage:
#   cd infra/certs
#   sh generate.sh                          # uses BIND_IP=10.45.30.145
#   BIND_IP=192.168.1.50 sh generate.sh     # override IP

set -eu

BIND_IP="${BIND_IP:-10.45.30.145}"
CERT_DIR="$(cd "$(dirname "$0")" && pwd)"

CA_KEY="$CERT_DIR/ca.key"
CA_CRT="$CERT_DIR/ca.crt"
SERVER_KEY="$CERT_DIR/server.key"
SERVER_CSR="$CERT_DIR/server.csr"
SERVER_CRT="$CERT_DIR/server.crt"
SAN_CNF="$CERT_DIR/san.cnf"

echo "==> Generating CA private key (4096-bit RSA)..."
openssl genrsa -out "$CA_KEY" 4096

echo "==> Generating self-signed CA certificate (10 years)..."
openssl req -new -x509 -days 3650 \
    -key "$CA_KEY" \
    -subj "/CN=Ziyara-Local-CA/O=Ziyara/C=SY" \
    -out "$CA_CRT"

echo "==> Generating server private key (2048-bit RSA)..."
openssl genrsa -out "$SERVER_KEY" 2048

echo "==> Writing SAN config for IP: $BIND_IP ..."
cat > "$SAN_CNF" <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions     = v3_req
prompt             = no

[req_distinguished_name]
CN = $BIND_IP

[v3_req]
keyUsage         = keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName   = @alt_names

[alt_names]
IP.1 = $BIND_IP
IP.2 = 127.0.0.1
EOF

echo "==> Generating server CSR..."
openssl req -new \
    -key "$SERVER_KEY" \
    -out "$SERVER_CSR" \
    -config "$SAN_CNF"

echo "==> Signing server cert with CA (825 days — iOS maximum)..."
openssl x509 -req -days 825 \
    -in "$SERVER_CSR" \
    -CA "$CA_CRT" \
    -CAkey "$CA_KEY" \
    -CAcreateserial \
    -extfile "$SAN_CNF" \
    -extensions v3_req \
    -out "$SERVER_CRT"

# Clean up CSR and SAN config (not needed after signing)
rm -f "$SERVER_CSR" "$SAN_CNF"

echo ""
echo "Certificate files ready:"
printf "  CA cert:     %s\n" "$CA_CRT"
printf "  Server cert: %s\n" "$SERVER_CRT"
printf "  Server key:  %s\n" "$SERVER_KEY"
echo ""
echo "──────────────────────────────────────────────────────────"
echo "NEXT STEPS"
echo "──────────────────────────────────────────────────────────"
echo ""
echo "1. Copy the CA cert into the Flutter Android raw resources:"
echo ""
FLUTTER_RAW="SYRIA-TOURISM-APP-main/SYRIA-TOURISM-APP-main/android/app/src/main/res/raw"
printf "   mkdir -p %s\n" "$FLUTTER_RAW"
printf "   cp %s %s/ziyara_ca.crt\n" "$CA_CRT" "$FLUTTER_RAW"
echo ""
echo "2. Start the stack:"
echo "   docker compose --profile ipmode up -d --build"
echo ""
echo "3. Build the Flutter app pointing at the HTTPS API:"
printf "   flutter build apk --dart-define=ZIYARA_API_URL=https://%s:7005/api/v1\n" "$BIND_IP"
echo ""
echo "4. (Optional) Install ca.crt on your browser/OS to avoid the SSL warning."
echo "──────────────────────────────────────────────────────────"
