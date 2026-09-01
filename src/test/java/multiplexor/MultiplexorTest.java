package multiplexor;

/*
@author Utshab Niraula
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Tests for the routing part of the Multiplexor, so the delivering and the
// ERROR replies.
//
// These ones do open real sockets, because that is the whole point of this
// part. It starts a Multiplexor on its own port, connects to it like a real
// program would, and checks what comes back.
//
// Run it with:
//    java -cp out multiplexor.MultiplexorTest
public final class MultiplexorTest {

    // A port nobody else in the project uses, so running these tests does not
    // clash with a Multiplexor I already have open in another window.
    private static final int TEST_PORT = 5599;

    private static int testsRun;

    public static void main(String[] args) throws Exception {
        startMultiplexor();

        testRegisterGetsAReplyBack();
        testMessageReachesTheRightProgram();
        testUnknownDestinationGivesAnError();
        testBadLineGivesAnError();
        testSendingBeforeRegisteringGivesAnError();
        testErrorLineStillHasFiveFields();
        testTwoProgramsCanBeConnectedAtOnce();

        System.out.println("MultiplexorTest: " + testsRun + " tests passed");

        // The Multiplexor never stops on its own and the client threads keep
        // running, so the program would just hang here. Exiting on purpose.
        System.exit(0);
    }

    // Starts a Multiplexor on its own thread and waits a moment so it is
    // definitely listening before the tests start connecting to it.
    private static void startMultiplexor() throws Exception {
        Multiplexor mux = new Multiplexor(TEST_PORT);

        Thread t = new Thread(new Runnable() {
            public void run() {
                mux.start();
            }
        });
        t.start();

        Thread.sleep(400);
    }

    private static void testRegisterGetsAReplyBack() throws Exception {
        Program light = new Program("light-north");

        check(light.reply.equals("STATE|mux|light-north|REGISTERED|OK"),
                "registering should get an OK reply back");

        light.close();
    }

    private static void testMessageReachesTheRightProgram() throws Exception {
        Program controller = new Program("controller");
        Program light = new Program("light-north");

        String line = "COMMAND|controller|light-north|SET_COLOR|GREEN";
        controller.send(line);

        check(light.read().equals(line),
                "the light should get the command exactly as it was sent");

        controller.close();
        light.close();
    }

    private static void testUnknownDestinationGivesAnError() throws Exception {
        Program controller = new Program("controller");

        controller.send("COMMAND|controller|light-west|SET_COLOR|RED");
        String answer = controller.read();

        check(answer.contains("UNKNOWN_DESTINATION"),
                "sending to a program that is not connected should give an error");
        check(answer.contains("light-west"),
                "the error should say which name was not found");

        controller.close();
    }

    private static void testBadLineGivesAnError() throws Exception {
        Program controller = new Program("controller");

        controller.send("HELLO");

        check(controller.read().contains("BAD_MESSAGE"),
                "a line that is not a proper message should give an error");

        controller.close();
    }

    private static void testSendingBeforeRegisteringGivesAnError() throws Exception {
        // On purpose does NOT register first.
        Socket socket = new Socket("localhost", TEST_PORT);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        out.println("COMMAND|ghost|light-north|SET_COLOR|RED");

        check(in.readLine().contains("NOT_REGISTERED"),
                "sending before registering should give an error");

        socket.close();
    }

    // This is the bug my own error messages had at first. The broken line gets
    // put in the last field, and if that line has | in it then the ERROR ends
    // up with more than 5 fields and nobody can read it.
    private static void testErrorLineStillHasFiveFields() throws Exception {
        Program controller = new Program("controller");

        controller.send("COMMAND|controller|light-north|SET_COLOR");
        String answer = controller.read();

        check(answer.split("\\|", -1).length == 5,
                "an ERROR must still have exactly 5 fields");
        check(Message.parse(answer) != null,
                "the program getting the ERROR should be able to parse it");

        controller.close();
    }

    private static void testTwoProgramsCanBeConnectedAtOnce() throws Exception {
        Program a = new Program("light-east");
        Program b = new Program("light-west");

        a.send("STATE|light-east|light-west|COLOR|GREEN");
        check(b.read().contains("light-east"), "the first program's message should arrive");

        b.send("STATE|light-west|light-east|COLOR|RED");
        check(a.read().contains("light-west"), "the second program's message should arrive too");

        a.close();
        b.close();
    }

    // A little helper that connects to the Multiplexor and registers, so the
    // tests above do not have to repeat all of this every time.
    private static final class Program {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String reply;

        private Program(String name) throws IOException {
            socket = new Socket("localhost", TEST_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("REGISTER|" + name + "|mux|CONNECT|");
            reply = in.readLine();
        }

        private void send(String line) {
            out.println(line);
        }

        private String read() throws IOException {
            return in.readLine();
        }

        private void close() throws IOException {
            socket.close();
        }
    }

    private static void check(boolean passed, String what) {
        testsRun++;

        if (!passed) {
            throw new IllegalStateException("FAILED: " + what);
        }
    }
}
