package bm.gps.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.gps.MessageOsmand;
import bm.gps.tracker.TrackerOsmAnd;
import bm.gps.tracker.TrackerOsmAnd.TrackerStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.quartz.TriggerKey;

class QuartzSchedulerBeanTest {

  @Test
  void sendCurrentAndScheduleNextUsesMessageSpeedWhenPresent() {
    PlayerOsmAnd player = new PlayerOsmAnd();
    TrackerOsmAnd tracker = new TrackerOsmAnd("device-with-speed", "http://localhost:5055");
    player.setTracker(tracker);
    player.osmandTrack =
        List.of(
            new MessageOsmand(
                "device-with-speed", 48.2082, 16.3738, 1L, 12.5d, 87.0d, 171.0, null, null));

    QuartzSchedulerBean scheduler =
        new QuartzSchedulerBean(player, new TriggerKey("job-with-speed", "group-with-speed"));

    scheduler.sendCurrentAndScheduleNext();

    assertEquals(12.5d, tracker.getTrackerStatus().getSpeed(), 0.0001d);
    assertEquals(87.0d, tracker.getTrackerStatus().getBearing(), 0.0001d);
    assertEquals(1, player.nextIndex);
  }

  @Test
  void sendCurrentAndScheduleNextFallsBackToLastSentSpeed() {
    PlayerOsmAnd player = new PlayerOsmAnd();
    TrackerOsmAnd tracker = new TrackerOsmAnd("device-fallback-speed", "http://localhost:5055");
    player.setTracker(tracker);

    TrackerStatus status = tracker.getTrackerStatus();
    status.setLatitude(48.2082);
    status.setLongitude(16.3738);
    status.setAltitude(171.0);
    status.setBearing(45.0d);
    status.setFixTime(OffsetDateTime.now().minusSeconds(10));
    tracker.sendTrackerStatus();

    player.osmandTrack =
        List.of(
            new MessageOsmand(
                "device-fallback-speed", 48.2091, 16.3738, 2L, null, null, 171.0, null, null));

    QuartzSchedulerBean scheduler =
        new QuartzSchedulerBean(
            player, new TriggerKey("job-fallback-speed", "group-fallback-speed"));

    scheduler.sendCurrentAndScheduleNext();

    assertNotNull(tracker.getTrackerStatus().getSpeed());
    assertTrue(tracker.getTrackerStatus().getSpeed() > 0.0d);
    assertNotNull(tracker.getTrackerStatus().getBearing());
    assertTrue(tracker.getTrackerStatus().getBearing() >= 0.0d);
    assertTrue(tracker.getTrackerStatus().getBearing() <= 360.0d);
    assertEquals(1, player.nextIndex);
  }
}
