#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./build_one.sh                          ← interactive mode
#   ./build_one.sh <tenantId> "<Name>"      ← non-interactive (debug+release)
#
# Upload mode:
#   APK_UPLOAD_URL=http://perumdati.tech APK_UPLOAD_TOKEN=<jwt> ./build_one.sh

OUTPUT_DIR="${APK_OUTPUT_DIR:-/root/scrcpy/public/apk}"
cd "$(dirname "$0")"

# ─── Non-interactive mode ─────────────────────────────────────────────────────
if [ $# -ge 2 ]; then
  TENANT_ID="$1"
  TENANT_NAME="$2"
  BUILD_TYPE="both"
else

# ─── Interactive mode ─────────────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════╗"
echo "║       EyeDroid APK Builder           ║"
echo "╚══════════════════════════════════════╝"
echo ""

# Step 1: Tenant ID
read -rp "▶ Tenant ID       : " TENANT_ID
[ -z "$TENANT_ID" ] && { echo "❌ Tenant ID tidak boleh kosong"; exit 1; }

# Step 2: Tenant Name
read -rp "▶ Tenant Name     : " TENANT_NAME
[ -z "$TENANT_NAME" ] && { echo "❌ Tenant Name tidak boleh kosong"; exit 1; }

# Step 3: Build variant
echo ""
echo "Pilih build variant:"
echo "  1) Debug"
echo "  2) Release"
echo "  3) Debug + Release"
echo "  4) Upload ke server (Release)"
read -rp "▶ Pilihan [1-4]   : " CHOICE

case "$CHOICE" in
  1) BUILD_TYPE="debug" ;;
  2) BUILD_TYPE="release" ;;
  3) BUILD_TYPE="both" ;;
  4) BUILD_TYPE="upload"
     read -rp "▶ Upload URL      : " APK_UPLOAD_URL
     read -rp "▶ JWT Token       : " APK_UPLOAD_TOKEN
     export APK_UPLOAD_URL APK_UPLOAD_TOKEN
     ;;
  *) echo "❌ Pilihan tidak valid"; exit 1 ;;
esac

fi  # end interactive mode

# ─── Build ────────────────────────────────────────────────────────────────────
FLAVOR=$(echo "$TENANT_ID" | tr '-' '_')

echo ""
echo "🏗️  Building: $TENANT_ID ($TENANT_NAME) — variant: ${BUILD_TYPE}"
echo ""

case "$BUILD_TYPE" in
  debug)   TASKS=":ui:assembleDebug" ;;
  release|upload) TASKS=":ui:assembleRelease" ;;
  both)    TASKS=":ui:assembleDebug :ui:assembleRelease" ;;
esac

./gradlew $TASKS \
  -PtenantFlavors="$TENANT_ID" \
  -PtenantNames="$TENANT_NAME"

# ─── Deploy / Copy ────────────────────────────────────────────────────────────
APK_REL="ui/build/outputs/apk/${FLAVOR}/release/ui-${FLAVOR}-release.apk"
APK_DBG="ui/build/outputs/apk/${FLAVOR}/debug/ui-${FLAVOR}-debug.apk"

if [ "$BUILD_TYPE" = "upload" ] && [ -n "${APK_UPLOAD_URL:-}" ]; then
  [ -f "$APK_REL" ] || { echo "❌ APK release tidak ditemukan: $APK_REL"; exit 1; }
  HTTP=$(curl -sf -o /dev/null -w "%{http_code}" \
    -X POST "$APK_UPLOAD_URL/api/admin/apk/$TENANT_ID" \
    -H "Authorization: Bearer ${APK_UPLOAD_TOKEN:-}" \
    -F "file=@$APK_REL")
  [ "$HTTP" = "200" ] \
    && echo "✅ Uploaded: eyedroid-${TENANT_ID}.apk" \
    || echo "❌ Upload gagal (HTTP $HTTP)"
else
  mkdir -p "$OUTPUT_DIR"
  if [ -f "$APK_REL" ]; then
    cp "$APK_REL" "$OUTPUT_DIR/eyedroid-${TENANT_ID}.apk"
    echo "📦 Release : $OUTPUT_DIR/eyedroid-${TENANT_ID}.apk"
  fi
  if [ -f "$APK_DBG" ]; then
    cp "$APK_DBG" "$OUTPUT_DIR/eyedroid-${TENANT_ID}-debug.apk"
    echo "🐛 Debug   : $OUTPUT_DIR/eyedroid-${TENANT_ID}-debug.apk"
  fi
fi

echo ""
echo "✅ Done: $TENANT_ID"
