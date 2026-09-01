/** A device shown and controlled by the digital twin. */
public interface Device {
    String getId();
    String getState();
    void apply(String action, String value);
}
