package multiplexor;

// A message is one line of text sent between the programs.
// Our format is: TYPE|SOURCE|DESTINATION|ACTION|VALUE
// Example: COMMAND|controller|light-north|SET_COLOR|GREEN
//
// This class holds those 5 pieces. Later I will add the code that
// reads a line and splits it up.
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
