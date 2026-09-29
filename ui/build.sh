#!/bin/bash
# Compiles the traffic display. Works from anywhere.
set -e
cd "$(dirname "$0")/.."

FX="${PATH_TO_FX:-.deps/javafx-17.0.16}"
if [ ! -d "$FX" ]; then
  echo "JavaFX SDK not found at: $FX"
  echo "Run bash ui/setup-javafx.sh, or set PATH_TO_FX to a JavaFX 17 SDK lib folder."
  exit 1
fi

mkdir -p out
javac --release 17 --module-path "$FX" --add-modules javafx.controls \
      -d out $(find src/main/java -name '*.java') ui/*.java
echo "built controller and display -> out"
