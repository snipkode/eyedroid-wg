#!/bin/bash
# setup-env.sh — Setup Android build environment on Ubuntu VPS
# Usage: bash setup-env.sh
set -e

ANDROID_SDK_ROOT="$HOME/android-sdk"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

echo "=== EyeDroid VPN — Build Environment Setup ==="
echo "OS: $(lsb_release -d | cut -f2)"

# 1. Java 17
echo "[1/4] Installing JDK 17..."
apt-get update -qq && apt-get install -y openjdk-17-jdk-headless g++ git unzip curl > /dev/null
java -version

# 2. Android SDK command-line tools
echo "[2/4] Installing Android SDK command-line tools..."
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
curl -sL "$CMDLINE_TOOLS_URL" -o /tmp/cmdline-tools.zip
unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_SDK_ROOT/cmdline-tools"
mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
rm /tmp/cmdline-tools.zip

# 3. SDK components
echo "[3/4] Installing SDK components (platform, build-tools, NDK, CMake)..."
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

yes | sdkmanager --licenses > /dev/null 2>&1
sdkmanager \
  "platforms;android-36" \
  "build-tools;36.0.0" \
  "ndk;27.0.12077973" \
  "cmake;3.22.1" \
  --sdk_root="$ANDROID_SDK_ROOT" 2>&1 | grep -E "Install|Done|Error" || true

# 4. Environment variables
echo "[4/4] Writing environment variables to ~/.bashrc..."
cat >> ~/.bashrc << EOF

# Android SDK — added by setup-env.sh
export ANDROID_HOME="$ANDROID_SDK_ROOT"
export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
export PATH="\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH"
EOF

# 5. local.properties for this project
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "sdk.dir=$ANDROID_SDK_ROOT" > "$SCRIPT_DIR/local.properties"
echo "local.properties written → $SCRIPT_DIR/local.properties"

echo ""
echo "=== Setup complete! ==="
echo "Run: source ~/.bashrc"
echo "Then: ./gradlew :ui:assembleDebug --no-daemon"
echo "APK:  ui/build/outputs/apk/debug/ui-debug.apk"
