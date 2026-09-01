/** The shared five-field socket message used by the prototype. */
public final class Message {
    private final String type;
    private final String source;
    private final String destination;
    private final String action;
    private final String value;

    public Message(String type, String source, String destination, String action, String value) {
        this.type = require("type", type, false);
        this.source = require("source", source, false);
        this.destination = require("destination", destination, false);
        this.action = require("action", action, false);
        this.value = require("value", value, true);
    }

    public static Message parse(String line) {
        if (line == null) {
            throw new IllegalArgumentException("message is null");
        }
        String[] fields = line.split("\\|", -1);
        if (fields.length != 5) {
            throw new IllegalArgumentException("message must have five fields: " + line);
        }
        return new Message(fields[0], fields[1], fields[2], fields[3], fields[4]);
    }

    private static String require(String name, String value, boolean emptyAllowed) {
        if (value == null || (!emptyAllowed && value.isEmpty())) {
            throw new IllegalArgumentException(name + " is required");
        }
        if (value.contains("|") || value.contains("\n") || value.contains("\r")) {
            throw new IllegalArgumentException(name + " contains an invalid character");
        }
        return value;
    }

    public String toLine() {
        return type + "|" + source + "|" + destination + "|" + action + "|" + value;
    }

    public String getType() { return type; }
    public String getSource() { return source; }
    public String getDestination() { return destination; }
    public String getAction() { return action; }
    public String getValue() { return value; }
}
