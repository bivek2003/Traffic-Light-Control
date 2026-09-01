package trafficcontrol.device;

public final class DeviceStateTest {
    public static void main(String[] args) {
        testTrafficLight();
        testPedestrianButton();
        testVehicleDetector();
        testAuxiliaryDevice();
        System.out.println("DeviceStateTest passed");
    }

    private static void testTrafficLight() {
        TrafficLight light = new TrafficLight("light-north");
        check("RED".equals(light.getState()), "traffic light should start red");
        light.handleCommand("SET_COLOR", "GREEN");
        check("GREEN".equals(light.getState()), "traffic light did not change color");
        expectIllegalArgument(() -> light.handleCommand("SET_COLOR", "BLUE"));
    }

    private static void testPedestrianButton() {
        PedestrianButton button = new PedestrianButton("button-east");
        check("IDLE".equals(button.getState()), "button should start idle");
        button.press();
        check("REQUESTED".equals(button.getState()), "button did not store request");
        button.handleCommand("CLEAR_REQUEST", "");
        check("IDLE".equals(button.getState()), "button request did not clear");
    }

    private static void testVehicleDetector() {
        VehicleDetector detector = new VehicleDetector("detector-south-2");
        check("CLEAR".equals(detector.getState()), "detector should start clear");
        detector.detectVehicle();
        check("DETECTED".equals(detector.getState()), "vehicle was not detected");
        detector.clearVehicle();
        check("CLEAR".equals(detector.getState()), "vehicle did not clear");
    }

    private static void testAuxiliaryDevice() {
        AuxiliaryDevice device = new AuxiliaryDevice("aux-weather");
        check("NORMAL".equals(device.getState()), "auxiliary device should start normal");
        device.setStatus("FAULT");
        check("FAULT".equals(device.getState()), "auxiliary status did not change");
        expectIllegalArgument(() -> device.setStatus("UNKNOWN"));
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
