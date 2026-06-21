#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./build_one.sh <tenantId> <"Tenant Name"> [OUTPUT_DIR]
#
# Contoh:
#   ./build_one.sh perumda-ti "Perumda TI"
#   ./build_one.sh system "EyeDroid" /tmp/apk-out
#
# Upload mode:
#   APK_UPLOAD_URL=http://perumdati.tech APK_UPLOAD_TOKEN=<jwt> ./build_one.sh system "EyeDroid"

if [ $# -lt 2 ]; then
  echo "Usage: $0 <tenantId> <\"Tenant Name\"> [OUTPUT_DIR]" >&2
  exit 1
fi

TENANT_ID="$1"
TENANT_NAME="$2"
OUTPUT_DIR="${3:-/root/scrcpy/public/apk}"

FLAVOR=$(echo "$TENANT_ID" | tr '-' '_')

echo "🏗️  Building tenant: $TENANT_ID ($TENANT_NAME)"

cd "$(dirname "$0")"
./gradlew :ui:assembleRelease :ui:assembleDebug \
  -PtenantFlavors="$TENANT_ID" \
  -PtenantNames="$TENANT_NAME"

APK_REL="ui/build/outputs/apk/${FLAVOR}/release/ui-${FLAVOR}-release.apk"
APK_DBG="ui/build/outputs/apk/${FLAVOR}/debug/ui-${FLAVOR}-debug.apk"

if [ -n "${APK_UPLOAD_URL:-}" ]; then
  for TYPE in release debug; do
    APK_SRC="ui/build/outputs/apk/${FLAVOR}/${TYPE}/ui-${FLAVOR}-${TYPE}.apk"
    [ -f "$APK_SRC" ] || { echo "⚠️  Not found: $APK_SRC"; continue; }
    ENDPOINT="$APK_UPLOAD_URL/api/admin/apk/$TENANT_ID"
    [ "$TYPE" = "debug" ] && ENDPOINT="$ENDPOINT/debug"
    HTTP=$(curl -sf -o /dev/null -w "%{http_code}" \
      -X POST "$ENDPOINT" \
      -H "Authorization: Bearer ${APK_UPLOAD_TOKEN:-}" \
      -F "file=@$APK_SRC")
    [ "$HTTP" = "200" ] && echo "✅ Uploaded: eyedroid-${TENANT_ID}-${TYPE}.apk" || echo "❌ Upload failed (HTTP $HTTP)"
  done
else
  mkdir -p "$OUTPUT_DIR"
  if [ -f "$APK_REL" ]; then
    cp "$APK_REL" "$OUTPUT_DIR/eyedroid-${TENANT_ID}.apk"
    echo "📦 $OUTPUT_DIR/eyedroid-${TENANT_ID}.apk"
  fi
  if [ -f "$APK_DBG" ]; then
    cp "$APK_DBG" "$OUTPUT_DIR/eyedroid-${TENANT_ID}-debug.apk"
    echo "🐛 $OUTPUT_DIR/eyedroid-${TENANT_ID}-debug.apk"
  fi
fi

echo "✅ Done: $TENANT_ID"
