#!/bin/sh
# Prepares a running Pleos Connect emulator for the AirRide demo.
#  1) GPS → Seoul (the emulator defaults to Mountain View, CA; the Korean navigation needs a Korean location)
#  2) Pleos Connect Virtual Console port (http://127.0.0.1:8088/)
#  3) Install the release APK and grant runtime permissions (vehicle speed, notifications)
set -e
cd "$(dirname "$0")/.."
ADB="${ADB:-adb}"

# Gangnam, Seoul (longitude latitude)
LON="${LON:-127.0276}"
LAT="${LAT:-37.4979}"

$ADB wait-for-device
echo "GPS → $LAT, $LON"
for _ in 1 2 3; do $ADB emu geo fix "$LON" "$LAT" >/dev/null; sleep 1; done

echo "Virtual Console → http://127.0.0.1:8088/"
$ADB forward tcp:8088 tcp:8088 >/dev/null

if [ -f apk/CabinCrew_AirRide_release.apk ]; then
  echo "Installing AirRide"
  $ADB install -r apk/CabinCrew_AirRide_release.apk >/dev/null
  $ADB shell pm grant com.cabincrew.airride android.car.permission.CAR_SPEED || true
  $ADB shell pm grant com.cabincrew.airride android.permission.POST_NOTIFICATIONS || true
fi

echo "Done. Open http://127.0.0.1:8088/ in a browser and keep the tab open while using the console."
