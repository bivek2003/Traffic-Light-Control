# Integration Plan

## Shared contract first

All members use Java and follow [device-interface-specification.md](device-interface-specification.md). JavaFX is used only by the visual simulator. The display uses six lanes per road, with three lanes moving in each direction and a yellow line between the opposing traffic. Before integration, each module must connect to a host and port and send the simple text messages defined in the interface document.
