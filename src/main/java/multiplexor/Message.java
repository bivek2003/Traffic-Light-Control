package multiplexor;

// A message is one line of text sent between the programs.
// Our format is: TYPE|SOURCE|DESTINATION|ACTION|VALUE
// Example: COMMAND|controller|light-north|SET_COLOR|GREEN
//
// This class holds those 5 pieces and can read a line and split it up.
public class Message {

    private String type;         // REGISTER, COMMAND, STATE, EVENT or ERROR
    private String source;       // who sent it, like "controller"
    private String destination;  // who it is for, like "light-north"
    private String action;       // what to do, like "SET_COLOR"
    private String value;        // the detail, like "GREEN"

    public Message(String type, String source, String destination, String action, String value) {
        this.type = type;
        this.source = source;
        this.destination = destination;
        this.action = action;
        this.value = value;
    }

    // Takes a line of text and turns it into a Message.
    // Gives back null if the line is not a proper message. Then whoever
    // called this can send an ERROR back.
    public static Message parse(String line) {
        if (line == null) {
            return null;
        }

        // split() uses regex and the | symbol means something special in
        // regex, so it has to be written as "\\|" or this does not work.
        // The -1 keeps the last piece even when it is empty, because a
        // message like STATE|light-north|controller|COLOR| is allowed.
        String[] pieces = line.trim().split("\\|", -1);

        // Take the spaces off each field too. Trimming the whole line only
        // cleans up the ends, so "COMMAND | controller" would still keep the
        // spaces around the middle fields. My test caught this.
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = pieces[i].trim();
        }

        // The spec says a message has to have exactly 5 fields.
        if (pieces.length != 5) {
            return null;
        }

        // A message with no sender or no destination is no use to us.
        if (pieces[1].isEmpty() || pieces[2].isEmpty()) {
            return null;
        }

        return new Message(pieces[0], pieces[1], pieces[2], pieces[3], pieces[4]);
    }

    // Puts the 5 pieces back together into one line so it can be sent.
    public String toLine() {
        return type + "|" + source + "|" + destination + "|" + action + "|" + value;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public String getAction() {
        return action;
    }

    public String getValue() {
        return value;
    }
}
