package trafficcontrol.testharness;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.ProtocolException;

/** Parses and runs test-harness script files. */
public final class TestScript {
    private final String name;
    private final List<Step> steps;

    private TestScript(String name, List<Step> steps) {
        this.name = name;
        this.steps = steps;
    }

    public static TestScript load(Path path) throws IOException, ProtocolException {
        List<Step> parsed = new ArrayList<>();
        List<String> lines = Files.readAllLines(path);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            parsed.add(parseStep(path + ":" + (index + 1), line));
        }
        return new TestScript(path.getFileName().toString(), parsed);
    }

    public static TestScript defaultSmokeTest() throws ProtocolException {
        List<Step> defaultSteps = Arrays.asList(
                parseStep("default:1", "SEND STATE|test-harness|missing-device|PING|hello"),
                parseStep("default:2", "EXPECT_FIELDS ERROR|mux|test-harness|UNKNOWN_DESTINATION|*"));
        return new TestScript("default-multiplexer-smoke", defaultSteps);
    }

    public TestReport run(BufferedReader input, PrintWriter output, int timeoutMillis)
            throws IOException {
        return run(input, output, timeoutMillis, null);
    }

    public TestReport run(BufferedReader input, PrintWriter output, int timeoutMillis,
            TimeoutSetter timeoutSetter) throws IOException {
        Context context = new Context(input, output, timeoutMillis, timeoutSetter);
        TestReport report = new TestReport(name);
        for (Step step : steps) {
            try {
                step.run(context, report);
            } catch (AssertionError | IOException | InterruptedException error) {
                if (error instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                report.fail(step.label() + " -> " + error.getMessage());
                break;
            }
        }
        return report;
    }

    static Step parseStep(String location, String line) throws ProtocolException {
        String[] parts = line.split("\\s+", 2);
        String command = parts[0];
        String argument = parts.length > 1 ? parts[1].trim() : "";

        switch (command) {
            case "SEND":
                Message.parse(argument);
                return new SendStep(location, argument);
            case "SEND_RAW":
                requireArgument(location, command, argument);
                return new SendStep(location, argument);
            case "EXPECT":
                Message.parse(argument);
                return new ExpectExactStep(location, argument);
            case "EXPECT_FIELDS":
                return new ExpectFieldsStep(location, argument);
            case "WAIT":
                return new WaitStep(location, parsePositiveInt(location, command, argument));
            case "TIMEOUT":
                return new TimeoutStep(location, parsePositiveInt(location, command, argument));
            default:
                throw new ProtocolException(location + " unknown script command: " + command);
        }
    }

    private static int parsePositiveInt(String location, String command, String value)
            throws ProtocolException {
        requireArgument(location, command, value);
        try {
            int number = Integer.parseInt(value);
            if (number <= 0) {
                throw new NumberFormatException("not positive");
            }
            return number;
        } catch (NumberFormatException error) {
            throw new ProtocolException(location + " " + command + " requires a positive number");
        }
    }

    private static void requireArgument(String location, String command, String value)
            throws ProtocolException {
        if (value.isEmpty()) {
            throw new ProtocolException(location + " " + command + " requires an argument");
        }
    }

    interface Step {
        void run(Context context, TestReport report) throws IOException, InterruptedException;

        String label();
    }

    public interface TimeoutSetter {
        void setTimeoutMillis(int timeoutMillis) throws IOException;
    }

    static final class Context {
        private final BufferedReader input;
        private final PrintWriter output;
        private final TimeoutSetter timeoutSetter;
        private int timeoutMillis;

        Context(BufferedReader input, PrintWriter output, int timeoutMillis,
                TimeoutSetter timeoutSetter) {
            this.input = input;
            this.output = output;
            this.timeoutSetter = timeoutSetter;
            this.timeoutMillis = timeoutMillis;
        }

        void send(String line) {
            output.println(line);
            if (output.checkError()) {
                throw new AssertionError("socket write failed");
            }
        }

        String receive() throws IOException {
            try {
                String line = input.readLine();
                if (line == null) {
                    throw new AssertionError("socket closed while waiting for response");
                }
                return line;
            } catch (SocketTimeoutException error) {
                throw new AssertionError("timed out after " + timeoutMillis + " ms");
            }
        }

        int getTimeoutMillis() {
            return timeoutMillis;
        }

        void setTimeoutMillis(int timeoutMillis) throws IOException {
            if (timeoutSetter != null) {
                timeoutSetter.setTimeoutMillis(timeoutMillis);
            }
            this.timeoutMillis = timeoutMillis;
        }
    }

    private static final class SendStep implements Step {
        private final String location;
        private final String line;

        SendStep(String location, String line) {
            this.location = location;
            this.line = line;
        }

        @Override
        public void run(Context context, TestReport report) {
            context.send(line);
            report.pass(location + " sent " + line);
        }

        @Override
        public String label() {
            return location;
        }
    }

    private static final class ExpectExactStep implements Step {
        private final String location;
        private final String expected;

        ExpectExactStep(String location, String expected) {
            this.location = location;
            this.expected = expected;
        }

        @Override
        public void run(Context context, TestReport report) throws IOException {
            String actual = context.receive();
            if (!expected.equals(actual)) {
                throw new AssertionError("expected " + expected + " but received " + actual);
            }
            report.pass(location + " received " + actual);
        }

        @Override
        public String label() {
            return location;
        }
    }

    private static final class ExpectFieldsStep implements Step {
        private final String location;
        private final String[] expectedFields;

        ExpectFieldsStep(String location, String expected) throws ProtocolException {
            this.location = location;
            Message.parse(expected.replace("*", "ANY"));
            this.expectedFields = expected.split("\\|", -1);
            if (expectedFields.length != 5) {
                throw new ProtocolException(location + " EXPECT_FIELDS requires five fields");
            }
        }

        @Override
        public void run(Context context, TestReport report) throws IOException {
            String actual = context.receive();
            String[] actualFields = actual.split("\\|", -1);
            if (actualFields.length != 5) {
                throw new AssertionError("received malformed message: " + actual);
            }
            for (int index = 0; index < expectedFields.length; index++) {
                if (!"*".equals(expectedFields[index])
                        && !expectedFields[index].equals(actualFields[index])) {
                    throw new AssertionError("expected " + String.join("|", expectedFields)
                            + " but received " + actual);
                }
            }
            report.pass(location + " received " + actual);
        }

        @Override
        public String label() {
            return location;
        }
    }

    private static final class WaitStep implements Step {
        private final String location;
        private final int millis;

        WaitStep(String location, int millis) {
            this.location = location;
            this.millis = millis;
        }

        @Override
        public void run(Context context, TestReport report) throws InterruptedException {
            Thread.sleep(millis);
            report.pass(location + " waited " + millis + " ms");
        }

        @Override
        public String label() {
            return location;
        }
    }

    private static final class TimeoutStep implements Step {
        private final String location;
        private final int millis;

        TimeoutStep(String location, int millis) {
            this.location = location;
            this.millis = millis;
        }

        @Override
        public void run(Context context, TestReport report) throws IOException {
            context.setTimeoutMillis(millis);
            report.pass(location + " timeout set to " + millis + " ms");
        }

        @Override
        public String label() {
            return location;
        }
    }
}
