#!/usr/bin/env bash
# Deploy Lean TV to a real Android TV over network ADB and set it as the home app.
set -euo pipefail
cd "$(dirname "$0")/.."

TV="${1:?usage: scripts/install-tv.sh <tv-ip:5555>}"

adb connect "$TV"
./gradlew :app:assembleRelease
adb -s "$TV" install -r app/build/outputs/apk/release/app-release.apk
adb -s "$TV" shell cmd package set-home-activity dev.esmanhoto.leantv/.MainActivity

# AOT-compile the launcher and the daily apps to native code: on a weak TV CPU
# this cuts launch time and JIT churn. Costs flash storage; redo after app updates.
AOT_PACKAGES=(
  dev.esmanhoto.leantv
  com.netflix.ninja
  com.google.android.youtube.tv
  com.surfshark.vpnclient.android
  com.disney.disneyplus
  com.wbd.stream
  com.amazon.amazonvideo.livingroom
  fi.yle.areena.tv
  com.globo.globotv
)
echo ">> AOT-compiling launcher + daily apps (a few minutes, one-off)..."
for pkg in "${AOT_PACKAGES[@]}"; do
  if adb -s "$TV" shell pm list packages "$pkg" | grep -q "$pkg"; then
    echo "   $pkg"
    adb -s "$TV" shell cmd package compile -m speed -f "$pkg" || true
  fi
done

echo ">> Done. Press HOME on the remote."
echo ">> Rescue (restore the stock launcher):"
echo "   adb -s $TV shell pm enable com.google.android.tvlauncher"
echo "   adb -s $TV shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity"
