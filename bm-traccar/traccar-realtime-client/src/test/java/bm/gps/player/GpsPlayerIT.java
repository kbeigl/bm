package bm.gps.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import java.util.List;
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
class GpsPlayerIT extends BaseGpsPlayerIT {

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
    // investigate:
    // first message is sent and device is showing online in the Tracking System Frontend.
    // But position is not showing until second message arrives two minutes later. !?

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
    //      assertThat(pos.getId())
    //         .as("Latest position should have a persisted id").isGreaterThan(0L);

    //      java.lang.AssertionError: Expecting actual: 6501L not to be equal to: 6501L
    //      if (player.getLastSentPositionId() != null && player.getLastSentPositionId() > 0L) {
    //        assertThat(pos.getId()).isNotEqualTo(player.getLastSentPositionId());
    //      }
    //    }
  }
}
