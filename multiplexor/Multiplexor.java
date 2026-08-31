package multiplexor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// This is my part of the project (Member 2).
//
// The Multiplexor sits in the middle. The controller, the devices and the
// test harness all connect to it and it passes messages between them.
//
// Right now it can take several programs at the same time and it remembers
// the name each one registers with. It still does not deliver anything,
// that is next.
//
// To run it:
//    javac -d out multiplexor/*.java
//    java -cp out multiplexor.Multiplexor
public class Multiplexor {

    // The port to use if nobody gives one when starting the program.
    // Note: do not use 5000 on a Mac. macOS already uses that port for
    // AirPlay, so the program fails with "Address already in use".
    public static final int DEFAULT_PORT = 5050;

    private int port;

    // Everybody who is connected right now.
    // Each connection runs on its own thread and they all touch this list,
    // so I have to lock it before changing it or two threads can mess it up.
    private List<ClientHandler> clients = new ArrayList<>();

    // The name each program registered with, so I can find the right one
    // when I start delivering messages. Locked the same way as the list.
    private Map<String, ClientHandler> namedClients = new HashMap<>();

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

    // Opens the server socket and keeps waiting for programs to connect.
    public void start() {
        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("[mux] listening on port " + port);

            // Keep going forever so more than one program can join.
            while (true) {
                // accept() waits here until somebody connects.
                Socket socket = server.accept();

                // Give this connection its own handler on its own thread.
                ClientHandler handler = new ClientHandler(socket, this);
                addClient(handler);

                Thread t = new Thread(handler);
                t.start();
            }
        } catch (IOException e) {
            System.out.println("[mux] something went wrong: " + e.getMessage());
        }
    }

    // Adds a program to the list when it connects.
    public synchronized void addClient(ClientHandler handler) {
        clients.add(handler);
        System.out.println("[mux] " + clients.size() + " program(s) connected");
    }

    // Saves the name a program registered with.
    public synchronized void addName(String name, ClientHandler handler) {
        namedClients.put(name, handler);
        System.out.println("[mux] names so far: " + namedClients.keySet());
    }

    // Takes a program off both lists when it disconnects.
    public synchronized void removeClient(ClientHandler handler) {
        clients.remove(handler);

        if (handler.getName() != null) {
            namedClients.remove(handler.getName());
        }

        System.out.println("[mux] a program left, " + clients.size() + " still connected");
    }
}
