# Controller and simulation

References: Team 05 Traffic Light Controller Plan (26 September 2026),
and Team 05 Design Diagram V5.5. Downloaded reference documents are unchanged.

## Access and execution

| Procedure | Method | Device objects |
| --- | --- | --- |
| ModeSelection | select | PedInterface, EmergencyDetector |
| NormalTrafficControl | step | TrafficLights |
| NightTime | step | CarDetector, TrafficLights |
| Pedestrian | step | PedInterface |
| Emergency | step | EmergencyDetector, TrafficLights |

The controller creates the four objects using restricted `LabApiViews`.
Each object keeps only its own allowed API operations. The six methods and
enum names in `devices.LabApi` match section 2 of the plan.
The folders use the repository's `src/main/java` layout, with the plan's
package names beneath it.

The main thread performs selection and inline waits. There are no controller
worker threads, schedulers, or timer objects. JavaFX necessarily has a separate
framework application thread and animation callback. This is the GUI exception
to the document's literal "no other thread" wording; the GUI never runs a mode
or writes a traffic/pedestrian output. Shared API access is synchronized.

Startup is ALL_RED/STOP with two seconds of clearance. Every traffic step
finishes GREEN -> YELLOW -> ALL_RED before another mode is selected. Normal
retains its next phase across interruptions by other modes. Night demand rotates
through minor through, minor left, major left, then default major through, skipping
empty requested phases. This starvation fix changes the plan's strict priority;
see `traffic-flow.md`. All durations
come from `Config`; night uses the host's local system clock.

The pedestrian request is cleared after seven seconds of WALK, then STOP is
displayed during eight seconds of clearance. A request arriving during WALK
is included in that crossing; a request after clearing remains pending.
Selection prevents consecutive pedestrian steps.

GUI lane 1 is L, lane 2 is C, and lane 3 is R. Green left arrows serve L only;
through green serves C/R. Every turning vehicle now waits at red. Vehicles
already beyond the stop line finish crossing; the GUI does not shorten any
controller phase. The display is a teaching simulation, not a calibrated
road-geometry or collision-safety model.

Closing the GUI disables its inputs and waits for the current bounded step
and clearance. Interrupting a delay also completes its duration before the
controller exits between steps. No real hardware is connected by this runner.

## Checks

`ui.LabObjectsTest` checks output guards, startup, road/lane mapping, latching,
selection priority, access signatures, red-light compliance and protected lefts.
`ui.ControllerGuiTest` uses the actual window, GUI buttons, shared API and real
timings for the normal cycle, each night choice, repeated pedestrian requests,
simultaneous emergency/pedestrian requests, both emergency roads, sensor release,
the stuck-sensor cap, return to normal, and safe GUI close. It checks observed
traffic sequences and rejects WALK overlapping traffic. Night steps are selected
explicitly in this scenario test so it also runs during the day.
Legacy socket, device, protocol and emergency regression tests remain available.

## Remaining

- [ ] Confirm the open hardware/policy decisions listed in the team plan.
- [ ] Supply the real `LabApi` adapter and repeat the scenarios on lab hardware.
- [ ] Check automatic night entry/exit at the 22:00 and 06:00 clock boundaries.
- [ ] Agree on simultaneous/stuck emergency fairness; see emergency notes.

Changing `USE_SIMULATOR` to false currently stops with an explicit message:
it cannot invent an adapter for an API implementation the team has not supplied.
The older Multiplexor protocol is retained, not silently treated as that adapter.
