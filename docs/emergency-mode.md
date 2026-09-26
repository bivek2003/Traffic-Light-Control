# Emergency mode work - Utshab

- [x] Track active emergency directions in arrival order.
- [x] Break ties in one update using N, S, E, W order.
- [x] Remove cleared requests and ignore repeated active detections.
- [x] Hold emergency green until detection clears, then time yellow and all-red exit.
- [ ] Add entry clearance and pedestrian coordination.
- [ ] Connect detector events and shared controller outputs.
- [ ] Test timing, pedestrian coordination, faults, and mode handoff.

EmergencyRequests is a first step, not a working emergency mode.
Call update with a complete detector snapshot whenever a detector changes.
The next direction is only a candidate; it must not bypass clearance.
Keep the served direction separately during yellow and all-red transitions.

EmergencyMode tracks the served direction until exit clearance finishes.
Its startAfterClearance method assumes entry clearance and pedestrian crossing are complete.
Call advance with increasing millisecond times and that direction's detector reading.
Apply each stage's light pattern before the next update; late updates do not skip all-red.
Timing comes from ControllerConfig; tests use the proposed 4-second / 5-second values.
Neither class is wired into the live controller yet. Fault handling is still unfinished.

Before integration, coordinate with Bivek on mode hooks, detector messages,
lane-level outputs, and the proposed 4-second yellow / 5-second all-red timing.
Coordinate with Priyash on crossing completion and pending pedestrian requests.
Update the shared diagrams to show clearance and completion of an active crossing.
The current code supports direction-wide colors and solid-red fail-safe only;
lane control, pedestrian signals, and flashing red still need shared support.
