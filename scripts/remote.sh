#!/usr/bin/env bash
# Turn this terminal into a TV remote (for the emulator or a real TV).
#   scripts/remote.sh              -> controls the emulator
#   scripts/remote.sh <tv-ip:5555> -> controls a real TV over network ADB
set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
if [ $# -ge 1 ]; then TARGET=(-s "$1"); else TARGET=(-e); fi

echo "Arrows = D-pad   Enter = OK   b/Esc = Back   h = Home   m = Menu   q = quit"
while true; do
  IFS= read -rsn1 key
  case "$key" in
    $'\x1b')
      rest=""
      read -rsn2 -t 0.01 rest || true
      case "$rest" in
        "[A") code=DPAD_UP ;;
        "[B") code=DPAD_DOWN ;;
        "[C") code=DPAD_RIGHT ;;
        "[D") code=DPAD_LEFT ;;
        *) code=BACK ;;
      esac ;;
    "") code=DPAD_CENTER ;;
    b) code=BACK ;;
    h) code=HOME ;;
    m) code=MENU ;;
    q) break ;;
    *) continue ;;
  esac
  "$ADB" "${TARGET[@]}" shell input keyevent "KEYCODE_$code" >/dev/null 2>&1 &
done
