#!/usr/bin/env bash
# Fast debug build + install ke device via ADB
# Usage: ./dev.sh [tenantId] [TenantName]
set -euo pipefail
cd "$(dirname "$0")"

TENANT_ID="${1:-system}"
TENANT_NAME="${2:-EyeDroid}"
FLAVOR=$(echo "$TENANT_ID" | tr '-' '_')
APK="ui/build/outputs/apk/${FLAVOR}/debug/ui-${FLAVOR}-debug.apk"

echo "🔨 Building debug: $TENANT_ID..."
./gradlew :ui:assembleDebug \
  -PtenantFlavors="$TENANT_ID" \
  -PtenantNames="$TENANT_NAME" \
  --quiet

echo "📲 Installing..."
adb install -r "$APK"
adb shell am start -n "com.eyedroid.vpn.debug/com.eyedroid.vpn.ui.login.LoginActivity"
echo "✅ Done"
