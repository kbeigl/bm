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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.gps.MessageOsmand;
import bm.gps.player.PlayerOsmAnd;
import bm.gps.tracker.TrackerOsmAnd;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import bm.traccar.generated.model.dto.User;
import bm.traccar.rt.RealTimeController;
import bm.traccar.scenario.Scenario;
import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Most simple scenario demonstrating basic tracker registration and GPS player usage.
 *
 * <p>This scenario:
 *
 * <ul>
 *   <li>Creates a test device
 *   <li>Registers the device as a tracker
 *   <li>Simulates GPS position updates
 *   <li>Verifies real-time position updates via RealTimeController
 *   <li>Cleans up all created resources
 * </ul>
 */
public class SimplestTrackerScenario extends Scenario {

  private Device testDevice;
  //  private PlayerOsmAnd player;

  // keep track of all created resources for cleanup
  // TODO app should always keep server and controller Devices in sync
  private final List<Object> resourcesFromCode = new ArrayList<>();

  @Override
  public String getName() {
    return "Most Simple Tracker Scenario";
  }

  @Override
  public void initScenario() throws Exception {

    User realtimeAdmin = context.getRealtimeAdmin();
    logger.info(
        "realtimeAdmin: {} / {} / {}",
        realtimeAdmin.getId(),
        realtimeAdmin.getName(),
        realtimeAdmin.getEmail());

    // evaluate Optional as return value, questionable
    Optional<User> controllerUser = context.getRealTimeController().getCurrentUser();
    assertTrue(controllerUser.isPresent());
    assertEquals(realtimeAdmin, controllerUser.get());

    // ----------------------------------------------------

    logger.info("Creating test device as {}", context.getApi().getAuthentication());
    testDevice =
        context.getService().createDeviceWithTracker("test-device", "test-uniqueid", "test-model");

    assertNotNull(testDevice);
    assertEquals("test-device", testDevice.getName());
    assertTrue(context.getService().hasTracker("test-uniqueid"));

    resourcesFromCode.add(testDevice);
    logger.info("Created test device: {} (id={})", testDevice.getName(), testDevice.getId());
    // The tracker is automatically registered by createDeviceWithTracker()
    logger.info("Tracker registered for device: {}", testDevice.getUniqueId());
  }

  @Override
  public void runScenario() throws Exception {

    // get tracker of testDevice
    if (!context.getService().hasTracker(testDevice.getUniqueId())) {
      throw new IllegalStateException(
          "Tracker for device "
              + testDevice.getName()
              + " (uniqueId="
              + testDevice.getUniqueId()
              + ") is not registered");
    }

    TrackerOsmAnd testTracker =
        context.getTrackerRegistration().lookupTracker(testDevice.getUniqueId());

    // Send a position
    MessageOsmand msg =
        MessageOsmand.now(testDevice.getUniqueId(), 52.52, 13.40, 10.0, 20.0, 30.0, null, null);
    logger.info(
        "Sending test position for device {}: {}",
        testDevice.getUniqueId(),
        msg != null ? msg.toString() : "N/A");
    testTracker.sendMessage(msg);
    logger.info(
        "Message timestamp: {}",
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochSecond(msg.timestamp())));

    RealTimeController controller = context.getRealTimeController();
    controller.awaitPositionForDevice(testDevice.getUniqueId(), 10000);

    Optional<Position> lastPosition =
        controller.getLatestPositionForDevice(testDevice.getUniqueId());
    logger.info(
        "Latest position for device {}: {}",
        testDevice.getUniqueId(),
        lastPosition.isPresent() ? lastPosition.get() : "N/A");
    assertNotNull(lastPosition);
  }

  /* snatched from BaseGpsPlayerIT.java */
  protected PlayerOsmAnd createPlayer(TrackerOsmAnd tracker, String resourcePath) {
    assertThat(tracker).as("tracker must be available").isNotNull();
    File gpxFile = getResourceFile(resourcePath);
    assertThat(gpxFile).exists();

    PlayerOsmAnd player = new PlayerOsmAnd();
    player.setTracker(tracker);
    player.load(gpxFile);
    assertThat(player.getTrack())
        .as("Parsed messages should not be empty for resource '%s'", resourcePath)
        .isNotEmpty();

    return player;
  }

  protected File getResourceFile(String resourcePath) {
    URL resource = getClass().getClassLoader().getResource(resourcePath);
    assertThat(resource)
        .as("Resource '%s' should exist in test classpath", resourcePath)
        .isNotNull();
    return new File(resource.getFile());
  }

  @Override
  public void shutdownScenario() throws Exception {
    logger.info("Cleaning up scenario resources...");

    // Stop playback first so no further messages are sent to tracker routes
    //    if (player != null) {
    //      player.stopOsmAndTrack();
    //      logger.info(
    //          "Stopped player for {}", testDevice != null ? testDevice.getName() : "test-device");
    //    }

    // Clean up in reverse order
    if (testDevice != null) {
      // Use the RealTimeService from context to delete device and tracker
      context.getService().deleteDeviceWithTracker(testDevice.getId());
      logger.info("Deleted test device and tracker: {}", testDevice.getName());
    }

    logger.info("Scenario cleanup complete");
  }
}
