import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** Scripted test harness for the prototype. */
public final class Main {
    private static final int DEFAULT_PORT = 5050;

    private Main() {
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        try {
            int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
            Path script = Paths.get(args.length > 2 ? args[2] : "scripts/test-devices.txt");
            run(host, port, script);
        } catch (Exception | AssertionError error) {
            System.err.println("[FAIL] " + error.getMessage());
            System.exit(1);
        }
    }

    private static void run(String host, int port, Path script) throws Exception {
        List<String> lines = Files.readAllLines(script, StandardCharsets.UTF_8);
        try (Socket socket = new Socket(host, port);
             BufferedReader input = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter output = new PrintWriter(new OutputStreamWriter(
                     socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            output.println(new Message("REGISTER", "test-harness", "multiplexor",
                    "CONNECT", "").toLine());
            expect(input, "STATE|multiplexor|test-harness|REGISTERED|OK");

            int steps = 0;
            for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
                String line = lines.get(lineNumber).trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] command = line.split("\\s+", 2);
                if (command.length != 2) {
                    throw new IllegalArgumentException("line " + (lineNumber + 1)
                            + ": expected SEND, EXPECT, or WAIT plus a value");
                }
                if ("SEND".equals(command[0])) {
                    Message.parse(command[1]);
                    output.println(command[1]);
                    System.out.println("[SEND] " + command[1]);
                } else if ("EXPECT".equals(command[0])) {
                    expect(input, command[1]);
                } else if ("WAIT".equals(command[0])) {
                    Thread.sleep(Integer.parseInt(command[1]));
                } else {
                    throw new IllegalArgumentException("line " + (lineNumber + 1)
                            + ": unknown script command " + command[0]);
                }
                steps++;
            }
            System.out.println("[PASS] " + script + " completed " + steps + " steps");
        }
    }

    private static void expect(BufferedReader input, String expected) throws IOException {
        String actual = input.readLine();
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but received " + actual);
        }
        System.out.println("[EXPECT] " + actual);
    }
}
