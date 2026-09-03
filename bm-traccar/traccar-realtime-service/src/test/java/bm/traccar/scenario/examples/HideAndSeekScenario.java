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
package bm.traccar.scenario.examples;

import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.User;
import bm.traccar.scenario.Scenario;
import java.util.ArrayList;
import java.util.List;

/**
 * Example scenario demonstrating multiple trackers with different users.
 *
 * <p>This scenario simulates a hide-and-seek game with:
 *
 * <ul>
 *   <li>A "runner" device (being tracked)
 *   <li>Multiple "chaser" devices (doing the tracking)
 *   <li>Different users with different permissions
 *   <li>Real-time position updates for all devices
 * </ul>
 */
public class HideAndSeekScenario extends Scenario {

  private User managerUser;
  private User hideUser;
  private User seekUser;

  private Device runnerDevice;
  private Device chaser1Device;
  private Device chaser2Device;

  private final List<User> createdUsers = new ArrayList<>();
  private final List<Device> createdDevices = new ArrayList<>();

  @Override
  public String getName() {
    return "Hide and Seek Scenario";
  }

  @Override
  public void initScenario() throws Exception {
    logger.info("Creating hide-and-seek scenario with multiple users and devices...");

    long timestamp = System.currentTimeMillis();

    // Create manager user (can see all devices)
    managerUser = createUser("manager-" + timestamp, "manager@example.com", true);
    managerUser.setDeviceLimit(5);
    managerUser = context.getApi().getUsersApi().updateUser(managerUser.getId(), managerUser);

    // Create hide user (runner)
    hideUser = createUser("hide-" + timestamp, "hide@example.com", false);
    hideUser.setDeviceLimit(1);
    hideUser = context.getApi().getUsersApi().updateUser(hideUser.getId(), hideUser);

    // Create seek user (chasers)
    seekUser = createUser("seek-" + timestamp, "seek@example.com", false);
    seekUser.setDeviceLimit(2);
    seekUser = context.getApi().getUsersApi().updateUser(seekUser.getId(), seekUser);

    logger.info("Created users: manager, hide, seek");

    // Create devices
    runnerDevice = createDevice("runner-" + timestamp, "runner-id-" + timestamp);
    chaser1Device = createDevice("chaser1-" + timestamp, "chaser1-id-" + timestamp);
    chaser2Device = createDevice("chaser2-" + timestamp, "chaser2-id-" + timestamp);

    logger.info("Created devices: runner, chaser1, chaser2");

    // TODO: Assign devices to users
    // TODO: Register trackers
    // TODO: Set up geofences or other tracking rules

    logger.info("Hide-and-seek scenario initialized");
  }

  @Override
  public void runScenario() throws Exception {
    logger.info("Starting hide-and-seek game...");

    // TODO: Start GPS players for all devices
    // Example:
    // GpsPlayer runnerPlayer = new GpsPlayer(runnerDevice);
    // GpsPlayer chaser1Player = new GpsPlayer(chaser1Device);
    // GpsPlayer chaser2Player = new GpsPlayer(chaser2Device);

    // TODO: Simulate movement
    // runnerPlayer.moveTo(lat1, lon1);
    // chaser1Player.moveTo(lat2, lon2);
    // chaser2Player.moveTo(lat3, lon3);

    // TODO: Verify via RealTimeController
    // - Check that manager can see all devices
    // - Check that hideUser only sees runner
    // - Check that seekUser only sees chasers

    Thread.sleep(2000); // Simulate game time

    logger.info("Hide-and-seek game completed");
  }

  @Override
  public void shutdownScenario() throws Exception {
    logger.info("Cleaning up hide-and-seek scenario...");

    // Delete devices
    for (Device device : createdDevices) {
      try {
        context.getApi().getDevicesApi().deleteDevice(device.getId());
        logger.info("Deleted device: {}", device.getName());
      } catch (Exception e) {
        logger.warn("Failed to delete device {}: {}", device.getName(), e.getMessage());
      }
    }

    // Delete users
    for (User user : createdUsers) {
      try {
        context.getApi().getUsersApi().deleteUser(user.getId());
        logger.info("Deleted user: {}", user.getName());
      } catch (Exception e) {
        logger.warn("Failed to delete user {}: {}", user.getName(), e.getMessage());
      }
    }

    logger.info("Hide-and-seek scenario cleanup complete");
  }

  // Helper methods

  private User createUser(String name, String email, boolean isAdmin) throws Exception {
    User user = new User();
    user.setName(name);
    user.setEmail(email);
    user.setPassword("test-password");
    user.setAdministrator(isAdmin);

    user = context.getApi().getUsersApi().createUser(user);
    createdUsers.add(user);
    logger.info("Created user: {} (id={})", user.getName(), user.getId());
    return user;
  }

  private Device createDevice(String name, String uniqueId) throws Exception {
    Device device = new Device();
    device.setName(name);
    device.setUniqueId(uniqueId);
    device.setModel("test-model");

    device = context.getApi().getDevicesApi().createDevice(device);
    createdDevices.add(device);
    logger.info("Created device: {} (id={})", device.getName(), device.getId());
    return device;
  }
}
