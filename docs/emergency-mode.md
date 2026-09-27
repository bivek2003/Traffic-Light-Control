# Emergency mode work - Utshab

- [x] Track active emergency directions in arrival order.
- [x] Break ties in one update using N, S, E, W order.
- [x] Remove cleared requests and ignore repeated active detections.
- [x] Hold emergency green until detection clears, then time yellow and all-red exit.
- [x] Time entry clearance and wait for an active pedestrian crossing to finish.
- [x] Interrupt every emergency stage for a fault and time all-red recovery.
- [x] Connect the request queue to mode updates and test consecutive emergencies.
- [ ] Connect detector events and shared controller outputs.
- [x] Test local timing, crossing status, faults, and control-release decisions.
- [ ] Verify live detector, light, pedestrian, and normal/night integration.

For integration, use EmergencyMode.update(now, detected, pedestrianCrossing, anyFaultActive).
The mode owns its EmergencyRequests queue and retains the served direction during exit.
Pass a full EnumSet of active detectors on each event and timer update, even during faults.
Use one controller thread or lock for updates and reading results.
Do not mix this entry point with direct start, advance, or setFault calls.
Those lower-level methods remain available for the individual state tests.
After update, read getStage and getDirection to apply the agreed shared output commands.
Check requiresControl before handing traffic back to Bivek's mode selector.
It stays true during faults, clearance, and the IDLE gap before another waiting request.
When it becomes false, Bivek selects normal/night mode from the current clock.
Consecutive emergencies repeat entry clearance; keep already-red lights red during that entry.

EmergencyMode tracks the served direction until exit clearance finishes.
Call start(direction, now, pedestrianCrossing) once for a selected request.
Call advance(now, detected, pedestrianCrossing) with that direction's detector reading.
Times must increase in milliseconds; each update uses the current crossing status.
Entry follows yellow, all-red, then green only if the detector is still active.
An active crossing holds traffic red until it ends, then starts full all-red clearance.
The shared controller must prevent new Walk signals while emergency service is pending.
ENTRY_YELLOW refers to the previously green traffic, not the emergency direction.
The integrator must retain that previous traffic group when applying entry commands.
IDLE after clearance means the controller may select another mode or waiting request.
Apply each stage's light pattern before the next update; late updates do not skip all-red.
Timing comes from ControllerConfig; tests use the proposed 4-second / 5-second values.
The emergency module is not wired into the live controller yet.
Before start or advance, call setFault(anyFaultActive, now) with combined system fault status.
FAULT requires the shared output code to flash all traffic red and show pedestrian Stays.
Faults discard the served direction; clearing all faults starts full all-red recovery.
After recovery reaches IDLE, refresh detectors and select again; never restore old green.
Repeated clear reports do not restart clearance, but a new fault interrupts recovery.
Flashing commands and live fault reporting remain integration work.

Before integration, coordinate with Bivek on mode hooks, detector messages,
lane-level outputs, and the proposed 4-second yellow / 5-second all-red timing.
Coordinate with Priyash on crossing completion and pending pedestrian requests.
Update the shared diagrams to show clearance and completion of an active crossing.
The current code supports direction-wide colors and solid-red fail-safe only;
lane control, pedestrian signals, and flashing red still need shared support.
