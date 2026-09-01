package trafficcontrol.device;

/*
@author Bivek Panthi
*/

public interface Device {
    String getId();

    String getType();

    String getState();

    void handleCommand(String action, String value);
}
