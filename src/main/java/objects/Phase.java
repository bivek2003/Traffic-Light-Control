package objects;

import devices.TrafficPattern;

public enum Phase {
    NS_LEFT(Road.NS, TrafficPattern.NS_LEFT_GREEN, TrafficPattern.NS_LEFT_YELLOW),
    NS_THROUGH(Road.NS, TrafficPattern.NS_GREEN, TrafficPattern.NS_YELLOW),
    EW_LEFT(Road.EW, TrafficPattern.EW_LEFT_GREEN, TrafficPattern.EW_LEFT_YELLOW),
    EW_THROUGH(Road.EW, TrafficPattern.EW_GREEN, TrafficPattern.EW_YELLOW);

    public final Road road;
    public final TrafficPattern green;
    public final TrafficPattern yellow;

    Phase(Road road, TrafficPattern green, TrafficPattern yellow) {
        this.road = road;
        this.green = green;
        this.yellow = yellow;
    }
}
