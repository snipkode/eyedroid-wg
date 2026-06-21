#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://perumdati.tech/api"
OUTPUT_DIR="${1:-/root/scrcpy/public/apk}"

echo "🔍 Fetching tenant list from $BASE_URL/auth/tenants ..."
RESPONSE=$(curl -sf "$BASE_URL/auth/tenants")
TENANTS=$(echo "$RESPONSE" | python3 -c "
import json, sys
data = json.load(sys.stdin)
print(','.join(t['id'] for t in data['tenants']))
")

if [ -z "$TENANTS" ]; then
  echo "❌ No tenants found" >&2
  exit 1
fi

echo "✅ Tenants: $TENANTS"
mkdir -p "$OUTPUT_DIR"

# Build all flavors in one Gradle invocation
cd "$(dirname "$0")"
./gradlew :ui:assembleRelease -PtenantFlavors="$TENANTS"

# Copy APKs to output dir, named by tenant
echo "$TENANTS" | tr ',' '\n' | while read -r TENANT; do
  FLAVOR=$(echo "$TENANT" | tr '-' '_')
  APK_SRC="ui/build/outputs/apk/${FLAVOR}/release/ui-${FLAVOR}-release.apk"
  if [ -f "$APK_SRC" ]; then
    cp "$APK_SRC" "$OUTPUT_DIR/eyedroid-${TENANT}.apk"
    echo "📦 $OUTPUT_DIR/eyedroid-${TENANT}.apk"
  else
    echo "⚠️  APK not found: $APK_SRC"
  fi
done

echo "✅ Done. APKs in $OUTPUT_DIR"
