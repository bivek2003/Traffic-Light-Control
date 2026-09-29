# Traffic flow fixes

Run the controller-driven GUI with `bash ui/run.sh`.
Use Java 17+ and `bash ui/setup-javafx.sh`, or set `PATH_TO_FX` to a JavaFX 17 SDK.
The entry point is now `controller.Controller`, not the older socket-based app.

## Vehicle spacing

Cars keep checking their leader before, during and after a turn. Front cars
move first, and followers keep an 18-unit gap along their path. Both turns use
constant-speed quarter circles; the old left curve compressed cars together.
The displayed speed follows actual movement, including when a car is blocked.
Starting cars are placed entirely behind the stop line.

`TrafficSpacingTest` checks slower leaders with faster followers in all 12
direction/lane queues, plus 72 cars at a busy intersection. It checks body
separation through the normal green/yellow/all-red sequence. Right-turn path
checks remain in `RightTurnPathTest`.

## Left turns at night

The clock selects night mode from 22:00 to 06:00. In the original plan, constant
minor-road through demand could win every selection and indefinitely block lefts.
To fix that, the night priority list now rotates after each completed phase:
minor through, minor left, major left, major through. Empty requested phases
are skipped; major through remains the default. A waiting movement gets a turn
within four night steps, unless another mode interrupts night service.

This is an intentional change from the plan's strict night priority. The method
signatures, emergency/pedestrian priority and clearance timings are unchanged.
Lane L moves only on LEFT GREEN, not on the same road's through green.

Every signal head spans its approach's lanes with exactly three indicators:
a left arrow over L and two circles over C/R. Each indicator changes color.
The circles share the through signal, as required by the lab's whole-road API.
North/south heads are horizontal on screen;
east/west heads are rotated 90 degrees. During a left phase the through lamps stay red;
during a through phase the arrow stays red. Yellow/all-red clearance and the
existing phase order are unchanged.

Run `bash ui/test.sh` for the non-GUI checks. This is still a teaching simulator,
not a real-road collision model or a tested hardware controller.
