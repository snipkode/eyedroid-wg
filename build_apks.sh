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

# Build all flavors — release + debug
cd "$(dirname "$0")"
./gradlew :ui:assembleRelease :ui:assembleDebug \
  -PtenantFlavors="$TENANTS" \
  -PtenantNames="$TENANT_NAMES"

# Deploy APKs
echo "$TENANTS" | tr ',' '\n' | while read -r TENANT; do
  FLAVOR=$(echo "$TENANT" | tr '-' '_')

  # Release
  APK_REL="ui/build/outputs/apk/${FLAVOR}/release/ui-${FLAVOR}-release.apk"
  # Debug
  APK_DBG="ui/build/outputs/apk/${FLAVOR}/debug/ui-${FLAVOR}-debug.apk"

  if [ -n "${APK_UPLOAD_URL:-}" ]; then
    for TYPE in release debug; do
      APK_SRC="ui/build/outputs/apk/${FLAVOR}/${TYPE}/ui-${FLAVOR}-${TYPE}.apk"
      [ -f "$APK_SRC" ] || { echo "⚠️  Not found: $APK_SRC"; continue; }
      ENDPOINT="$APK_UPLOAD_URL/api/admin/apk/$TENANT"
      [ "$TYPE" = "debug" ] && ENDPOINT="$ENDPOINT/debug"
      HTTP=$(curl -sf -o /dev/null -w "%{http_code}" \
        -X POST "$ENDPOINT" \
        -H "Authorization: Bearer ${APK_UPLOAD_TOKEN:-}" \
        -F "file=@$APK_SRC")
      [ "$HTTP" = "200" ] && echo "✅ Uploaded: eyedroid-${TENANT}-${TYPE}.apk" || echo "❌ Upload failed $TENANT/$TYPE (HTTP $HTTP)"
    done
  else
    mkdir -p "$OUTPUT_DIR"
    if [ -f "$APK_REL" ]; then
      cp "$APK_REL" "$OUTPUT_DIR/eyedroid-${TENANT}.apk"
      echo "📦 $OUTPUT_DIR/eyedroid-${TENANT}.apk"
    fi
    if [ -f "$APK_DBG" ]; then
      cp "$APK_DBG" "$OUTPUT_DIR/eyedroid-${TENANT}-debug.apk"
      echo "🐛 $OUTPUT_DIR/eyedroid-${TENANT}-debug.apk"
    fi
  fi
done

echo "✅ Done."
