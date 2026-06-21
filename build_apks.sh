#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./build_apks.sh [OUTPUT_DIR]
#
# Upload mode (remote server):
#   APK_UPLOAD_URL=http://perumdati.tech APK_UPLOAD_TOKEN=<jwt> ./build_apks.sh

BASE_URL="http://perumdati.tech/api"
OUTPUT_DIR="${1:-/root/scrcpy/public/apk}"

echo "🔍 Fetching tenant list from $BASE_URL/auth/tenants ..."
RESPONSE=$(curl -sf "$BASE_URL/auth/tenants")

TENANTS=$(echo "$RESPONSE" | python3 -c "
import json, sys
data = json.load(sys.stdin)
print(','.join(t['id'] for t in data['tenants']))
")

# tenant names joined by | (pipe) — safe separator
TENANT_NAMES=$(echo "$RESPONSE" | python3 -c "
import json, sys
data = json.load(sys.stdin)
print('|'.join(t.get('name', t['id']) for t in data['tenants']))
")

if [ -z "$TENANTS" ]; then
  echo "❌ No tenants found" >&2
  exit 1
fi

echo "✅ Tenants: $TENANTS"
echo "✅ Names:   $TENANT_NAMES"

# Build all flavors in one Gradle invocation
cd "$(dirname "$0")"
./gradlew :ui:assembleRelease \
  -PtenantFlavors="$TENANTS" \
  -PtenantNames="$TENANT_NAMES"

# Deploy APKs
echo "$TENANTS" | tr ',' '\n' | while read -r TENANT; do
  FLAVOR=$(echo "$TENANT" | tr '-' '_')
  APK_SRC="ui/build/outputs/apk/${FLAVOR}/release/ui-${FLAVOR}-release.apk"

  if [ ! -f "$APK_SRC" ]; then
    echo "⚠️  APK not found: $APK_SRC"
    continue
  fi

  if [ -n "${APK_UPLOAD_URL:-}" ]; then
    echo "⬆️  Uploading eyedroid-${TENANT}.apk ..."
    HTTP=$(curl -sf -o /dev/null -w "%{http_code}" \
      -X POST "$APK_UPLOAD_URL/api/admin/apk/$TENANT" \
      -H "Authorization: Bearer ${APK_UPLOAD_TOKEN:-}" \
      -F "file=@$APK_SRC")
    if [ "$HTTP" = "200" ]; then
      echo "✅ Uploaded: eyedroid-${TENANT}.apk"
    else
      echo "❌ Upload failed for $TENANT (HTTP $HTTP)"
    fi
  else
    mkdir -p "$OUTPUT_DIR"
    cp "$APK_SRC" "$OUTPUT_DIR/eyedroid-${TENANT}.apk"
    echo "📦 $OUTPUT_DIR/eyedroid-${TENANT}.apk"
  fi
done

echo "✅ Done."
