#!/bin/bash
# Builds if needed, then starts the traffic display.
set -e
cd "$(dirname "$0")/.."

FX="${PATH_TO_FX:-$HOME/javafx/javafx-sdk-26.0.2/lib}"
if [ ! -f out/ui/TrafficApp.class ] || [ ! -f out/ui/style.css ]; then
  ./ui/build.sh
fi

java --module-path "$FX" --add-modules javafx.controls \
     --enable-native-access=javafx.graphics \
     -cp out ui.TrafficApp
