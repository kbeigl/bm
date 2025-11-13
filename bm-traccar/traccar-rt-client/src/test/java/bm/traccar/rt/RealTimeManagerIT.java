package bm.traccar.rt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bm.traccar.api.Api;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import bm.traccar.generated.model.dto.User;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

class RealTimeManagerIT extends BaseRealTimeTest {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeManagerIT.class);

  @Autowired protected Api api;

  // no dependency injection
  private RealTimeManager stateManager = RealTimeManager.getInstance();

  @Test
  void setupTest() {
    // 1. SIMULATE LOGIN (from REST API) into scenario
    logger.info("--- Logging in... ---");
    // we have created an admin with password in the scenario.setup
    assertNotNull(scenario.admin, "admin is not set");
    assertNotNull(scenario.admin.getPassword(), "admin.password is not set");
    api.setBasicAuth(scenario.admin.getEmail(), scenario.admin.getPassword());
    // save stateManager user!?
    // api.getBasicAuth().getUsername() / .getPassword();
    // move down and only set User after setup was successful ?
    stateManager.setLoginUser(scenario.admin);
    logger.info("stateManager: " + stateManager.getCurrentUser().map(User::getName).orElse("None"));

    // 2. SIMULATE INITIAL DATA LOAD (from REST API)
    logger.info("--- Loading initial users from server...   ---");
    List<User> initialUsers = new ArrayList<>();
    initialUsers = api.getUsersApi().getUsers(null);
    initialUsers.forEach(u -> logger.info("User.id{}: {}", u.getId(), u.getEmail()));
    stateManager.loadInitialUsers(initialUsers);
    logger.debug("Initial users loaded: " + stateManager.getAllUsers());

    List<Long> initialUserIds = initialUsers.stream().map(User::getId).sorted().toList();
    List<Long> stateManagerUserIds =
        stateManager.getAllUsers().stream().map(User::getId).sorted().toList();
    assertEquals(initialUserIds, stateManagerUserIds, "User lists do not match after initial load");

    logger.info("--- Loading initial devices from server... ---");
    List<Device> initialDevices = new ArrayList<>();
    initialDevices = api.getDevicesApi().getDevices(null);
    initialDevices.forEach(u -> logger.info("Device.id{}: {}", u.getId(), u.getName()));
    stateManager.loadInitialDevices(initialDevices);
    logger.debug("Initial devices loaded: " + stateManager.getAllDevices());

    List<Long> initialDeviceIds = initialDevices.stream().map(Device::getId).sorted().toList();
    List<Long> stateManagerDeviceIds =
        stateManager.getAllDevices().stream().map(Device::getId).sorted().toList();
    assertEquals(
        initialDeviceIds, stateManagerDeviceIds, "Device lists do not match after initial load");

    // 3. SIMULATE LIVE UPDATE (from WebSocket for example)
    logger.info("--- WebSocket message received: New Position ---");
    Position newPosition = new Position();
    newPosition.setId(5001l);
    // Associate with an existing device
    long deviceId = initialDevices.get(0).getId();
    newPosition.setDeviceId(deviceId);
    newPosition.setLatitude(BigDecimal.valueOf(49.4093d));
    newPosition.setLongitude(BigDecimal.valueOf(8.6947d));
    newPosition.setSpeed(BigDecimal.valueOf(25.5d));
    // newPosition.setServerTime(ZonedDateTime.now());

    // 4. CHECK THE UPDATED STATE
    // This is the core logic: update the state with the new position
    stateManager.addOrUpdatePosition(newPosition);

    logger.info("State after WebSocket update:");
    // The device object is now updated automatically
    Optional<Device> updatedDevice = stateManager.getDeviceById(deviceId);
    updatedDevice.ifPresent(
        d -> {
          logger.info("Updated Device Details: " + d);
          logger.info("  - Status: " + d.getStatus());
          logger.info("  - Current Position ID: " + d.getPositionId());
        });

    // get full position for device
    Optional<Position> latestPosition = stateManager.getLatestPositionForDevice(deviceId);
    logger.info("latest position for device.id{}: {}", deviceId, latestPosition.orElse(null));
  }
}
