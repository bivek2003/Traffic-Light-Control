package multiplexor;

/*
@author Utshab Niraula
*/

// Tests for my Message class, which is the part of the Multiplexor that reads
// a line of text and splits it into the 5 fields.
//
// None of these tests open a socket, so they run instantly and I can check my
// parsing without starting the whole system up.
//
// Run it with:
//    java -cp out multiplexor.MessageTest
public final class MessageTest {

    private static int testsRun;

    public static void main(String[] args) {
        testGoodMessageParses();
        testEmptyLastFieldIsAllowed();
        testLineIsPutBackTogetherTheSameWay();
        testTooFewFieldsIsRejected();
        testTooManyFieldsIsRejected();
        testNullAndBlankLinesAreRejected();
        testMissingSourceOrDestinationIsRejected();
        testExtraSpacesAreTrimmed();

        System.out.println("MessageTest: " + testsRun + " tests passed");
    }

    // A normal message should end up with each field in the right place.
    private static void testGoodMessageParses() {
        Message m = Message.parse("COMMAND|controller|light-north|SET_COLOR|GREEN");

        check(m != null, "a good message should parse");
        check(m.getType().equals("COMMAND"), "type should be COMMAND");
        check(m.getSource().equals("controller"), "source should be controller");
        check(m.getDestination().equals("light-north"), "destination should be light-north");
        check(m.getAction().equals("SET_COLOR"), "action should be SET_COLOR");
        check(m.getValue().equals("GREEN"), "value should be GREEN");
    }

    // This is the one that caught me out. Java's split() throws away empty
    // fields at the end unless you pass -1, so a message ending in | looked
    // like it only had 4 fields and got rejected even though it is fine.
    private static void testEmptyLastFieldIsAllowed() {
        Message m = Message.parse("STATE|light-north|controller|COLOR|");

        check(m != null, "an empty last field should still parse");
        check(m.getValue().equals(""), "the value should be empty");
    }

    // Parsing a line and then building it again should give back the same text.
    private static void testLineIsPutBackTogetherTheSameWay() {
        String line = "EVENT|detector-north-1|controller|VEHICLE_DETECTED|NORTH_LANE_1";
        Message m = Message.parse(line);

        check(m != null, "the event message should parse");
        check(m.toLine().equals(line), "toLine should give back the same text");
    }

    // The spec says every message has exactly 5 fields, so 4 is not allowed.
    private static void testTooFewFieldsIsRejected() {
        check(Message.parse("COMMAND|controller|light-north|SET_COLOR") == null,
                "4 fields should be rejected");
        check(Message.parse("COMMAND|controller") == null,
                "2 fields should be rejected");
    }

    // And neither is 6.
    private static void testTooManyFieldsIsRejected() {
        check(Message.parse("COMMAND|a|b|c|d|e") == null,
                "6 fields should be rejected");
    }

    // These should be refused instead of crashing the Multiplexor.
    private static void testNullAndBlankLinesAreRejected() {
        check(Message.parse(null) == null, "null should be rejected");
        check(Message.parse("") == null, "an empty line should be rejected");
        check(Message.parse("   ") == null, "a line of spaces should be rejected");
        check(Message.parse("HELLO") == null, "a line with no | should be rejected");
    }

    // Without a sender or a destination the Multiplexor cannot do anything
    // useful with the message, so it should not accept it.
    private static void testMissingSourceOrDestinationIsRejected() {
        check(Message.parse("COMMAND||light-north|SET_COLOR|GREEN") == null,
                "a missing source should be rejected");
        check(Message.parse("COMMAND|controller||SET_COLOR|GREEN") == null,
                "a missing destination should be rejected");
    }

    // Programs might send a line with spaces around it, so the fields get
    // trimmed and the message should still work.
    private static void testExtraSpacesAreTrimmed() {
        Message m = Message.parse("  COMMAND | controller | light-north | SET_COLOR | GREEN  ");

        check(m != null, "a message with extra spaces should parse");
        check(m.getType().equals("COMMAND"), "the type should have no spaces left");
        check(m.getDestination().equals("light-north"), "the destination should have no spaces left");
    }

    // Counts the test and stops the program if the check failed, so a broken
    // test is impossible to miss.
    private static void check(boolean passed, String what) {
        testsRun++;

        if (!passed) {
            throw new IllegalStateException("FAILED: " + what);
        }
    }
}
