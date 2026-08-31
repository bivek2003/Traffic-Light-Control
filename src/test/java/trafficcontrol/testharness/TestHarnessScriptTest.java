package trafficcontrol.testharness;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.ProtocolException;

/** Dependency-free tests for the scripted test harness. */
public final class TestHarnessScriptTest {
    private static int testsRun;

    public static void main(String[] args) throws Exception {
        testRegistrationMessage();
        testExactExpectationAndSend();
        testWildcardExpectation();
        testScriptValidation();
        System.out.println("TestHarnessScriptTest: " + testsRun + " tests passed");
    }

    private static void testRegistrationMessage() throws Exception {
        check("REGISTER|test-harness|mux|CONNECT|TEST_HARNESS".equals(
                TestHarness.registrationMessage().toLine()), "registration message changed");
        Message acknowledgement = Message.parse("STATE|mux|test-harness|REGISTERED|OK");
        check(TestHarness.isRegistrationAcknowledgement(acknowledgement),
                "valid test harness registration acknowledgement was rejected");
        testsRun++;
    }

    private static void testExactExpectationAndSend() throws Exception {
        TestScript.Step send = TestScript.parseStep("test:1",
                "SEND EVENT|test-harness|controller|PING|hello");
        TestScript.Step expect = TestScript.parseStep("test:2",
                "EXPECT STATE|mux|test-harness|REGISTERED|OK");
        StringWriter outputBuffer = new StringWriter();
        TestScript.Context context = new TestScript.Context(
                new BufferedReader(new StringReader("STATE|mux|test-harness|REGISTERED|OK\n")),
                new PrintWriter(outputBuffer, true), 5_000, null);
        TestReport report = new TestReport("inline");

        send.run(context, report);
        expect.run(context, report);

        check(outputBuffer.toString().contains("EVENT|test-harness|controller|PING|hello"),
                "SEND did not write to the socket output");
        check(report.getFailed() == 0 && report.getPassed() == 2,
                "exact script steps should pass");
        testsRun++;
    }

    private static void testWildcardExpectation() throws Exception {
        TestScript.Step expect = TestScript.parseStep("test:1",
                "EXPECT_FIELDS ERROR|mux|test-harness|UNKNOWN_DESTINATION|*");
        TestScript.Context context = new TestScript.Context(
                new BufferedReader(new StringReader(
                        "ERROR|mux|test-harness|UNKNOWN_DESTINATION|missing-device\n")),
                new PrintWriter(new StringWriter(), true), 5_000, null);
        TestReport report = new TestReport("inline");

        expect.run(context, report);

        check(report.getFailed() == 0 && report.getPassed() == 1,
                "wildcard expectation should pass");
        testsRun++;
    }

    private static void testScriptValidation() {
        expectProtocolError("SEND EVENT|test-harness|controller|PING");
        expectProtocolError("EXPECT_FIELDS ERROR|mux|test-harness|UNKNOWN_DESTINATION");
        expectProtocolError("WAIT zero");
        testsRun++;
    }

    private static void expectProtocolError(String line) {
        try {
            TestScript.parseStep("test", line);
            throw new AssertionError("expected ProtocolException for: " + line);
        } catch (ProtocolException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
