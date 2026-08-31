package multiplexor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// This class looks after ONE program that is connected to the Multiplexor.
//
// Every program that connects gets its own ClientHandler, and each one runs
// on its own thread. It has to be a separate thread for each because
// readLine() makes the program stop and wait until something arrives. If
// they all shared one thread, one quiet program would freeze everybody else.
public class ClientHandler implements Runnable {

    private Socket socket;
    private Multiplexor mux;
    private BufferedReader in;
    private PrintWriter out;

    // The name this program registered with, like "controller".
    // It stays null until a REGISTER message arrives.
    private String name = null;

    public ClientHandler(Socket socket, Multiplexor mux) throws IOException {
        this.socket = socket;
        this.mux = mux;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // The "true" makes it send each line straight away instead of
        // holding on to it.
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    public String getName() {
        return name;
    }

    // Sends one line of text to this program.
    //
    // Synchronized because other programs' threads can all send to this
    // same program at the same time. Without it two messages get mixed
    // together into one broken line.
    public synchronized void send(String line) {
        out.println(line);
    }

    // This is what runs on the thread. It keeps reading lines until the
    // program on the other end disconnects.
    public void run() {
        System.out.println("[mux] someone connected");

        try {
            String line;

            // readLine() gives back null when the other side closes.
            while ((line = in.readLine()) != null) {

                // Ignore blank lines instead of complaining about them.
                if (line.trim().isEmpty()) {
                    continue;
                }

                System.out.println("[mux] got: " + line);
                handle(line);
            }
        } catch (IOException e) {
            // This happens if a program gets closed suddenly instead of
            // disconnecting properly.
            System.out.println("[mux] connection lost: " + e.getMessage());
        }

        // Whatever happened, tidy up.
        mux.removeClient(this);
        closeSocket();
    }

    // Works out what to do with one line that came in.
    private void handle(String line) {
        Message m = Message.parse(line);

        // parse() gives back null when the line is not a proper message.
        if (m == null) {
            System.out.println("[mux] that was not a proper message");
            return;
        }

        // A REGISTER is how a program tells us its name. I deal with it
        // here instead of passing it on to anybody else.
        if (m.getType().equals("REGISTER")) {
            name = m.getSource();
            mux.addName(name, this);
            System.out.println("[mux] registered: " + name);

            // Send a reply so the program knows it worked and does not
            // have to guess.
            send(new Message("STATE", "mux", name, "REGISTERED", "OK").toLine());
            return;
        }

        // Everything else has to come from a program that told us its name
        // first, otherwise we would not know who it is.
        if (name == null) {
            System.out.println("[mux] this program has not registered yet");
            return;
        }

        // Anything left over is a normal message, so let the Multiplexor
        // pass it on to whoever it is for.
        mux.deliver(m, this);
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException e) {
            // Closing anyway, nothing useful to do here.
        }
    }
}
