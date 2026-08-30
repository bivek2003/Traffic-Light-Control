# Integration Plan

## Shared contract first

All members use Java and follow [device-interface-specification.md](device-interface-specification.md). JavaFX is used only by the visual simulator. The display uses six lanes per road, with three lanes moving in each direction and a yellow line between the opposing traffic. Before integration, each module must connect to a host and port and send the simple text messages defined in the interface document.

## Integration sequence

| Milestone | Participants | Entry criteria | Evidence of completion |
| --- | --- | --- | --- |
| 1. Java sockets | Members 1, 2 | Multiplexor starts | A Java client connects and registers |
| 2. Device connection | Members 2, 3 | Registration works | A device event reaches the controller |
| 3. Controller loop | Members 1, 2, 3 | Routing works | The controller changes a traffic-light state |
| 4. JavaFX display | Members 3, 4 | State messages work | JavaFX shows six lanes, opposing traffic, the yellow center line, and the correct light color |
| 5. Java tests | Members 2–5 | Full system runs | Test scenarios pass |
| 6. Final check | All members | All modules are connected | Startup instructions and test results are ready |

## End-to-end acceptance scenarios

1. **Vehicle:** detect a vehicle and verify its direction eventually receives a green light.
2. **Pedestrian:** press the JavaFX pedestrian button and verify the request reaches the controller.
3. **Invalid message:** send a bad destination and verify an `ERROR` is returned.
4. **Disconnect:** disconnect the controller and verify traffic lights return to red.
5. **Lane direction:** place vehicles in all six lanes and verify each vehicle moves with its assigned direction of traffic without crossing the yellow center line.
