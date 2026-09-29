#!/bin/bash
# Downloads JavaFX 17, including the native libraries for this machine.
set -e
cd "$(dirname "$0")/.."
case "$(uname -s)-$(uname -m)" in
  Darwin-arm64) platform=mac-aarch64 ;;
  Darwin-x86_64) platform=mac ;;
  Linux-x86_64) platform=linux ;;
  *) echo "Use a matching JavaFX 17 SDK and set PATH_TO_FX to its lib folder."; exit 1 ;;
esac
mkdir -p .deps/javafx-17.0.16
for module in base graphics controls; do
  curl --fail --location --show-error \
    "https://repo.maven.apache.org/maven2/org/openjfx/javafx-$module/17.0.16/javafx-$module-17.0.16-$platform.jar" \
    --output ".deps/javafx-17.0.16/javafx-$module.jar"
done
