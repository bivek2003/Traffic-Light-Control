package trafficcontrol.protocol;

/** Indicates that a line does not satisfy the shared five-field protocol. */
public class ProtocolException extends Exception {
    private static final long serialVersionUID = 1L;

    public ProtocolException(String message) {
        super(message);
    }
}
