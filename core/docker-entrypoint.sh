#!/bin/sh
set -e
# Named volume mounts are often root:root; the app runs as ziyarah and must write uploads here.
MEDIA_ROOT="${APP_MEDIA_STORAGE_ROOT:-/data/media}"
mkdir -p "$MEDIA_ROOT"
chown -R ziyarah:ziyarah "$MEDIA_ROOT"
chmod -R u+rwX "$MEDIA_ROOT"

exec su-exec ziyarah:ziyarah sh -c 'exec java $JAVA_OPTS -jar /app/app.jar'
