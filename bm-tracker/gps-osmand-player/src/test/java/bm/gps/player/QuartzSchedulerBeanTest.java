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

  @Test
  void sendCurrentAndScheduleNextTenthMessage() {
    PlayerOsmAnd player = new PlayerOsmAnd();
    TrackerOsmAnd tracker = new TrackerOsmAnd("device-10", "http://localhost:5055");
    player.setTracker(tracker);

    java.util.List<MessageOsmand> messages = new java.util.ArrayList<>();
    for (int i = 0; i < 15; i++) {
      messages.add(
          new MessageOsmand(
              "device-10",
              48.0 + i * 0.001,
              16.0 + i * 0.001,
              (long) i,
              10.0,
              90.0,
              100.0,
              null,
              null));
    }
    player.osmandTrack = messages;
    player.nextIndex = 9; // 10th message (0-indexed 9)

    QuartzSchedulerBean scheduler =
        new QuartzSchedulerBean(player, new TriggerKey("job-10", "group-10"));

    scheduler.sendCurrentAndScheduleNext();

    assertEquals(10, player.nextIndex);
  }

  @Test
  void sendCurrentAndScheduleNextTriggersPlaybackCompletedOnLastMessage() throws Exception {
    PlayerOsmAnd player = new PlayerOsmAnd();
    TrackerOsmAnd tracker = new TrackerOsmAnd("device-last", "http://localhost:5055");
    player.setTracker(tracker);

    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    player.setOnPlaybackFinished(p -> latch.countDown());

    player.osmandTrack =
        List.of(new MessageOsmand("device-last", 48.0, 16.0, 1L, 10.0, 90.0, 100.0, null, null));
    player.nextIndex = 0;

    QuartzSchedulerBean scheduler =
        new QuartzSchedulerBean(player, new TriggerKey("job-last", "group-last"));

    scheduler.sendCurrentAndScheduleNext();

    assertEquals(1, player.nextIndex);
    assertTrue(latch.await(3, java.util.concurrent.TimeUnit.SECONDS));
  }
}
