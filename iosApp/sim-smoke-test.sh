#!/usr/bin/env bash
# iOS simulator smoke test — builds the iosApp, boots the simulator, installs, launches,
# and asserts the app stays alive (did not crash on the shared CMP UI). Captures a screenshot.
# This is the lightweight device test for iOS; a full XCUITest target is a follow-up.
#
# Usage: ./iosApp/sim-smoke-test.sh [SimulatorName]   (default: iPhone 16)
set -euo pipefail

SIM="${1:-iPhone 16}"
APP_ID="org.viewrr.iosApp"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/iosApp/build/out"
SHOT="${SHOT:-/tmp/viewrr-ios-smoke.png}"

echo "==> Building iosApp (arm64 simulator)"
xcodebuild -project "$ROOT/iosApp/iosApp.xcodeproj" -target iosApp -sdk iphonesimulator \
  -configuration Debug ARCHS=arm64 ONLY_ACTIVE_ARCH=YES \
  CONFIGURATION_BUILD_DIR="$OUT" build >/dev/null

echo "==> Booting simulator: $SIM"
xcrun simctl boot "$SIM" 2>/dev/null || true
xcrun simctl bootstatus "$SIM" >/dev/null 2>&1 || true

echo "==> Installing + launching"
xcrun simctl terminate "$SIM" "$APP_ID" 2>/dev/null || true
xcrun simctl install "$SIM" "$OUT/iosApp.app"
xcrun simctl launch "$SIM" "$APP_ID" >/dev/null

echo "==> Waiting then asserting the app is still alive (no launch crash)"
sleep 6
if xcrun simctl spawn "$SIM" launchctl list 2>/dev/null | grep -q "$APP_ID"; then
  xcrun simctl io "$SIM" screenshot "$SHOT" >/dev/null 2>&1 || true
  echo "PASS: $APP_ID is running on '$SIM'. Screenshot: $SHOT"
  exit 0
else
  echo "FAIL: $APP_ID is not running — it crashed on launch."
  exit 1
fi
