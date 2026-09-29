#!/bin/bash
set -e
cd "$(dirname "$0")/.."
FX="${PATH_TO_FX:-.deps/javafx-17.0.16}"
bash ui/build.sh
javac --release 17 --module-path "$FX" --add-modules javafx.controls \
  -cp out -d out $(find src/test/java -name '*.java')
for test in ui.LabObjectsTest ui.SimulationIntegrationTest ui.RightTurnPathTest ui.TrafficSpacingTest \
  trafficcontrol.controller.TrafficControllerLogicTest \
  trafficcontrol.controller.TrafficControllerSocketTest \
  trafficcontrol.controller.EmergencyModeTest trafficcontrol.controller.EmergencyRequestsTest \
  trafficcontrol.controller.EmergencyEntryTest trafficcontrol.controller.EmergencySequenceTest \
  trafficcontrol.controller.EmergencyFaultTest trafficcontrol.controller.EmergencyPlanTest \
  trafficcontrol.device.DeviceStateTest trafficcontrol.device.DeviceSimulatorLogicTest \
  trafficcontrol.device.DeviceSystemTest trafficcontrol.testharness.TestHarnessScriptTest \
  multiplexor.MessageTest multiplexor.MultiplexorTest; do
  java --module-path "$FX" --add-modules javafx.controls -ea -cp out "$test"
done
