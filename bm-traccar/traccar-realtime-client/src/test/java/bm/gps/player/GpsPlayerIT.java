package bm.gps.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bm.gps.tracker.BaseGpsTrackerIT;
import bm.gps.tracker.TrackerOsmAnd;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for gps-player using the full scenario setup from BaseRealTimeClientTest for a
 * full end-to-end test of the player component in the real-time client context.
 *
 * <p>This test demonstrates parsing GPX files and sending the extracted GPS messages through the
 * real-time client to verify that the positions are correctly processed and stored.
 */
class GpsPlayerIT extends BaseGpsTrackerIT {

  private static final Logger logger = LoggerFactory.getLogger(GpsPlayerIT.class);

  @Test
  void loadSingleGpxAndSendWithSingleTracker() throws Exception {
    logger.info("\t********** loadSingleGpxAndSendWithSingleTracker() **********");

    // for remote verification
    Device device = lookupDevice(runnerTracker);
    long deviceId = device.getId();

    PlayerOsmAnd player = createPlayer(runnerTracker, "gpx/RGB-BUELL-SE-080607.gpx");
    // should be set automatically
    // player.setLastSentPositionId(device.getPositionId());

    // do not play all messages at once. Only start playback.
    assertThat(player.playOsmAndTrack()).isTrue();

    Position pos = awaitPositionForDevice(deviceId, 8000L);
    assertNotNull(pos, "Position not available in controller after GPX sample processing");
    assertThat(pos.getDeviceId()).isEqualTo(deviceId);
    assertThat(pos.getId()).as("Latest position should have a persisted id").isGreaterThan(0L);

    // sleep(120000); // almost 2 minutes from first to second message in the GPX file

    // tbc
  }

  @Test
  void loadFourGpxAndSendWithFourTrackers() throws Exception {
    logger.info("\t********** loadFourGpxAndSendWithFourTrackers() **********");

    // set sample size to 100 or more to play around in the Tracking System Frontend
    List<PlayerOsmAnd> allPlayers =
        List.of(
            createPlayer(runnerTracker, "gpx/RGB-BUELL-NE-070528.gpx"), // 161
            createPlayer(chaser1Tracker, "gpx/RGB-BUELL-NE-070813.gpx"), // 490
            createPlayer(chaser2Tracker, "gpx/RGB-BUELL-NE-070902.gpx"), // 652
            createPlayer(mobileTracker, "gpx/RGB-BUELL-NE-080524.gpx")); // 988

    // do not play all messages at once. Only start playback.
    for (PlayerOsmAnd player : allPlayers) {
      // Device device = lookupDevice(player.getTracker());
      // long deviceId = device.getId();
      // player.setLastSentPositionId(device.getPositionId());

      assertThat(player.playOsmAndTrack()).isTrue();
    }

    // sleep(600000);

    // tbc

    // rest is verification and assertions ----------------
    //    for (PlayerOsmAnd player : allPlayers) {

    //      Device device = lookupDevice(player.getTracker());
    //      long deviceId = device.getId();
    //      // player.setLastSentPositionId(device.getPositionId());
    //
    //      Position pos = awaitPositionForDevice(deviceId, 12000L);
    //      assertNotNull(
    //          pos,
    //          "Position not available in controller after GPX processing for uniqueId "
    //              + player.getUniqueId());
    //      assertThat(pos.getDeviceId()).isEqualTo(deviceId);
    //      assertThat(pos.getId()).as("Latest position should have a persisted
    // id").isGreaterThan(0L);

    //      java.lang.AssertionError: Expecting actual: 6501L not to be equal to: 6501L
    //      if (player.getLastSentPositionId() != null && player.getLastSentPositionId() > 0L) {
    //        assertThat(pos.getId()).isNotEqualTo(player.getLastSentPositionId());
    //      }
    //    }
  }

  private Position awaitPositionForDevice(long deviceId, long timeoutMs)
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

  private PlayerOsmAnd createPlayer(TrackerOsmAnd tracker, String resourcePath) {
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
   * A Player does not need the deviceId to play messages.
   *
   * <p>The deviceId is only needed to verify the position updates in the controller. This way the
   * Player is decoupled from the internal database IDs and can work with the semantic uniqueIds
   * that are relevant to the scenario.
   *
   * <p>Helper method to find the Device associated with a Tracker, throwing an AssertionError if
   * not found. This ensures that the test fails if the scenario setup is missing the expected
   * device for the tracker.
   */
  private Device lookupDevice(TrackerOsmAnd tracker) throws AssertionError {
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
   * Test helper method to get a test resource file from the classpath
   *
   * @param resourcePath the relative path to the resource
   * @return the File object pointing to the resource
   */
  private File getResourceFile(String resourcePath) {
    URL resource = getClass().getClassLoader().getResource(resourcePath);
    assertThat(resource)
        .as("Resource '%s' should exist in test classpath", resourcePath)
        .isNotNull();
    return new File(resource.getFile());
  }
}
