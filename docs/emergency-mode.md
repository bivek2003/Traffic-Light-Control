# Emergency mode

Reference: Team 05 Traffic Light Controller Plan, 26 September 2026, and
Design Diagram V5.5. The current procedure is `procedures.Emergency`.

## Implemented

- `void step(EmergencyDetector em, TrafficLights lights)` matches both references.
- Receives only the two permitted device objects, not pedestrian or car inputs.
- Maps N/S to NS through and E/W to EW through.
- Holds green while either detector on that road is active.
- Polls every 500 ms, with a 60-second maximum hold.
- Completes 4-second yellow and 2-second all-red before returning.
- Uses shared `Config`, `Road`, `TrafficLights` and inline `Runtime.delay`.
- Runs on the controller's main thread, never the JavaFX thread.
- GUI checkboxes drive real simulator detector inputs.

An emergency waits until the current bounded step ends. It does not interrupt
a normal green, WALK, or pedestrian clearance. Once back at ALL_RED, mode
selection checks emergency before pedestrian, night and normal.

## Decisions still needed

- Simultaneous roads currently use N, S, E, W polling order. The plan has no
  arrival-order policy. Sustained NS requests can delay EW requests.
- A stuck detector remains eligible after the 60-second cap and clearance.
  The cap ends each green but does not guarantee service to other modes.
- Confirm the real API supports `pedClearRequest`, protected left phases,
  and the NS-major-road mapping before connecting hardware.
- Hardware communication errors need a lab-approved failure policy.
  `BLINK_RED` fault handling remains outside the supplied plan's scope.

`trafficcontrol.controller.EmergencyMode` is the earlier timestamp-driven
prototype. Its tests remain, but the GUI controller does not call it.
The separate Git branch named `prototype` is also untouched.
