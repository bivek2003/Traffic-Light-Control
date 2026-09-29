package config;

import java.time.LocalTime;
import objects.Road;

public final class Config {
    private Config() { }

    // All duration values are milliseconds.
    public static final long THROUGH_GREEN = 30_000;
    public static final long LEFT_GREEN = 10_000;
    public static final long YELLOW = 4_000;
    public static final long ALL_RED = 2_000;
    public static final long WALK = 7_000;
    public static final long PED_CLEARANCE = 8_000;
    public static final LocalTime NIGHT_START = LocalTime.of(22, 0);
    public static final LocalTime NIGHT_END = LocalTime.of(6, 0);
    public static final Road MAJOR_ROAD = Road.NS;
    public static final Road MINOR_ROAD = Road.EW;
    public static final long NIGHT_MAJOR_GREEN = 20_000;
    public static final long NIGHT_MINOR_GREEN = 15_000;
    public static final long NIGHT_LEFT_GREEN = 8_000;
    public static final long EMERGENCY_POLL = 500;
    public static final long EMERGENCY_MAX_HOLD = 60_000;
    public static final boolean USE_SIMULATOR = true;
}
