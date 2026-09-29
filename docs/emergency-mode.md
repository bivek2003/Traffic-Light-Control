# Emergency mode work - Utshab

Reference: Team 05 Traffic Light Controller Plan, dated 26 September 2026.
This plan replaces the earlier single-direction green and unlimited hold proposal.
The code on this branch is still a local state-machine prototype, not the final lab procedure.

## Implemented and tested

- [x] Track emergency requests and remove cleared detectors.
- [x] Map N/S to the NS through road and E/W to the EW through road.
- [x] Hold the road while either of its emergency detectors is active.
- [x] Limit each green service to 60 seconds, measured from entry to green.
- [x] Use 4-second yellow and 2-second all-red in the no-argument constructor.
- [x] Test road mapping, both-road requests, stuck detection, and clearance boundaries.
- [x] Retain legacy pedestrian waiting and fault tests for the existing prototype.
- [ ] Port to Emergency.step(EmergencyDetector em, TrafficLights lights).
- [ ] Use the shared Config, Road/Phase objects, and inline delay utility.
- [ ] Verify lab/simulator output patterns, access rules, and full-system scenarios.

## Temporary prototype API

Use EmergencyMode.update(now, detected, pedestrianCrossing, anyFaultActive).
Pass a full EnumSet of current emergency approaches on each update.
getRoad returns NORTH_SOUTH or EAST_WEST for the required through pattern.
getDirection retains the originally selected approach; it is not a single-lane output.
requiresControl includes waiting requests and unfinished clearance.
Use one controller thread for updates and reading results.
Do not mix update with the lower-level start/advance/setFault test methods.
Use increasing millisecond times and apply each stage before the next update.
The class creates no threads or timers; its caller currently supplies time.
The configurable constructor retains supplied timings for existing tests.
The no-argument constructor uses the new plan's yellow/all-red/hold defaults.
Do not use the existing shared ControllerConfig.defaults() as the new plan's configuration.

## Differences still requiring migration

The shared EmergencyDetector, TrafficLights, LabApi, Config, and Runtime from the plan
are not present on main. Do not add replacement teammate modules just for this branch.
When their public methods are available, port the logic to the required step signature.
The step must start with roads ALL_RED, run on the calling controller thread,
poll at 500 ms, and return only after yellow and the 2-second ALL_RED delay.
The existing update method returns during intermediate stages, so it does not meet S3.
Its external timestamp updates also do not implement the plan's inline waits.
The existing TrafficController also uses a scheduler thread; Bivek owns replacing that runner.
Remove pedestrian/fault inputs from the final emergency procedure's access surface.
Pedestrian completion belongs to its own bounded step before mode selection runs again.
Legacy FAULT support is not part of this plan; do not wire BLINK_RED into this delivery.
Whole-road patterns replace the earlier dependency on individual-lane light commands.

## Add these decisions to the team plan

1. Specify how to choose a road when NS and EW both report an emergency.
   The prototype keeps first arrival and N/S/E/W ties, but this is not stated in the plan.
2. Specify whether either detector on the selected road holds green. This prototype does.
3. Specify what happens after the 60-second limit if the detector is still true.
   The prototype ends green safely but the request stays eligible; it can be selected again.
   A hold cap alone does not guarantee that normal traffic gets a turn with a stuck sensor.
4. Provide the exact EmergencyDetector/TrafficLights methods and Runtime delay contract.
5. Define what to do if a delay is interrupted or a lab operation fails mid-step.
6. Confirm that emergency selection waits for the current bounded step to finish;
   this affects response time during 30-second normal greens and pedestrian crossing.
7. Keep named ownership explicit: Utshab owns Emergency; Roman owns NightTime.

The downloaded plan has not been edited. These notes are proposed additions for the team.
