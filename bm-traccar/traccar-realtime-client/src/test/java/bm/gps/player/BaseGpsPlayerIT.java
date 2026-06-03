package bm.gps.player;

import static org.assertj.core.api.Assertions.assertThat;

import bm.gps.tracker.BaseGpsTrackerIT;
import bm.gps.tracker.TrackerOsmAnd;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import java.io.File;
import java.net.URL;
import java.util.Optional;

/**
 * Abstract base class for GPS player integration tests.
 *
 * <p>Provides common functionality and setup for integration tests related to GPS player components
 * in the real-time client context.
 */
public abstract class BaseGpsPlayerIT extends BaseGpsTrackerIT {

  /**
   * Waits for a position to be available for the specified device.
   *
   * @param deviceId the ID of the device
   * @param timeoutMs the timeout in milliseconds
   * @return the latest position for the device, or null if not available within timeout
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  protected Position awaitPositionForDevice(long deviceId, long timeoutMs)
      throws InterruptedException {
    long start = System.currentTimeMillis();
    while ((System.currentTimeMillis() - start) < timeoutMs) {
      Optional<Position> posOpt = controller.getLatestPositionForDevice(deviceId);
      if (posOpt.isPresent()) {
        return posOpt.get();
      }
      sleep(100);
    }
    return null;
  }

  /**
   * Creates a PlayerOsmAnd with the specified tracker and GPX resource.
   *
   * @param tracker the tracker to associate with the player
   * @param resourcePath the relative path to the GPX resource file
   * @return a new PlayerOsmAnd instance with the track loaded
   */
  protected PlayerOsmAnd createPlayer(TrackerOsmAnd tracker, String resourcePath) {
    assertThat(tracker).as("Scenario tracker must be available").isNotNull();
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

  /**
   * Finds the Device associated with a Tracker.
   *
   * <p>A Player does not need the deviceId to play messages. The deviceId is only needed to verify
   * the position updates in the controller. This way the Player is decoupled from the internal
   * database IDs and can work with the semantic uniqueIds that are relevant to the scenario.
   *
   * @param tracker the tracker to look up
   * @return the Device associated with the tracker
   * @throws AssertionError if the device is not found
   */
  protected Device lookupDevice(TrackerOsmAnd tracker) throws AssertionError {
    Device device =
        controller
            .getDeviceByUniqueId(tracker.getUniqueId())
            .orElseThrow(
                () ->
                    new AssertionError(
                        "Missing scenario device for uniqueId " + tracker.getUniqueId()));
    return device;
  }

  /**
   * Gets a test resource file from the classpath.
   *
   * @param resourcePath the relative path to the resource
   * @return the File object pointing to the resource
   */
  protected File getResourceFile(String resourcePath) {
    URL resource = getClass().getClassLoader().getResource(resourcePath);
    assertThat(resource)
        .as("Resource '%s' should exist in test classpath", resourcePath)
        .isNotNull();
    return new File(resource.getFile());
  }
}
