# Android raw resources — TLS certificate

Place `ziyara_ca.crt` here before building the release APK for the `ipmode` deployment.

## How to generate it

From the repo root, run the certificate generator:

```sh
cd infra/certs
sh generate.sh
```

Then copy the CA certificate into this directory:

```sh
cp infra/certs/ca.crt \
   SYRIA-TOURISM-APP-main/SYRIA-TOURISM-APP-main/android/app/src/main/res/raw/ziyara_ca.crt
```

## Why this is needed

The `ipmode` profile uses a self-signed certificate issued by the Ziyara local CA.
Android release builds block all cleartext traffic and only trust system CAs by default.
The `network_security_config.xml` tells Android to also trust `@raw/ziyara_ca` for
connections to `10.45.30.145`, allowing the Flutter app to reach the HTTPS API.

## Build command

```sh
flutter build apk \
  --dart-define=ZIYARA_API_URL=https://10.45.30.145:9000/api/v1
```

## Important

- `ziyara_ca.crt` is listed in `.gitignore` — never commit it.
- Re-run `generate.sh` and replace this file whenever you rotate the certificate.
