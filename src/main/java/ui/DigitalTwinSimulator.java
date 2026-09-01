import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/** JavaFX UI and socket-connected digital twin using the original visual style. */
public final class DigitalTwinSimulator extends Application {
    private static final String PANEL = "-fx-background-color: white;"
            + "-fx-border-color: #d5dce3; -fx-border-radius: 10;"
            + "-fx-background-radius: 10;";
    private static final String PANEL_HEAD = "-fx-background-color: #f3f6f8;"
            + "-fx-border-color: transparent transparent #d5dce3 transparent;";
    private static final String BUTTON = "-fx-background-color: white;"
            + "-fx-border-color: #d5dce3; -fx-border-radius: 7;"
            + "-fx-background-radius: 7; -fx-text-fill: #0e1418;";

    private final Map<String, Device> devices = new LinkedHashMap<String, Device>();
    private final Map<String, TrafficLight> lights = new LinkedHashMap<String, TrafficLight>();
    private final Map<String, VehicleDetector> detectors =
            new LinkedHashMap<String, VehicleDetector>();
    private final Map<String, Labeled> stateLabels = new LinkedHashMap<String, Labeled>();
    private TrafficSimulation traffic;
    private IntersectionView view;
    private TextArea log;
    private Label connection;
    private Socket socket;
    private PrintWriter output;

    @Override
    public void start(Stage stage) {
        createDevices();
        traffic = new TrafficSimulation(lights, detectors);
        view = new IntersectionView(traffic, lights, 650);

        VBox root = new VBox(18, header(), body());
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color: #e9edf1;");

        stage.setTitle("Traffic Light Control");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
        stage.centerOnScreen();

        refreshDeviceStates();
        startAnimation();
        connect();
    }

    private void createDevices() {
        for (String direction : new String[]{"north", "south", "east", "west"}) {
            TrafficLight light = new TrafficLight("light-" + direction);
            lights.put(light.getId(), light);
            devices.put(light.getId(), light);
            Device button = new PedestrianButton("button-" + direction);
            VehicleDetector detector = new VehicleDetector("detector-" + direction);
            detectors.put(detector.getId(), detector);
            devices.put(button.getId(), button);
            devices.put(detector.getId(), detector);
        }
    }

    private VBox header() {
        Label eyebrow = new Label("CS 460 · TRAFFIC DISPLAY");
        eyebrow.setFont(Font.font("Menlo", FontWeight.BOLD, 10.5));
        eyebrow.setTextFill(Color.web("#0d7488"));

        Label title = new Label("Traffic Light Control");
        title.setFont(Font.font("Helvetica Neue", FontWeight.EXTRA_BOLD, 26));
        title.setTextFill(Color.web("#0e1418"));

        Label subtitle = new Label("Three lanes each way, embedded vehicle detection, "
                + "automatic demand-based signals, and traffic that obeys every phase.");
        subtitle.setFont(Font.font("Helvetica Neue", 13));
        subtitle.setTextFill(Color.web("#59656f"));

        connection = new Label("Connecting to Multiplexor...");
        connection.setFont(Font.font("Menlo", FontWeight.BOLD, 10));
        connection.setTextFill(Color.web("#59656f"));
        return new VBox(3, eyebrow, title, subtitle, connection);
    }

    private HBox body() {
        HBox body = new HBox(20, intersectionPanel(), rail());
        body.setAlignment(Pos.TOP_LEFT);
        return body;
    }

    private VBox intersectionPanel() {
        Label state = new Label("LIVE TRAFFIC");
        state.setFont(Font.font("Menlo", FontWeight.BOLD, 10));
        state.setTextFill(Color.web("#59656f"));
        HBox heading = panelHeading("INTERSECTION VIEW", state);
        VBox canvas = new VBox(view);
        canvas.setPadding(new Insets(13));
        VBox panel = new VBox(heading, canvas);
        panel.setStyle(PANEL);
        return panel;
    }

    private VBox rail() {
        VBox rail = new VBox(12, pedestrianPanel(), logPanel());
        rail.setPrefWidth(360);
        return rail;
    }

    private VBox pedestrianPanel() {
        HBox row = new HBox(7);
        row.setPadding(new Insets(10));
        for (String direction : new String[]{"north", "south", "east", "west"}) {
            String id = "button-" + direction;
            Button press = styledButton(direction.substring(0, 1).toUpperCase());
            press.setMaxWidth(Double.MAX_VALUE);
            press.setOnAction(event -> {
                String action = "PRESSED".equals(devices.get(id).getState()) ? "RESET" : "PRESS";
                applyLocally(id, action, "");
            });
            stateLabels.put(id, press);
            HBox.setHgrow(press, Priority.ALWAYS);
            row.getChildren().add(press);
        }
        return panel("PEDESTRIAN BUTTONS", row);
    }

    private VBox logPanel() {
        log = new TextArea();
        log.setEditable(false);
        log.setPrefRowCount(7);
        log.setFont(Font.font("Menlo", 10));
        log.setStyle("-fx-control-inner-background: white; -fx-text-fill: #59656f;");
        return panel("MESSAGE LOG", log);
    }

    private VBox panel(String title, Node content) {
        VBox panel = new VBox(panelHeading(title, null), content);
        panel.setStyle(PANEL);
        return panel;
    }

    private HBox panelHeading(String title, Node trailing) {
        Label label = new Label(title);
        label.setFont(Font.font("Menlo", FontWeight.BOLD, 10));
        label.setTextFill(Color.web("#59656f"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox heading = new HBox(8, label, spacer);
        if (trailing != null) {
            heading.getChildren().add(trailing);
        }
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.setPadding(new Insets(9, 12, 9, 12));
        heading.setStyle(PANEL_HEAD);
        return heading;
    }

    private Button styledButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("Helvetica Neue", FontWeight.BOLD, 11.5));
        button.setStyle(BUTTON);
        return button;
    }

    private void applyLocally(String id, String action, String value) {
        try {
            devices.get(id).apply(action, value);
            append(id + " = " + devices.get(id).getState());
            refreshDeviceStates();
        } catch (IllegalArgumentException error) {
            append("ERROR: " + error.getMessage());
        }
    }

    private void refreshDeviceStates() {
        for (Map.Entry<String, Labeled> entry : stateLabels.entrySet()) {
            String state = devices.get(entry.getKey()).getState();
            if (entry.getKey().startsWith("button-")) {
                entry.getValue().setText("PRESSED".equals(state) ? "WAIT" :
                        entry.getKey().substring("button-".length(), "button-".length() + 1)
                                .toUpperCase());
            } else {
                entry.getValue().setText(state);
            }
        }
        view.draw();
    }

    private void startAnimation() {
        new AnimationTimer() {
            private long previous;

            @Override
            public void handle(long now) {
                if (previous != 0) {
                    traffic.update(Math.min(0.05, (now - previous) / 1_000_000_000.0));
                    view.draw();
                }
                previous = now;
            }
        }.start();
    }

    private void connect() {
        String host = getParameters().getRaw().isEmpty()
                ? "localhost" : getParameters().getRaw().get(0);
        int port = getParameters().getRaw().size() < 2
                ? Multiplexor.DEFAULT_PORT : Integer.parseInt(getParameters().getRaw().get(1));
        Thread reader = new Thread(() -> readMessages(host, port), "digital-twin-socket");
        reader.setDaemon(true);
        reader.start();
    }

    private void readMessages(String host, int port) {
        try {
            socket = new Socket(host, port);
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            output = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8), true);
            output.println("REGISTER|digital-twin|multiplexor|CONNECT|");
            String acknowledgement = input.readLine();
            Platform.runLater(() -> connection.setText("CONNECTED · " + host + ":" + port
                    + " · " + acknowledgement));
            String line;
            while ((line = input.readLine()) != null) {
                final String command = line;
                Platform.runLater(() -> handleCommand(command));
            }
        } catch (IOException error) {
            Platform.runLater(() -> connection.setText("CONNECTION FAILED · " + error.getMessage()));
        }
    }

    private void handleCommand(String line) {
        try {
            Message message = Message.parse(line);
            if (!"COMMAND".equals(message.getType())) {
                throw new IllegalArgumentException("digital twin accepts COMMAND messages only");
            }
            Device device = devices.get(message.getDestination());
            if (device == null) {
                throw new IllegalArgumentException("unknown device " + message.getDestination());
            }
            device.apply(message.getAction(), message.getValue());
            if (device instanceof VehicleDetector && "DETECT".equals(message.getAction())) {
                traffic.addVehicle(directionFrom(device.getId()));
            }
            String stateAction = device instanceof TrafficLight ? "COLOR"
                    : device instanceof PedestrianButton ? "REQUEST" : "PRESENCE";
            send(new Message("STATE", device.getId(), message.getSource(), stateAction,
                    device.getState()).toLine());
            append(line + " → " + device.getState());
            refreshDeviceStates();
        } catch (IllegalArgumentException error) {
            send("ERROR|digital-twin|test-harness|BAD_COMMAND|"
                    + error.getMessage().replace('|', '/'));
        }
    }

    private char directionFrom(String id) {
        return Character.toUpperCase(id.substring(id.lastIndexOf('-') + 1).charAt(0));
    }

    private void send(String line) {
        if (output != null) output.println(line);
    }

    private void append(String line) {
        log.appendText(line + System.lineSeparator());
    }

    @Override
    public void stop() throws IOException {
        if (socket != null) socket.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
