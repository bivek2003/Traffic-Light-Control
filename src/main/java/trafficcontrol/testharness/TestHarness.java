package trafficcontrol.testharness;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import trafficcontrol.controller.ControllerConfig;
import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.MessageType;
import trafficcontrol.protocol.ProtocolException;

/** Executable test harness for scripted end-to-end socket tests. */
public final class TestHarness {
    public static final String COMPONENT_ID = "test-harness";
    private static final int DEFAULT_TIMEOUT_MILLIS = 5_000;

    private TestHarness() {
    }

    public static void main(String[] args) {
        try {
            Options options = Options.parse(args);
            List<TestScript> scripts = loadScripts(options.scriptPaths);
            boolean passed = runScripts(options, scripts);
            System.exit(passed ? 0 : 1);
        } catch (IllegalArgumentException | IOException | ProtocolException error) {
            System.err.println("[test-harness] " + error.getMessage());
            printUsage();
            System.exit(2);
        }
    }

    private static List<TestScript> loadScripts(List<Path> paths)
            throws IOException, ProtocolException {
        List<TestScript> scripts = new ArrayList<>();
        if (paths.isEmpty()) {
            scripts.add(TestScript.defaultSmokeTest());
            return scripts;
        }
        for (Path path : paths) {
            scripts.add(TestScript.load(path));
        }
        return scripts;
    }

    private static boolean runScripts(Options options, List<TestScript> scripts)
            throws IOException, ProtocolException {
        try (Socket socket = new Socket(options.host, options.port)) {
            socket.setSoTimeout(options.timeoutMillis);
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter output = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8), true);

            register(input, output);
            boolean allPassed = true;
            for (TestScript script : scripts) {
                TestReport report = script.run(input, output, options.timeoutMillis,
                        socket::setSoTimeout);
                report.print();
                allPassed = allPassed && report.passed();
            }
            return allPassed;
        }
    }

    private static void register(BufferedReader input, PrintWriter output)
            throws IOException, ProtocolException {
        output.println(registrationMessage().toLine());
        if (output.checkError()) {
            throw new IOException("failed to write registration to Multiplexor");
        }

        final String reply;
        try {
            reply = input.readLine();
        } catch (SocketTimeoutException error) {
            throw new IOException("timed out waiting for Multiplexor registration", error);
        }
        if (reply == null || !isRegistrationAcknowledgement(Message.parse(reply))) {
            throw new ProtocolException("Multiplexor did not acknowledge test harness registration");
        }
        System.out.println("[test-harness] registered");
    }

    public static Message registrationMessage() {
        return new Message(MessageType.REGISTER, COMPONENT_ID, "mux", "CONNECT", "TEST_HARNESS");
    }

    public static boolean isRegistrationAcknowledgement(Message message) {
        return message.getType() == MessageType.STATE
                && "mux".equals(message.getSource())
                && COMPONENT_ID.equals(message.getDestination())
                && "REGISTERED".equals(message.getAction())
                && "OK".equals(message.getValue());
    }

    private static void printUsage() {
        System.err.println("Usage: java -cp out trafficcontrol.testharness.TestHarness "
                + "[host] [port] [--timeout millis] [script-file ...]");
        System.err.println("Script commands: SEND, SEND_RAW, EXPECT, EXPECT_FIELDS, WAIT, TIMEOUT");
    }

    private static final class Options {
        private final String host;
        private final int port;
        private final int timeoutMillis;
        private final List<Path> scriptPaths;

        private Options(String host, int port, int timeoutMillis, List<Path> scriptPaths) {
            this.host = host;
            this.port = port;
            this.timeoutMillis = timeoutMillis;
            this.scriptPaths = scriptPaths;
        }

        static Options parse(String[] args) {
            String host = ControllerConfig.DEFAULT_HOST;
            int port = ControllerConfig.DEFAULT_PORT;
            int timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
            List<Path> scriptPaths = new ArrayList<>();
            int index = 0;

            if (index < args.length && !args[index].startsWith("--")) {
                host = args[index++];
            }
            if (index < args.length && !args[index].startsWith("--")) {
                port = parsePort(args[index++]);
            }
            while (index < args.length) {
                if ("--timeout".equals(args[index])) {
                    index++;
                    if (index >= args.length) {
                        throw new IllegalArgumentException("--timeout requires milliseconds");
                    }
                    timeoutMillis = parseTimeout(args[index++]);
                } else {
                    scriptPaths.add(Paths.get(args[index++]));
                }
            }
            return new Options(host, port, timeoutMillis, scriptPaths);
        }

        private static int parsePort(String value) {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed <= 0 || parsed > 65_535) {
                    throw new NumberFormatException("port out of range");
                }
                return parsed;
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("port must be 1 through 65535: " + value);
            }
        }

        private static int parseTimeout(String value) {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed <= 0) {
                    throw new NumberFormatException("timeout must be positive");
                }
                return parsed;
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("timeout must be a positive number: " + value);
            }
        }
    }
}
