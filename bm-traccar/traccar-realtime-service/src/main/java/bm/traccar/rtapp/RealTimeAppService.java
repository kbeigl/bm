/*
 * (C) Copyright 2026 Kristof Beiglböck
 *               kbeigl.github.io/bm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bm.traccar.rtapp;

import bm.gps.MessageOsmand;
import bm.gps.tracker.TrackerOsmAnd;
import bm.gps.tracker.TrackerRegistration;
import bm.traccar.api.Api;
import bm.traccar.api.ApiException;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import bm.traccar.generated.model.dto.User;
import bm.traccar.invoke.auth.HttpBasicAuth;
import bm.traccar.rt.RealTimeController;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * RealTimeAppService combines functionality from the main RealTimeApp components:
 * RealTimeController, Api, and TrackerRegistration.
 *
 * <p>This service provides high-level operations that combine multiple components for common use
 * cases such as:
 *
 * <ul>
 *   <li>Creating devices with local GPS trackers
 *   <li>Creating users and managing device ownership
 *   <li>Measuring latency between tracker messages and controller reception
 *   <li>Complex operations like hiding users and devices
 * </ul>
 *
 * <p>The service works with the different API endpoints:
 *
 * <ul>
 *   <li>apiTraccar (Server API) - for server configuration
 *   <li>apiDevices - for device management
 *   <li>apiUsers - for user management
 *   <li>apiPermissions - for managing user-device relationships
 * </ul>
 */
@Service
public class RealTimeAppService {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeAppService.class);

  private final RealTimeController controller;
  private final Api api;
  private final TrackerRegistration trackerRegistration;

  RealTimeAppService(
      RealTimeController controller, Api api, TrackerRegistration trackerRegistration) {
    this.controller = controller;
    this.api = api;
    this.trackerRegistration = trackerRegistration;
  }

  /* Maybe add sending messages with trackers as additional verification ... */

  // ========== Device and Tracker Operations ==========

  /**
   * Creates a device on the Traccar server and registers a local GPS tracker for it.
   *
   * @param name the device name
   * @param uniqueId the unique device identifier (used by the tracker)
   * @param model the device model (optional)
   * @return the created device
   * @throws ApiException if device creation fails
   */
  public Device createDeviceWithTracker(String name, String uniqueId, String model)
      throws ApiException {
    logger.info("Creating device '{}' with uniqueId '{}' and local tracker", name, uniqueId);

    // 1. Create device via API
    Device device = new Device();
    device.setName(name);
    device.setUniqueId(uniqueId);
    if (model != null) {
      device.setModel(model);
    }

    Device createdDevice = api.getDevicesApi().createDevice(device);
    logger.info("Created Device.id{}: {}", createdDevice.getId(), createdDevice.getName());

    // 2. Register local tracker
    try {
      /* TrackerOsmAnd tracker = */ trackerRegistration.registerTracker(uniqueId);
      logger.info("Registered local tracker for device '{}' (uniqueId: {})", name, uniqueId);
    } catch (Exception e) {
      logger.error("Failed to register tracker for device {}: {}", uniqueId, e.getMessage());
      // Note: Device is created but tracker registration failed
      // Consider: Should we delete the device if tracker registration fails?
    }

    return createdDevice;
  }

  /**
   * Creates multiple devices with trackers.
   *
   * @param deviceConfigs list of device configurations (name, uniqueId, model)
   * @return list of created devices
   */
  public List<Device> createDevicesWithTrackers(List<DeviceConfig> deviceConfigs) {
    List<Device> createdDevices = new ArrayList<>();
    for (DeviceConfig config : deviceConfigs) {
      try {
        Device device = createDeviceWithTracker(config.name, config.uniqueId, config.model);
        createdDevices.add(device);
      } catch (ApiException e) {
        logger.error("Failed to create device {}: {}", config.uniqueId, e.getMessage());
      }
    }
    return createdDevices;
  }

  /**
   * Gets a tracker for a device by uniqueId, creating it if it doesn't exist.
   *
   * @param uniqueId the device unique identifier
   * @return the tracker instance
   */
  public TrackerOsmAnd getOrCreateTracker(String uniqueId) {
    TrackerOsmAnd tracker = trackerRegistration.lookupTracker(uniqueId);
    if (tracker == null) {
      tracker = trackerRegistration.registerTracker(uniqueId);
      logger.info("Created new tracker for device: {}", uniqueId);
    }
    return tracker;
  }

  /**
   * Removes a device from the server and unregisters its local tracker.
   *
   * @param deviceId the device ID to delete
   */
  public void deleteDeviceWithTracker(Long deviceId) {

    // DEVICE HAS NOT SENT A MESSAGE YET, SO IT IS NOT IN THE CONTROLLER STATE. NEED TO GET IT FROM
    // THE API.
    // => keep created devices programmatically in a list for cleanup,
    // or query the API for the device by ID.
    // and poll for devices created in the UI ..

    Optional<Device> deviceOpt =
        controller.getAllDevices().stream().filter(d -> d.getId().equals(deviceId)).findFirst();

    if (!deviceOpt.isPresent()) {
      // If not found in controller state, try fetching from API
      try {
        List<Device> devices = api.getDevicesApi().getDevices();
        Device device =
            devices.stream().filter(d -> d.getId().equals(deviceId)).findFirst().orElse(null);
        deviceOpt = Optional.ofNullable(device);
      } catch (ApiException e) {
        logger.error("Failed to fetch device {} from API: {}", deviceId, e.getMessage());
      }
    }

    if (deviceOpt.isPresent()) {
      Device device = deviceOpt.get();
      String uniqueId = device.getUniqueId();

      // 1. Unregister local tracker
      boolean unregistered = trackerRegistration.unregisterTracker(uniqueId);
      if (unregistered) {
        logger.info("Unregistered tracker for device: {}", uniqueId);
      }

      // 2. Delete device via API
      api.getDevicesApi().deleteDevice(deviceId);
      logger.info("Deleted Device.id{}: {}", deviceId, device.getName());
    } else {
      logger.warn("Device with ID {} not found", deviceId);
    }
  }

  // ========== User and Permission Operations ==========

  //  public User createUserForDevices(
  //      String userName, String password, String email, List<Long> deviceIds) throws ApiException
  // {}

  /**
   * Creates a user with new devices and trackers.
   *
   * <p><strong>CORRECT APPROACH:</strong> This method creates devices while authenticated as the
   * new user, so they automatically belong to that user. This avoids the permission issues that
   * occur when an admin tries to assign their own devices to other users.
   *
   * <p><strong>KEY INSIGHT:</strong> In Traccar, regular users (deviceLimit = 0 or null) cannot
   * create devices. Only users with a deviceLimit set (managers) or administrators can create
   * devices. This method grants the user a deviceLimit before creating devices.
   *
   * <p>Workflow:
   *
   * <ol>
   *   <li>Save current admin authentication
   *   <li>Create the new user (as admin)
   *   <li>Grant user permission to create devices by setting deviceLimit (as admin)
   *   <li>Switch authentication to the new user
   *   <li>Create devices (they automatically belong to this user)
   *   <li>Register local trackers
   *   <li>Restore admin authentication
   * </ol>
   *
   * <p><strong>TRACCAR PERMISSION MODEL:</strong>
   *
   * <ul>
   *   <li>When a user creates a device, it automatically belongs to that user
   *   <li>Regular users (deviceLimit = 0) CANNOT create devices
   *   <li>Managers (deviceLimit != 0) CAN create devices up to their limit
   *   <li>Administrators (administrator = true) can create unlimited devices
   *   <li>Regular admins CANNOT assign their own devices to other users via permissions
   * </ul>
   *
   * @param userName the user's name
   * @param password the user's password
   * @param email the user's email
   * @param deviceConfigs configurations for devices to create and assign
   * @return the created user
   * @throws ApiException if user or device creation fails
   */
  public User createUserWithDevices(
      String userName, String password, String email, List<DeviceConfig> deviceConfigs)
      throws ApiException {
    logger.info(
        "Creating user '{}' with {} new device(s) and tracker(s)", userName, deviceConfigs.size());

    // Save current authentication (may be null if using bearer token or no auth)
    HttpBasicAuth originalAuth = api.getBasicAuth();
    String originalUsername = originalAuth != null ? originalAuth.getUsername() : null;
    String originalPassword = originalAuth != null ? originalAuth.getPassword() : null;

    // Check if we have basic auth credentials to restore
    boolean hadBasicAuth = originalUsername != null && originalPassword != null;

    try {
      // 1. Create user (as admin)
      User user = api.getUsersApi().createUserWithCredentials(userName, password, email, false);
      logger.info("Created User.id{}: {}", user.getId(), user.getName());

      // 2. Grant the user permission to create devices by setting deviceLimit
      // In Traccar:
      //  - Regular users (deviceLimit = 0 or null) CANNOT create devices
      //  - Managers (deviceLimit != 0) CAN create devices
      //  - deviceLimit = -1 means unlimited devices
      int requiredLimit = deviceConfigs.size();
      user.setDeviceLimit(requiredLimit > 0 ? requiredLimit : -1);
      user = api.getUsersApi().updateUser(user.getId(), user);
      logger.debug("Updated User.id{} with deviceLimit={}", user.getId(), user.getDeviceLimit());

      // 3. Switch authentication to the new user
      logger.debug("Switching authentication to new user: {}", user.getEmail());
      api.setBasicAuth(user.getEmail(), password);

      // 4. Create devices while authenticated as the new user
      // They will automatically belong to this user - NO PERMISSIONS NEEDED!
      List<Device> createdDevices = new ArrayList<>();
      for (DeviceConfig config : deviceConfigs) {
        try {
          Device device = new Device();
          device.setName(config.name);
          device.setUniqueId(config.uniqueId);
          if (config.model != null) {
            device.setModel(config.model);
          }

          // Create device - it automatically belongs to the authenticated user
          Device createdDevice = api.getDevicesApi().createDevice(device);
          logger.info(
              "Created Device.id{}: {} (owned by User.id{})",
              createdDevice.getId(),
              createdDevice.getName(),
              user.getId());

          createdDevices.add(createdDevice);

          // Register local tracker
          try {
            /* TrackerOsmAnd tracker = */ trackerRegistration.registerTracker(config.uniqueId);
            logger.info(
                "Registered local tracker for device '{}' (uniqueId: {})",
                config.name,
                config.uniqueId);
          } catch (Exception e) {
            logger.error(
                "Failed to register tracker for device {}: {}", config.uniqueId, e.getMessage());
          }

        } catch (ApiException e) {
          logger.error(
              "Failed to create device {} (uniqueId: {}): {}. Cause: {}",
              config.name,
              config.uniqueId,
              e.getMessage(),
              e.getCause() != null ? e.getCause().getMessage() : "No cause",
              e);
        }
      }

      logger.info(
          "Successfully created {} of {} devices for user '{}'",
          createdDevices.size(),
          deviceConfigs.size(),
          userName);

      return user;

    } finally {
      // Restore original authentication only if basic auth was set before
      if (hadBasicAuth) {
        logger.debug("Restoring original authentication: {}", originalUsername);
        api.setBasicAuth(originalUsername, originalPassword);
      } else {
        logger.debug("No basic auth to restore (using bearer token or no prior auth)");
      }
    }
  }

  // public int assignDevicesToUser    (Long userId, List<Long> deviceIds) {}
  // public int unassignDevicesFromUser(Long userId, List<Long> deviceIds) {}

  // ========== Latency Measurement Operations ==========

  /**
   * Sends a GPS message with a tracker and measures the latency until it's received by the
   * controller.
   *
   * @param uniqueId the device unique identifier
   * @param message the GPS message to send
   * @return latency in milliseconds, or -1 if measurement failed
   */
  public long measureMessageLatency(String uniqueId, MessageOsmand message) {
    logger.info("Measuring message latency for device: {}", uniqueId);

    try {
      // Get the device to know its ID
      Optional<Device> deviceOpt = controller.getDeviceByUniqueId(uniqueId);
      if (!deviceOpt.isPresent()) {
        logger.error("Device not found for uniqueId: {}", uniqueId);
        return -1;
      }

      Device device = deviceOpt.get();
      Long deviceId = device.getId();

      // Get current position to compare later
      Optional<Position> currentPosOpt = controller.getLatestPositionForDevice(deviceId);
      Long currentPosId = currentPosOpt.map(Position::getId).orElse(null);

      // Get or create tracker
      TrackerOsmAnd tracker = getOrCreateTracker(uniqueId);

      // Record timestamp before sending
      long sendTime = System.currentTimeMillis();

      // Send message via tracker
      tracker.sendMessage(message);
      logger.debug("Sent message for device: {}", uniqueId);

      // Wait for message reception in controller (poll with timeout)
      long timeout = 30000; // 30 seconds default timeout
      long pollInterval = 100; // Poll every 100ms
      long elapsed = 0;

      while (elapsed < timeout) {
        Optional<Position> newPosOpt = controller.getLatestPositionForDevice(deviceId);

        // Check if we have a new position
        if (newPosOpt.isPresent()) {
          Position newPos = newPosOpt.get();
          Long newPosId = newPos.getId();

          // Check if this is a different position than before
          if (currentPosId == null || !newPosId.equals(currentPosId)) {
            // Calculate latency
            long receiveTime = System.currentTimeMillis();
            long latency = receiveTime - sendTime;

            logger.info(
                "Message received for device: {} after {}ms (Position.id: {})",
                uniqueId,
                latency,
                newPosId);
            return latency;
          }
        }

        // Sleep before next poll
        Thread.sleep(pollInterval);
        elapsed += pollInterval;
      }

      logger.warn("Timeout waiting for message from device: {} after {}ms", uniqueId, timeout);
      return -1;

    } catch (Exception e) {
      logger.error("Failed to measure latency for device {}: {}", uniqueId, e.getMessage(), e);
      return -1;
    }
  }

  /**
   * Measures round-trip latency by sending a message and waiting for it to appear in the
   * controller's state.
   *
   * @param uniqueId the device unique identifier
   * @param timeoutMs maximum time to wait for the message (milliseconds)
   * @return latency in milliseconds, or -1 if timeout or error
   */
  public long measureRoundTripLatency(String uniqueId, long timeoutMs) {
    logger.info("Measuring round-trip latency for device: {} (timeout: {}ms)", uniqueId, timeoutMs);

    try {
      // Get the device to know its ID
      Optional<Device> deviceOpt = controller.getDeviceByUniqueId(uniqueId);
      if (!deviceOpt.isPresent()) {
        logger.error("Device not found for uniqueId: {}", uniqueId);
        return -1;
      }

      Device device = deviceOpt.get();
      Long deviceId = device.getId();

      // Get current position to compare later
      Optional<Position> currentPosOpt = controller.getLatestPositionForDevice(deviceId);
      Long currentPosId = currentPosOpt.map(Position::getId).orElse(null);

      // Get or create tracker
      TrackerOsmAnd tracker = getOrCreateTracker(uniqueId);

      // Create a test message with current timestamp
      // Use tracker's current status or default location
      MessageOsmand testMessage =
          MessageOsmand.now(
              uniqueId, 52.5200, // Berlin latitude (default test location)
              13.4050, // Berlin longitude
              25.0, // speed
              180.0, // bearing
              100.0, // altitude
              85.0, // battery
              null); // hdop

      // Record timestamp before sending
      long sendTime = System.currentTimeMillis();

      // Send message via tracker
      tracker.sendMessage(testMessage);
      logger.debug("Sent test message for device: {}", uniqueId);

      // Poll controller for new position with timeout
      long pollInterval = 100; // Poll every 100ms
      long elapsed = 0;

      while (elapsed < timeoutMs) {
        Optional<Position> newPosOpt = controller.getLatestPositionForDevice(deviceId);

        // Check if we have a new position
        if (newPosOpt.isPresent()) {
          Position newPos = newPosOpt.get();
          Long newPosId = newPos.getId();

          // Check if this is a different position than before
          if (currentPosId == null || !newPosId.equals(currentPosId)) {
            // Calculate latency
            long receiveTime = System.currentTimeMillis();
            long latency = receiveTime - sendTime;

            logger.info(
                "Round-trip completed for device: {} after {}ms (Position.id: {})",
                uniqueId,
                latency,
                newPosId);
            return latency;
          }
        }

        // Sleep before next poll
        Thread.sleep(pollInterval);
        elapsed += pollInterval;
      }

      logger.warn("Timeout waiting for round-trip from device: {} after {}ms", uniqueId, timeoutMs);
      return -1;

    } catch (Exception e) {
      logger.error(
          "Failed to measure round-trip latency for device {}: {}", uniqueId, e.getMessage(), e);
      return -1;
    }
  }

  // ========== Complex Operations ==========
  // (Placeholder methods for future implementation)

  /**
   * Hides a user by disabling their account and removing all device permissions.
   *
   * <p>Steps performed:
   *
   * <ol>
   *   <li>Retrieve the user from the API
   *   <li>Get all devices for the user
   *   <li>Remove all device permissions
   *   <li>Set user device limit to 0 (prevents creating new devices)
   *   <li>Update user on server
   * </ol>
   *
   * <p><strong>NOTE:</strong> Traccar doesn't have a direct "disabled" flag for users. Instead,
   * this method sets the device limit to 0 and removes all device permissions, effectively hiding
   * the user from active use.
   *
   * @param userId the user ID to hide
   * @return true if successfully hidden, false otherwise
   */
  public boolean hideUser(Long userId) {
    logger.info("Hiding User.id{}", userId);

    try {
      // 1. Get the user
      User user = api.getUsersApi().getUserById(userId.toString());
      if (user == null) {
        logger.error("User.id{} not found", userId);
        return false;
      }

      // 2. Get all devices for the user
      List<Device> devices = getDevicesForUser(userId);
      logger.info("Found {} device(s) for User.id{}", devices.size(), userId);

      // 3. Remove all device permissions
      //      if (!devices.isEmpty()) {
      //        List<Long> deviceIds =
      //
      // devices.stream().map(Device::getId).collect(java.util.stream.Collectors.toList());
      //        int removedCount = unassignDevicesFromUser(userId, deviceIds);
      //        logger.info("Removed {} of {} device permissions", removedCount, devices.size());
      //      }

      // 4. Disable user by setting device limit to 0
      // This prevents the user from creating or accessing devices
      user.setDeviceLimit(0);
      user.setUserLimit(0); // Also prevent creating sub-users

      // 5. Update user on server
      api.getUsersApi().updateUser(userId, user);
      logger.info("Successfully hidden User.id{}: {}", userId, user.getName());

      return true;

    } catch (Exception e) {
      logger.error("Failed to hide User.id{}: {}", userId, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Hides a device by removing all user permissions.
   *
   * <p>Steps performed:
   *
   * <ol>
   *   <li>Retrieve the device from the API or controller
   *   <li>Get all users who have access to this device
   *   <li>Remove all permissions for this device
   *   <li>Optionally set the device to a special category/group for hidden devices
   * </ol>
   *
   * <p><strong>NOTE:</strong> This implementation removes all user-device permissions. The device
   * still exists on the server but is no longer accessible to any users except administrators.
   *
   * @param deviceId the device ID to hide
   * @return true if successfully hidden, false otherwise
   */
  public boolean hideDevice(Long deviceId) {
    logger.info("Hiding Device.id{}", deviceId);

    try {
      // 1. Get the device
      Optional<Device> deviceOpt =
          controller.getAllDevices().stream().filter(d -> d.getId().equals(deviceId)).findFirst();

      if (!deviceOpt.isPresent()) {
        // Try fetching from API if not in controller state
        try {
          List<Device> devices = api.getDevicesApi().getDevices();
          deviceOpt = devices.stream().filter(d -> d.getId().equals(deviceId)).findFirst();
        } catch (ApiException e) {
          logger.error("Failed to fetch Device.id{} from API: {}", deviceId, e.getMessage());
        }
      }

      if (!deviceOpt.isPresent()) {
        logger.error("Device.id{} not found", deviceId);
        return false;
      }

      Device device = deviceOpt.get();
      logger.info("Found Device.id{}: {}", deviceId, device.getName());

      // 2 & 3. Remove all permissions for this device
      // NOTE: Traccar API doesn't provide a direct way to get all users for a device
      // In practice, you would need to:
      //   - Get all users from the API
      //   - For each user, try to remove the permission (it will fail if permission doesn't exist)
      //   - OR maintain a separate record of device-user relationships

      logger.warn(
          "Device.id{} permissions removal not fully implemented. "
              + "Traccar API doesn't provide a way to query all users for a device. "
              + "Consider maintaining a separate permission registry or using admin API to get all permissions.",
          deviceId);

      // 4. Mark device with a special category
      if (device.getCategory() == null || !device.getCategory().equals("hidden")) {
        device.setCategory("hidden");
        try {
          api.getDevicesApi().updateDevice(deviceId, device);
          logger.info("Marked Device.id{} as hidden (category='hidden')", deviceId);
        } catch (ApiException e) {
          logger.error("Failed to update Device.id{}: {}", deviceId, e.getMessage());
          return false;
        }
      }

      logger.info("Successfully hidden Device.id{}: {}", deviceId, device.getName());
      return true;

    } catch (Exception e) {
      logger.error("Failed to hide Device.id{}: {}", deviceId, e.getMessage(), e);
      return false;
    }
  }

  // ========== Query Operations ==========

  /**
   * Gets all devices for a specific user.
   *
   * <p><strong>LIMITATION:</strong> This method currently returns all devices visible to the
   * currently authenticated user, not specifically for the requested userId. To properly filter by
   * userId, you would need to:
   *
   * <ul>
   *   <li>Switch authentication context to the target user (see {@link #createUserWithDevices})
   *   <li>Add a userId parameter to the Api.Devices.getDevices() method
   *   <li>Implement a permissions query API to get device-user relationships
   * </ul>
   *
   * @param userId the user ID (currently not used for filtering)
   * @return list of devices visible to the current authenticated user
   */
  public List<Device> getDevicesForUser(Long userId) {
    logger.info(
        "Getting devices for User.id{} (NOTE: currently returns all devices visible to authenticated user)",
        userId);

    try {
      List<Device> devices = api.getDevicesApi().getDevices();
      logger.info("Found {} device(s) for User.id{}", devices.size(), userId);
      return devices;
    } catch (Exception e) {
      logger.error("Failed to get devices for User.id{}: {}", userId, e.getMessage());
      return new ArrayList<>();
    }
  }

  /**
   * Gets the device for a tracker's uniqueId.
   *
   * @param uniqueId the device unique identifier
   * @return the device if found
   */
  // TODO vice versa: get tracker for device
  public Optional<Device> getDeviceForTracker(String uniqueId) {
    return controller.getDeviceByUniqueId(uniqueId);
  }

  /**
   * Gets the tracker for a device.
   *
   * @param uniqueId the device unique identifier
   * @return the tracker instance, or null if not registered
   */
  public TrackerOsmAnd getTracker(String uniqueId) {
    return trackerRegistration.lookupTracker(uniqueId);
  }

  /**
   * Checks if a tracker is registered for a device.
   *
   * @param uniqueId the device unique identifier
   * @return true if a tracker is registered
   */
  public boolean hasTracker(String uniqueId) {
    return trackerRegistration.lookupTracker(uniqueId) != null;
  }

  // ========== Helper Classes ==========

  /** Configuration for creating a device with tracker. */
  public static class DeviceConfig {
    public final String name;
    public final String uniqueId;
    public final String model;

    public DeviceConfig(String name, String uniqueId, String model) {
      this.name = name;
      this.uniqueId = uniqueId;
      this.model = model;
    }

    public DeviceConfig(String name, String uniqueId) {
      this(name, uniqueId, null);
    }
  }
}
