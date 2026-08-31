package trafficcontrol.protocol;

/** The only message types permitted by the socket protocol. */
public enum MessageType {
    REGISTER,
    COMMAND,
    STATE,
    EVENT,
    ERROR
}
