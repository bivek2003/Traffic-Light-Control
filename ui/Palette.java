package ui;

import javafx.scene.paint.Color;

/**
 * Colours for everything drawn on the canvas.
 *
 * <p>Road materials do not change with the theme. Asphalt is asphalt whether
 * the surrounding window is light or dark, so only the window chrome is
 * themed, and that is handled in style.css instead.
 */
public final class Palette {

    // Road surface.
    public static final Color SURROUND = Color.web("#181c20");
    public static final Color KERB     = Color.web("#3a4149");
    public static final Color ASPHALT  = Color.web("#24282d");
    public static final Color JUNCTION = Color.web("#2c3138");

    // Markings.
    public static final Color PAINT  = Color.web("#e9ecea");
    public static final Color YELLOW = Color.web("#efc14a");

    // Signal states. These are semantic and must stay unmistakable, which is
    // why the interface accent elsewhere is a cyan that cannot be confused
    // with any of them.
    public static final Color RED   = Color.web("#e5484d");
    public static final Color AMBER = Color.web("#f0a92e");
    public static final Color GREEN = Color.web("#46b26a");

    public static final Color LAMP_OFF   = Color.web("#2b3137");
    public static final Color HEAD_BODY  = Color.web("#14181c");

    // Vehicle bodies, kept desaturated so the signals stay the brightest
    // thing on screen.
    public static final Color[] CAR = {
        Color.web("#cdd6dd"), Color.web("#93a5b2"), Color.web("#7c8b97"),
        Color.web("#b5c1ca"), Color.web("#a3b4c0")
    };
    public static final Color CAR_ACCENT = Color.web("#5ec6d8");

    // People. Warm against the cool greys of the traffic, so a person on the
    // crossing is never mistaken for a small car.
    public static final Color PED_BODY = Color.web("#2f3944");
    public static final Color PED_HEAD = Color.web("#f0e3c8");
    public static final Color PED_LIMB = Color.web("#c9bba2");

    public static Color forState(String state) {
        if ("GREEN".equals(state)) return GREEN;
        if ("AMBER".equals(state)) return AMBER;
        return RED;
    }

    private Palette() {
    }
}
