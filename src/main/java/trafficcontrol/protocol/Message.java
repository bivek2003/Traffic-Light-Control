package trafficcontrol.protocol;

import java.util.Objects;

/** An immutable TYPE|SOURCE|DESTINATION|ACTION|VALUE protocol message. */
public final class Message {
    private final MessageType type;
    private final String source;
    private final String destination;
    private final String action;
    private final String value;

    public Message(MessageType type, String source, String destination, String action, String value) {
        this.type = Objects.requireNonNull(type, "type");
        this.source = requireField("source", source, false);
        this.destination = requireField("destination", destination, false);
        this.action = requireField("action", action, false);
        this.value = requireField("value", value, true);
    }

    public static Message parse(String line) throws ProtocolException {
        if (line == null) {
            throw new ProtocolException("message is null");
        }

        String[] fields = line.split("\\|", -1);
        if (fields.length != 5) {
            throw new ProtocolException("message must contain exactly five fields");
        }

        final MessageType type;
        try {
            type = MessageType.valueOf(fields[0]);
        } catch (IllegalArgumentException error) {
            throw new ProtocolException("unknown message type: " + fields[0]);
        }

        try {
            return new Message(type, fields[1], fields[2], fields[3], fields[4]);
        } catch (IllegalArgumentException error) {
            throw new ProtocolException(error.getMessage());
        }
    }

    public static Message command(String destination, String action, String value) {
        return new Message(MessageType.COMMAND, "controller", destination, action, value);
    }

    public static Message error(String source, String destination, String reason, String detail) {
        String safeDetail = detail == null ? "" : detail.replace('|', '/').replace('\n', ' ')
                .replace('\r', ' ');
        return new Message(MessageType.ERROR, source, destination, reason, safeDetail);
    }

    private static String requireField(String name, String value, boolean emptyAllowed) {
        Objects.requireNonNull(value, name);
        if (!emptyAllowed && value.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        if (value.indexOf('|') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(name + " contains a protocol delimiter");
        }
        return value;
    }

    public MessageType getType() {
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

    public String toLine() {
        return type + "|" + source + "|" + destination + "|" + action + "|" + value;
    }

    @Override
    public String toString() {
        return toLine();
    }
}
