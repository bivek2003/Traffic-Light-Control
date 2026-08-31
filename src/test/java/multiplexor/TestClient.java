package multiplexor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

// A small program I wrote so I can test the Multiplexor without having to
// type messages by hand in a terminal.
//
// It connects, sends a few messages, then disconnects.
// Watch the Multiplexor window to see what it received.
//
// IMPORTANT: start the Multiplexor FIRST, then run this one.
public class TestClient {

    public static void main(String[] args) {
        int port = Multiplexor.DEFAULT_PORT;

        // Let me pass a different port if I want to.
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        // The messages I want to try. The last one is broken on purpose,
        // so I can check the Multiplexor notices it is not a real message.
        String[] messages = {
            "REGISTER|controller|mux|CONNECT|",
            "COMMAND|controller|light-north|SET_COLOR|GREEN",
            "EVENT|detector-north-1|controller|VEHICLE_DETECTED|NORTH_LANE_1",
            "HELLO"
        };

        try {
            Socket socket = new Socket("localhost", port);
            System.out.println("connected to the multiplexor on port " + port);

            // The "true" makes it send each line straight away.
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            for (String message : messages) {
                System.out.println("sending: " + message);
                out.println(message);

                // Wait half a second between messages so it is easier to
                // watch what is happening in the other window.
                Thread.sleep(500);
            }

            socket.close();
            System.out.println("done, disconnected");

        } catch (IOException e) {
            System.out.println("could not connect. Is the Multiplexor running? " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("the waiting got interrupted");
        }
    }
}
