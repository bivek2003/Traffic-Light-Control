import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Routes messages between Main and the JavaFX digital twin. */
public final class Multiplexor {
    public static final int DEFAULT_PORT = 5050;
    private final Map<String, Client> clients = new ConcurrentHashMap<String, Client>();

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new Multiplexor().start(port);
    }

    private void start(int port) throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[multiplexor] listening on port " + port);
            while (true) {
                Client client = new Client(server.accept());
                Thread thread = new Thread(client, "multiplexor-client");
                thread.setDaemon(true);
                thread.start();
            }
        }
    }

    private void route(Client sender, Message message) {
        if ("REGISTER".equals(message.getType())) {
            sender.id = message.getSource();
            Client previous = clients.putIfAbsent(sender.id, sender);
            if (previous != null && previous != sender) {
                sender.send("ERROR|multiplexor|" + sender.id + "|DUPLICATE_ID|" + sender.id);
                return;
            }
            sender.send("STATE|multiplexor|" + sender.id + "|REGISTERED|OK");
            System.out.println("[multiplexor] registered " + sender.id);
            return;
        }

        if (sender.id == null) {
            sender.send("ERROR|multiplexor|unknown|NOT_REGISTERED|");
            return;
        }

        String targetId = "test-harness".equals(sender.id)
                ? "digital-twin" : "test-harness";
        Client target = clients.get(targetId);
        if (target == null) {
            sender.send("ERROR|multiplexor|" + sender.id + "|NOT_CONNECTED|" + targetId);
            return;
        }
        target.send(message.toLine());
        System.out.println("[multiplexor] " + sender.id + " -> " + targetId
                + ": " + message.toLine());
    }

    private final class Client implements Runnable {
        private final Socket socket;
        private final BufferedReader input;
        private final PrintWriter output;
        private String id;

        private Client(Socket socket) throws IOException {
            this.socket = socket;
            input = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            output = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = input.readLine()) != null) {
                    try {
                        route(this, Message.parse(line));
                    } catch (IllegalArgumentException error) {
                        send("ERROR|multiplexor|" + (id == null ? "unknown" : id)
                                + "|BAD_MESSAGE|" + error.getMessage().replace('|', '/'));
                    }
                }
            } catch (IOException error) {
                System.err.println("[multiplexor] connection lost: " + error.getMessage());
            } finally {
                if (id != null) {
                    clients.remove(id, this);
                }
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // Connection is already closing.
                }
            }
        }

        private synchronized void send(String line) {
            output.println(line);
        }
    }
}
