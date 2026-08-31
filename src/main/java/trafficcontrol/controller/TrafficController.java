package trafficcontrol.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.MessageType;
import trafficcontrol.protocol.ProtocolException;

/** Socket client and executable entry point for Member 1's traffic controller. */
public final class TrafficController implements AutoCloseable {
    public static final String COMPONENT_ID = "controller";

    private final String host;
    private final int port;
    private final TrafficControllerLogic logic;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private volatile boolean closed;

    public TrafficController(String host, int port, ControllerConfig config) {
        this.host = host;
        this.port = port;
        this.logic = new TrafficControllerLogic(config, System.currentTimeMillis());
    }

    public static void main(String[] args) {
        String host = args.length >= 1 ? args[0] : ControllerConfig.DEFAULT_HOST;
        int port;
        try {
            port = args.length >= 2 ? Integer.parseInt(args[1]) : ControllerConfig.DEFAULT_PORT;
        } catch (NumberFormatException error) {
            System.err.println("Port must be a number: " + args[1]);
            return;
        }

        try (TrafficController controller =
                new TrafficController(host, port, ControllerConfig.defaults())) {
            Runtime.getRuntime().addShutdownHook(new Thread(controller::close));
            controller.run();
        } catch (IOException | ProtocolException error) {
            System.err.println("[controller] " + error.getMessage());
        }
    }

    public void run() throws IOException, ProtocolException {
        connectAndRegister();
        sendAll(logic.initialCommands());
        scheduler.scheduleAtFixedRate(this::advanceSafely, 100, 100, TimeUnit.MILLISECONDS);

        String line;
        while (!closed && (line = input.readLine()) != null) {
            handleLine(line);
        }
        if (!closed) {
            throw new IOException("multiplexer connection closed; devices must enter fail-safe red");
        }
    }

    private void connectAndRegister() throws IOException, ProtocolException {
        socket = new Socket(host, port);
        input = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
        output = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream(), StandardCharsets.UTF_8), true);

        send(registrationMessage());
        String replyLine = input.readLine();
        if (replyLine == null || !isRegistrationAcknowledgement(Message.parse(replyLine))) {
            throw new ProtocolException("multiplexer did not acknowledge controller registration");
        }
        System.out.println("[controller] registered with " + host + ":" + port);
    }

    private void handleLine(String line) {
        final Message message;
        try {
            message = Message.parse(line);
        } catch (ProtocolException error) {
            send(Message.error(COMPONENT_ID, "mux", "BAD_MESSAGE", line));
            return;
        }

        if (message.getType() == MessageType.ERROR) {
            System.err.println("[controller] " + message.toLine());
            return;
        }

        try {
            sendAll(logic.accept(message, System.currentTimeMillis()));
        } catch (IllegalArgumentException error) {
            send(Message.error(COMPONENT_ID, message.getSource(),
                    "UNSUPPORTED_MESSAGE", error.getMessage()));
        }
    }

    private void advanceSafely() {
        try {
            sendAll(logic.advance(System.currentTimeMillis()));
        } catch (RuntimeException error) {
            System.err.println("[controller] timer failure: " + error.getMessage());
            sendAll(logic.forceAllRed(System.currentTimeMillis()));
        }
    }

    private synchronized void send(Message message) {
        if (output != null && !closed) {
            output.println(message.toLine());
            if (output.checkError()) {
                System.err.println("[controller] failed to write to multiplexer");
            }
        }
    }

    private void sendAll(List<Message> messages) {
        for (Message message : messages) {
            send(message);
        }
    }

    public static Message registrationMessage() {
        return new Message(MessageType.REGISTER, COMPONENT_ID, "mux", "CONNECT", "CONTROLLER");
    }

    public static boolean isRegistrationAcknowledgement(Message message) {
        return message.getType() == MessageType.STATE
                && "mux".equals(message.getSource())
                && COMPONENT_ID.equals(message.getDestination())
                && "REGISTERED".equals(message.getAction())
                && "OK".equals(message.getValue());
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        if (output != null) {
            for (Message message : logic.forceAllRed(System.currentTimeMillis())) {
                output.println(message.toLine());
            }
            output.flush();
        }
        closed = true;
        scheduler.shutdownNow();
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // The controller is already shutting down.
            }
        }
    }
}
