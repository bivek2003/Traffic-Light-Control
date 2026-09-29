#!/bin/bash
# Builds if needed, then starts the traffic display.
set -e
cd "$(dirname "$0")/.."

FX="${PATH_TO_FX:-.deps/javafx-17.0.16}"
./ui/build.sh

java --module-path "$FX" --add-modules javafx.controls \
     --enable-native-access=javafx.graphics \
     -cp out ui.TrafficApp "$@"
