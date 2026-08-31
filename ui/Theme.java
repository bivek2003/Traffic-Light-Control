package ui;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Colours and fonts for the window around the canvas, and the small helpers
 * that turn them into the objects JavaFX wants.
 *
 * <p>Everything here is set through the JavaFX API rather than a stylesheet,
 * so the display needs nothing but JavaFX itself.
 */
public final class Theme {

    public final Color ground;
    public final Color surface;
    public final Color surfaceAlt;
    public final Color line;
    public final Color ink;
    public final Color inkSoft;
    public final Color inkFaint;
    public final Color accent;

    // Signal colours do not change with the theme.
    public static final Color RED = Color.web("#e5484d");
    public static final Color AMBER = Color.web("#f0a92e");
    public static final Color GREEN = Color.web("#46b26a");

    public static final Font TITLE = Font.font("Helvetica Neue", FontWeight.EXTRA_BOLD, 26);
    public static final Font BODY = Font.font("Helvetica Neue", 13);
    public static final Font SMALL = Font.font("Helvetica Neue", 11.5);
    public static final Font BUTTON = Font.font("Helvetica Neue", FontWeight.BOLD, 12.5);
    public static final Font MONO = Font.font("Menlo", 11);
    public static final Font MONO_BOLD = Font.font("Menlo", FontWeight.BOLD, 10.5);
    public static final Font MONO_ID = Font.font("Menlo", FontWeight.BOLD, 11.5);

    private Theme(Color ground, Color surface, Color surfaceAlt, Color line,
                  Color ink, Color inkSoft, Color inkFaint, Color accent) {
        this.ground = ground;
        this.surface = surface;
        this.surfaceAlt = surfaceAlt;
        this.line = line;
        this.ink = ink;
        this.inkSoft = inkSoft;
        this.inkFaint = inkFaint;
        this.accent = accent;
    }

    public static Theme light() {
        return new Theme(
                Color.web("#e9edf1"), Color.web("#ffffff"), Color.web("#f3f6f8"),
                Color.web("#d5dce3"), Color.web("#0e1418"), Color.web("#59656f"),
                Color.web("#8b969f"), Color.web("#0d7488"));
    }

    public static Theme dark() {
        return new Theme(
                Color.web("#0d1115"), Color.web("#151a20"), Color.web("#1b212a"),
                Color.web("#2a323c"), Color.web("#e5ebf1"), Color.web("#94a1ad"),
                Color.web("#6b7883"), Color.web("#5ec6d8"));
    }

    // ---- helpers --------------------------------------------------------

    public static Background fill(Color colour) {
        return new Background(new BackgroundFill(colour, CornerRadii.EMPTY, Insets.EMPTY));
    }

    public static Background fill(Color colour, double radius) {
        return new Background(new BackgroundFill(colour, new CornerRadii(radius), Insets.EMPTY));
    }

    public static Border outline(Color colour, double radius) {
        return new Border(new BorderStroke(colour, BorderStrokeStyle.SOLID,
                new CornerRadii(radius), new BorderWidths(1)));
    }

    /** A single rule beneath a panel heading. */
    public static Border underline(Color colour) {
        return new Border(new BorderStroke(colour, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(0, 0, 1, 0)));
    }

    /** A single rule above the control bar. */
    public static Border overline(Color colour) {
        return new Border(new BorderStroke(colour, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, new BorderWidths(1, 0, 0, 0)));
    }
}
