package trafficcontrol.device;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.MessageType;
import trafficcontrol.protocol.ProtocolException;

public final class DeviceConnection implements AutoCloseable {
    public interface Listener {
        void lineReceived(DeviceConnection connection, String line);

        void connectionLost(DeviceConnection connection);
    }

    private final String id;
    private final String host;
    private final int port;
    private final Listener listener;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread readerThread;
    private volatile boolean closed;

    public DeviceConnection(String id, String host, int port, Listener listener) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.listener = listener;
    }

    public void start() throws IOException, ProtocolException {
        socket = new Socket(host, port);
        input = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
        output = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream(), StandardCharsets.UTF_8), true);

        send(registrationMessage());
        String replyLine = input.readLine();
        if (replyLine == null || !isRegistrationReply(Message.parse(replyLine))) {
            close();
            throw new ProtocolException("Multiplexor did not register " + id);
        }

        readerThread = new Thread(this::readLoop, "device-reader-" + id);
        readerThread.start();
    }

    private void readLoop() {
        try {
            String line;
            while (!closed && (line = input.readLine()) != null) {
                listener.lineReceived(this, line);
            }
        } catch (IOException error) {
            if (!closed) {
                System.err.println("[devices] connection lost for " + id + ": "
                        + error.getMessage());
            }
        } finally {
            if (!closed) {
                listener.connectionLost(this);
            }
        }
    }

    public synchronized void send(Message message) throws IOException {
        if (closed || output == null) {
            throw new IOException("connection is closed for " + id);
        }
        output.println(message.toLine());
        if (output.checkError()) {
            throw new IOException("failed to send message for " + id);
        }
    }

    private Message registrationMessage() {
        return new Message(MessageType.REGISTER, id, "mux", "CONNECT", "DEVICE_HUB");
    }

    private boolean isRegistrationReply(Message message) {
        return message.getType() == MessageType.STATE
                && "mux".equals(message.getSource())
                && id.equals(message.getDestination())
                && "REGISTERED".equals(message.getAction())
                && "OK".equals(message.getValue());
    }

    public String getId() {
        return id;
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // The connection is already closing.
            }
        }
    }
}
