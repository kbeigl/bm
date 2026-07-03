package bm.traccar.scenario.examples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.gps.player.PlayerOsmAnd;
import bm.gps.tracker.TrackerOsmAnd;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.scenario.Scenario;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

  // keep track of all created resources for cleanup
  // TODO app should always keep server and controller Devices in sync
  private final List<Object> resourcesFromCode = new ArrayList<>();

  @Override
  public String getName() {
    return "Simplest Tracker Scenario";
  }

  @Override
  public void initScenario() throws Exception {

    logger.info("Creating test device as {}", context.getApi().whoIsAuthenticated());
    testDevice =
        context
            .getAppService()
            .createDeviceWithTracker("test-device", "test-uniqueid", "test-model");

    logger.info("context.getRealtimeAdmin(): {}", context.getRealtimeAdmin());
    logger.info(
        "context.getRealTimeController().getCurrentUser(): {}",
        context.getRealTimeController().getCurrentUser());

    assertNotNull(testDevice);
    assertEquals("test-device", testDevice.getName());
    assertTrue(context.getAppService().hasTracker("test-uniqueid"));

    resourcesFromCode.add(testDevice);
    logger.info("Created test device: {} (id={})", testDevice.getName(), testDevice.getId());
    // The tracker is automatically registered by createDeviceWithTracker()
    logger.info("Tracker registered for device: {}", testDevice.getUniqueId());

    logger.info("Scenario initialization complete");
  }

  @Override
  public void runScenario() throws Exception {
    logger.info("Running scenario logic...");

    // get tracker of testDevice
    if (!context.getAppService().hasTracker(testDevice.getUniqueId())) {
      throw new IllegalStateException(
          "Tracker for device "
              + testDevice.getName()
              + " (uniqueId="
              + testDevice.getUniqueId()
              + ") is not registered");
    }

    TrackerOsmAnd testTracker =
        context.getTrackerRegistration().lookupTracker(testDevice.getUniqueId());
    PlayerOsmAnd player = createPlayer(testTracker, "gpx/HD-KTM-260725.gpx");
    assertThat(player.playOsmAndTrack()).isTrue();

    // HD-KTM-260725.gpx
    // player.sendPosition(latitude, longitude);

    // TODO: Verify position updates via RealTimeController
    // Example:
    // Position lastPosition = context.getRealTimeController().getLastPosition(testDevice.getId());
    // assertNotNull(lastPosition);

    // Simulate some delay for real-time processing
    Thread.sleep(5000);
    logger.info("Scenario execution complete");
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

    // Clean up in reverse order
    if (testDevice != null) {
      // Use the RealTimeAppService from context to delete device and tracker
      context.getAppService().deleteDeviceWithTracker(testDevice.getId());
      logger.info("Deleted test device and tracker: {}", testDevice.getName());
    }

    logger.info("Scenario cleanup complete");
  }
}
