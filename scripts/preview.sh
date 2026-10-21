#!/usr/bin/env bash
# Build Lean TV and run it in a local Android TV emulator (1080p).
# Requires the Android SDK with cmdline-tools and the emulator installed.
set -euo pipefail
cd "$(dirname "$0")/.."

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
AVD_NAME="leantv"
# Apple Silicon / ARM hosts. On an Intel machine use the x86 variant instead.
IMAGE="system-images;android-31;android-tv;arm64-v8a"

if ! "$SDK/emulator/emulator" -list-avds | grep -qx "$AVD_NAME"; then
  echo ">> Creating Android TV AVD from $IMAGE"
  yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" "$IMAGE" >/dev/null
  echo no | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd \
    -n "$AVD_NAME" -k "$IMAGE" -d tv_1080p
  # avdmanager defaults to hw.keyboard=no (the line may not even exist until first
  # boot), which makes the emulator ignore arrow keys / Enter / Esc — the whole
  # point of a desktop preview. Remove any existing line and force yes.
  CFG="$HOME/.android/avd/$AVD_NAME.avd/config.ini"
  grep -v "^hw.keyboard " "$CFG" > "$CFG.tmp" && echo "hw.keyboard = yes" >> "$CFG.tmp" && mv "$CFG.tmp" "$CFG"
fi

./gradlew :app:assembleDebug

if ! "$ADB" devices | grep -q "^emulator-"; then
  echo ">> Booting emulator"
  "$SDK/emulator/emulator" -avd "$AVD_NAME" -netdelay none -netspeed full &
  "$ADB" wait-for-device
fi

until [ "$("$ADB" -e shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 2
done

"$ADB" -e install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" -e shell am start -n dev.esmanhoto.leantv/.MainActivity

echo ">> Lean TV is running in the emulator."
echo ">> To make it the actual HOME app there (the API 31 TV image ignores the"
echo ">> HOME role while its own launcher is enabled, so disable it, like on a real TV):"
echo "   adb -e shell cmd role add-role-holder --user 0 android.app.role.HOME dev.esmanhoto.leantv"
echo "   adb -e shell pm disable-user --user 0 com.google.android.tvlauncher"
echo ">> Undo: adb -e shell pm enable com.google.android.tvlauncher"
echo ">> Note: this Google TV emulator image re-enables its launcher on every boot,"
echo ">> so re-run the disable after a reboot. Real TVs keep the setting."
