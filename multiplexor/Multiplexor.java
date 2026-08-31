package multiplexor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

// This is my part of the project (Member 2).
//
// The Multiplexor sits in the middle. The controller, the devices and the
// test harness all connect to it and it passes messages between them.
//
// Right now it only takes ONE program at a time and just prints whatever
// that program sends. Next I need to make it handle a few programs at the
// same time, and then actually deliver the messages.
//
// To run it:
//    javac -d out multiplexor/*.java
//    java -cp out multiplexor.Multiplexor 5000
public class Multiplexor {

    // The port to use if nobody gives one when starting the program.
    public static final int DEFAULT_PORT = 5000;

    private int port;

    public Multiplexor(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // The port can be given as the first argument.
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("The port has to be a number. You typed: " + args[0]);
                return;
            }
        }

        Multiplexor mux = new Multiplexor(port);
        mux.start();
    }

    // Opens the server socket and waits for a program to connect.
    public void start() {
        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("[mux] listening on port " + port);

            // accept() waits here until somebody connects.
            Socket socket = server.accept();
            System.out.println("[mux] someone connected");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // Keep reading lines until the other program closes.
            // readLine() gives back null when that happens.
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[mux] got: " + line);

                // Try turning the line into a Message so I can check my
                // parsing works. It does not go anywhere yet.
                Message m = Message.parse(line);
                if (m == null) {
                    System.out.println("[mux] that was not a proper message");
                } else {
                    System.out.println("[mux] it is for: " + m.getDestination());
                }
            }

            System.out.println("[mux] the program disconnected");
            socket.close();
            server.close();

        } catch (IOException e) {
            System.out.println("[mux] something went wrong: " + e.getMessage());
        }
    }
}
