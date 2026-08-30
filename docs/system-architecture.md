# Traffic Flow Controller System Architecture

## Purpose

The complete system is written in Java. It uses Java socket classes for communication and JavaFX for the visual intersection. The traffic controller decides the signal state, simulated devices send and receive updates, and JavaFX displays the result. Each road has six lanes: three lanes travel in one direction and three travel in the opposite direction, separated by a yellow center line.

## Component view

```mermaid
flowchart LR
    TH[Java test harness\nMember 5] <-->|Java sockets| MX[Java Multiplexor\nMember 2]
    MX <-->|Java sockets| TC[Java traffic controller\nMember 1]
    MX <-->|Java sockets| DSH[Java device simulators\nMember 3]
    DSH <--> SIM[JavaFX traffic display\nMember 4]

    subgraph DEVICES[Device simulators — Member 3]
      TL[Traffic-light heads]
      PB[Pedestrian push buttons]
      VD[Vehicle detectors]
      AX[Auxiliary devices]
    end

    DSH <--> TL
    DSH <--> PB
    DSH <--> VD
    DSH <--> AX
```

## Module ownership and boundaries

| Module | Owner | Owns | Does not own |
| --- | --- | --- | --- |
| Traffic Controller | Member 1 | Java control logic and signal timing | Socket routing or JavaFX drawing |
| Multiplexor | Member 2 | Java `ServerSocket` connections and message routing | Traffic-control decisions |
| Device Simulators | Member 3 | Java device state and command handling | Signal timing rules |
| JavaFX Simulator | Member 4 | Intersection display and vehicle animation | Controller decisions |
| Main Test Harness | Member 5 | Java test scenarios and result checks | Production control logic |

## Road layout

- Each road contains six lanes in total.
- Three lanes carry traffic in one direction and three lanes carry traffic in the opposite direction.
- A yellow center line separates the two directions of travel.
- JavaFX vehicles must move only in the direction assigned to their lane.
- The traffic lights control both opposing approaches at the intersection.

## System rules

- All modules use Java; JavaFX is used only for the user interface.
- Modules communicate with Java `Socket` and `ServerSocket` classes.
- Each device has a unique ID such as `light-north` or `button-east`.
- The Multiplexor routes text messages but does not make traffic decisions.
- Only the controller changes traffic-light states.
- If the controller disconnects, traffic lights return to red.
