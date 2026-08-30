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
