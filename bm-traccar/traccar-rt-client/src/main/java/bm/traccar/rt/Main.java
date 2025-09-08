package bm.traccar.rt;

import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import bm.traccar.generated.model.dto.User;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Main {
  public static void main(String[] args) {
    // Get the singleton instance of our state manager
    TraccarRealTime stateManager = TraccarRealTime.getInstance();

    // 1. SIMULATE LOGIN (from REST API)
    System.out.println("--- Logging in... ---");
    User user = new User();
    // user.setId(1);
    user.setName("admin");
    user.setEmail("admin@example.com");
    stateManager.loginUser(user);
    System.out.println(
        "Logged in user: " + stateManager.getCurrentUser().map(User::getName).orElse("None"));
    System.out.println();

    // 2. SIMULATE INITIAL DATA LOAD (from REST API)
    System.out.println("--- Loading initial devices via REST... ---");
    List<Device> initialDevices = new ArrayList<>();
    Device dev1 = new Device();
    // dev1.setId(101);
    dev1.setName("Delivery Van");
    dev1.setStatus("offline");
    initialDevices.add(dev1);
    stateManager.loadInitialDevices(initialDevices);
    System.out.println("Initial devices loaded: " + stateManager.getAllDevices());
    System.out.println();

    // Check the latest position (should be empty right now)
    System.out.println(
        "Latest position for device 101: "
            + stateManager.getLatestPositionForDevice(101).orElse(null));
    System.out.println();

    // 3. SIMULATE LIVE UPDATE (from WebSocket)
    System.out.println("--- WebSocket message received: New Position ---");
    Position newPosition = new Position();
    // newPosition.setId(5001);
    newPosition.setDeviceId(101L);
    newPosition.setLatitude(BigDecimal.valueOf(49.4093));
    newPosition.setLongitude(BigDecimal.valueOf(8.6947));
    newPosition.setSpeed(BigDecimal.valueOf(25.5));
    // newPosition.setServerTime(ZonedDateTime.now());

    // This is the core logic: update the state with the new position
    stateManager.addOrUpdatePosition(newPosition);

    // 4. CHECK THE UPDATED STATE
    System.out.println("State after WebSocket update:");

    // The device object is now updated automatically
    Optional<Device> updatedDevice = stateManager.getDeviceById(101);
    updatedDevice.ifPresent(
        d -> {
          System.out.println("Updated Device Details: " + d);
          System.out.println("  - Status: " + d.getStatus());
          System.out.println("  - Last Update Time: " + d.getLastUpdate());
          System.out.println("  - Current Position ID: " + d.getPositionId());
        });

    // Now we can easily get the full position object for the device
    Optional<Position> latestPosition = stateManager.getLatestPositionForDevice(101);
    System.out.println("\nResolved latest position for device 101: " + latestPosition.orElse(null));
  }
}
