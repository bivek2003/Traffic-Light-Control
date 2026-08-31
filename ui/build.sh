#!/bin/bash
# Compiles the traffic display. Works from anywhere.
set -e
cd "$(dirname "$0")/.."

FX="${PATH_TO_FX:-$HOME/javafx/javafx-sdk-26.0.2/lib}"
if [ ! -d "$FX" ]; then
  echo "JavaFX SDK not found at: $FX"
  echo
  echo "Download the SDK for your machine from https://gluonhq.com/products/javafx/"
  echo "then either unpack it to ~/javafx/javafx-sdk-26.0.2, or point PATH_TO_FX"
  echo "at its lib folder:"
  echo "    export PATH_TO_FX=/path/to/javafx-sdk-26.0.2/lib"
  exit 1
fi

javac --module-path "$FX" --add-modules javafx.controls -d out ui/*.java
echo "built -> out/ui"
