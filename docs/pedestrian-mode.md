# Pedestrian milestone and controller integration

Reviewed against main at `91a792f`, Team05 Design Diagram V5.4, and
Utshab's `docs/emergency-mode.md`, and TO5 SAD v1.0 (29 September 2026).
This is an independent prototype,
not the final lab procedure or a change to the team's shared design.

## Current prototype API

| Method | Contract |
| --- | --- |
| `PedestrianMode(long crossingMillis)` | Caller supplies a positive duration; no assumed team default. |
| `PedestrianMode(long crossingMillis, LongSupplier clock)` | Inject a monotonic millisecond clock for deterministic tests. |
| `start()` | SAD lifecycle entry; starts queued requests using the configured clock. |
| `update()` | SAD lifecycle update; checks crossing completion using the configured clock. |
| `stop()` | SAD lifecycle exit; rejects unfinished crossings and preserves queued requests. |
| `request(Direction)` | Queue a request; requests during a crossing wait for the next cycle. |
| `hasPendingRequest()` | Report queued demand, separately from an active crossing. |
| `start(long now)` | Start queued requests only after vehicle clearance and Emergency releases control. |
| `advance(long now)` | Finish the crossing at the supplied duration. |
| `isCrossingActive()` | Supply Emergency's current prototype `pedestrianCrossing` input. |
| `getCrossingDirections()` | Return a copy of the directions being served. |

Use one calling thread and increasing millisecond timestamps. The caller owns
signals and mode selection: hold vehicle lights red during crossing, apply
pedestrian STOP on completion, then update/select the next mode. Pending
requests must not interrupt `EmergencyMode.requiresControl()`. These rules
are caller obligations; the standalone component cannot inspect device state.
The default clock uses `System.nanoTime()` converted to milliseconds. Choose
either the no-argument lifecycle or timestamp methods for an instance; do not
mix clock domains. Timestamp methods remain available for prototype compatibility.
Early-stop rejection is an implementation decision: the SAD names `stop()` but
does not specify its behavior. Mode selection must let an active crossing finish.

## Production integration requirements

- V5.4 gives Pedestrian access to `pedInterface` and `trafficLights`;
  Emergency accesses `emergencyDetector` and `trafficLights`.
- The planned bounded-step implementation completes Pedestrian before mode
  selection runs again. The current timestamp prototype is not that procedure.
- SAD v1.0 uses `start()/update()/stop()` for both modes, while Utshab's code
  and earlier plan notes expose different signatures. Pedestrian now provides
  the SAD lifecycle; Emergency's existing API remains unchanged.
- Main has button `CLEAR_REQUEST`, but no shared WALK/STOP device command,
  emergency-detector event contract, or planned lab wrapper classes.
- Confirm the shared Pedestrian signature, timing, request clearing, and
  device methods when available. Do not invent replacements in this branch.
- Keep `TrafficController.main` as the application entry point and integrate
  through its controller logic once those contracts exist. Its current
  scheduler also needs the team-owned single-thread migration described by Utshab.
- Fetch and review Utshab's changes before each milestone; review the diff
  and run affected tests before committing. Push/merge/PR require user approval.

## Verification

`PedestrianModeTest` checks invalid calls, crossing timing, duplicate and queued
requests, and state isolation. `PedestrianEmergencyTest` checks Emergency waiting,
all-red entry, and safe release after Emergency exit. Existing controller and
Emergency tests remain applicable; no production wiring is enabled yet.
`PedestrianLifecycleTest` checks the SAD method names, early-stop rejection,
completion boundaries, and compatibility with Utshab's Emergency API.
