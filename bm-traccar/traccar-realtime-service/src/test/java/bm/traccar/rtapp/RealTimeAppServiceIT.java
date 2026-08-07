package bm.traccar.rtapp;

import static org.junit.jupiter.api.Assertions.*;

import bm.gps.tracker.TrackerOsmAnd;
import bm.traccar.BaseRealTimeAppTest;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.User;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for RealTimeAppService demonstrating combined operations with devices, trackers,
 * and users.
 */
public class RealTimeAppServiceIT extends BaseRealTimeAppTest {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeAppServiceIT.class);

  @Autowired private RealTimeAppService rtService;

  @Test
  void testCreateDeviceWithTracker() throws Exception {
    logger.info("\t********** testCreateDeviceWithTracker() **********");

    String timestamp = String.valueOf(System.currentTimeMillis());
    String uniqueId = "test-mobile-001-" + timestamp;

    // create a device with tracker
    Device device = rtService.createDeviceWithTracker("Test Mobile", uniqueId, "iPhone");

    // device should be created
    assertNotNull(device);
    assertNotNull(device.getId());
    assertEquals("Test Mobile", device.getName());
    assertEquals(uniqueId, device.getUniqueId());

    // tracker should be registered
    assertTrue(rtService.hasTracker(uniqueId));
    TrackerOsmAnd tracker = rtService.getOrCreateTracker(uniqueId);
    assertNotNull(tracker);
    assertEquals(uniqueId, tracker.getUniqueId());

    // Cleanup
    rtService.deleteDeviceWithTracker(device.getId());
    assertFalse(rtService.hasTracker(uniqueId));

    // check if tracker is unregistered and removed from controller state
    // (lookup should return empty)
  }

  @Test
  void testCreateUserWithDevices() throws Exception {
    logger.info("\t********** testCreateUserWithDevices() **********");
    // Given: authenticate as admin
    logger.info("Authenticated: {}", api.getAuthentication());

    // Use unique email to avoid conflicts from previous test runs
    String timestamp = String.valueOf(System.currentTimeMillis());
    String uniqueEmail = "multidevice" + timestamp + "@example.com";
    String uniqueId1 = "mobile-001-" + timestamp;
    String uniqueId2 = "mobile-002-" + timestamp;
    String uniqueId3 = "tablet-001-" + timestamp;

    // When: create a user with new devices and trackers
    List<RealTimeAppService.DeviceConfig> deviceConfigs =
        Arrays.asList(
            new RealTimeAppService.DeviceConfig("Mobile 1", uniqueId1, "iPhone"),
            new RealTimeAppService.DeviceConfig("Mobile 2", uniqueId2, "Android"),
            new RealTimeAppService.DeviceConfig("Tablet 1", uniqueId3));

    User user =
        rtService.createUserWithDevices("multideviceuser", "pass123", uniqueEmail, deviceConfigs);

    // Then: user should be created
    assertNotNull(user);
    assertNotNull(user.getId());
    assertEquals("multideviceuser", user.getName());

    // Verify devices were created by checking via API (authenticate as the new user)
    api.setBasicAuth(user.getEmail(), "pass123");
    logger.info("Authenticated: {}", api.getAuthentication());

    List<Device> userDevices = api.getDevicesApi().getDevices();
    assertEquals(3, userDevices.size(), "User should have 3 devices");
    logger.info("✓ Verified: User has {} devices", userDevices.size());

    // And: trackers should be registered
    assertTrue(rtService.hasTracker(uniqueId1));
    assertTrue(rtService.hasTracker(uniqueId2));
    assertTrue(rtService.hasTracker(uniqueId3));

    // Delete the devices we just created
    for (Device device : userDevices) rtService.deleteDeviceWithTracker(device.getId());

    api.getUsersApi().deleteUser(user.getId());

    logger.info("✓ User with multiple devices and trackers created and cleaned up successfully");

    api.setBasicAuth(admin.getEmail(), admin.getName());
    logger.info("Set back admin authentication: {}", api.getAuthentication());
  }

  // @Test
  void testAssignAndUnassignDevices() throws Exception {
    logger.info("\t********** testAssignAndUnassignDevices() **********");

    // Given: authenticate as admin and create user and devices
    logger.info("Authenticated: {}", api.getAuthentication());

    String timestamp = String.valueOf(System.currentTimeMillis());
    String uniqueEmail = "assigntest" + timestamp + "@example.com";
    String uniqueId1 = "device-a-001-" + timestamp;
    String uniqueId2 = "device-b-001-" + timestamp;

    // Create user directly (without devices)
    // User user = api.getUsersApi().createUserWithCredentials("assigntest", "pass123", uniqueEmail,
    // false);
    User user = new User();
    user.setName("assigntest");
    user.setEmail(uniqueEmail);
    user.setPassword("pass123");
    user.setDeviceLimit(2);
    user = api.getUsersApi().createUser(user);
    logger.info(
        "Created user: {}.{} with deviceLimit {}",
        user.getName(),
        user.getId(),
        user.getDeviceLimit());

    // we can only authenticate as the user as long as we have the password
    // dont forget to pass the password to the real person (hard to get from server)
    //    api.setBasicAuth("assigntest", "pass123");
    //    logger.info("Authenticated: {}", api.whoIsAuthenticated());
    //    logger.info("isManager: {}", api.getUsersApi().isManager(user));

    Device device1 = rtService.createDeviceWithTracker("Device A", uniqueId1, "Android");
    Device device2 = rtService.createDeviceWithTracker("Device B", uniqueId2, "iPhone");

    //    // When: assign devices to user
    //    int assignedCount =
    //        rtService.assignDevicesToUser(
    //            user.getId(), Arrays.asList(device1.getId(), device2.getId()));
    //    // Then: devices should be assigned
    //    assertEquals(2, assignedCount);
    //    // When: unassign one device
    //    int unassignedCount = rtService.unassignDevicesFromUser(user.getId(),
    // List.of(device1.getId()));
    //    // Then: device should be unassigned
    //    assertEquals(1, unassignedCount);

    // Cleanup
    api.setBearerToken(virtualAdmin);

    api.getUsersApi().deleteUser(user.getId());
    rtService.deleteDeviceWithTracker(device1.getId());
    rtService.deleteDeviceWithTracker(device2.getId());

    logger.info("✓ Device assignment and unassignment tested successfully");
  }

  // @Test
  void testGetDeviceForTracker() throws Exception {
    logger.info("********** testGetDeviceForTracker() **********");

    // Given: authenticate as admin and create device with tracker
    String timestamp = String.valueOf(System.currentTimeMillis());
    String uniqueId = "lookup-001-" + timestamp;

    Device device = rtService.createDeviceWithTracker("Lookup Test", uniqueId, "Android");

    // Wait a bit for the device to appear in controller state
    Thread.sleep(500);

    // When: get device by tracker uniqueId
    var deviceOpt = rtService.getDeviceForTracker(uniqueId);

    // Then: device should be found
    assertTrue(deviceOpt.isPresent());
    assertEquals(uniqueId, deviceOpt.get().getUniqueId());

    // Cleanup
    rtService.deleteDeviceWithTracker(device.getId());

    logger.info("✓ Device lookup by tracker uniqueId successful");
  }

  // @Test
  void testCreateMultipleDevicesWithTrackers() throws Exception {
    logger.info("\n********** testCreateMultipleDevicesWithTrackers() **********");

    // Given: authenticate as admin
    controller.loginAndInitialize(admin);

    // Use unique identifiers to avoid conflicts
    String timestamp = String.valueOf(System.currentTimeMillis());
    String uniqueId1 = "fleet-001-" + timestamp;
    String uniqueId2 = "fleet-002-" + timestamp;
    String uniqueId3 = "fleet-003-" + timestamp;

    // When: create multiple devices with trackers
    List<RealTimeAppService.DeviceConfig> configs =
        Arrays.asList(
            new RealTimeAppService.DeviceConfig("Fleet 1", uniqueId1, "Truck"),
            new RealTimeAppService.DeviceConfig("Fleet 2", uniqueId2, "Van"),
            new RealTimeAppService.DeviceConfig("Fleet 3", uniqueId3, "Car"));

    List<Device> devices = rtService.createDevicesWithTrackers(configs);

    // Then: all devices should be created
    assertEquals(3, devices.size());

    // And: all trackers should be registered
    assertTrue(rtService.hasTracker(uniqueId1));
    assertTrue(rtService.hasTracker(uniqueId2));
    assertTrue(rtService.hasTracker(uniqueId3));

    // Cleanup
    for (Device device : devices) {
      rtService.deleteDeviceWithTracker(device.getId());
    }

    // Verify cleanup
    assertFalse(rtService.hasTracker(uniqueId1));
    assertFalse(rtService.hasTracker(uniqueId2));
    assertFalse(rtService.hasTracker(uniqueId3));

    logger.info("✓ Multiple devices with trackers created and cleaned up successfully");
  }
}
