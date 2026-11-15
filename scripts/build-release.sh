#!/bin/sh
# Builds the signed release APK and copies it to apk/ with the contest file name.
set -e
cd "$(dirname "$0")/.."
: "${JAVA_HOME:=/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export JAVA_HOME
if [ ! -f keystore.properties ]; then
  echo "keystore.properties missing (release signing)." >&2
  exit 1
fi
./gradlew assembleRelease
mkdir -p apk
cp app/build/outputs/apk/release/app-release.apk apk/CabinCrew_AirRide_release.apk
ls -lh apk/CabinCrew_AirRide_release.apk
