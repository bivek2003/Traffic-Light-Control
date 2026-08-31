package trafficcontrol.controller;

public final class ControllerConfig {
    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5050;

    private final long minimumGreenMillis;
    private final long maximumGreenMillis;
    private final long yellowMillis;
    private final long allRedMillis;

    public ControllerConfig(long minimumGreenMillis, long maximumGreenMillis,
            long yellowMillis, long allRedMillis) {
        if (minimumGreenMillis <= 0 || maximumGreenMillis < minimumGreenMillis
                || yellowMillis <= 0 || allRedMillis <= 0) {
            throw new IllegalArgumentException("invalid signal timing configuration");
        }
        this.minimumGreenMillis = minimumGreenMillis;
        this.maximumGreenMillis = maximumGreenMillis;
        this.yellowMillis = yellowMillis;
        this.allRedMillis = allRedMillis;
    }

    public static ControllerConfig defaults() {
        return new ControllerConfig(5_000, 15_000, 2_000, 1_000);
    }

    public long getMinimumGreenMillis() {
        return minimumGreenMillis;
    }

    public long getMaximumGreenMillis() {
        return maximumGreenMillis;
    }

    public long getYellowMillis() {
        return yellowMillis;
    }

    public long getAllRedMillis() {
        return allRedMillis;
    }
}
