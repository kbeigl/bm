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
package bm.gps.gpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import bm.gps.player.PlayerOsmAnd;
import bm.gps.tracker.TrackerOsmAnd;
import bm.gps.tracker.TrackerRegistration;
import org.junit.jupiter.api.Test;

class Gpx2OsmandLoaderTest {

  @Test
  void onPlayerFinishedRemovesPlayerAndUnregistersTracker() {
    Gpx2OsmandParser parser = mock(Gpx2OsmandParser.class);
    TrackerRegistration trackerRegistration = mock(TrackerRegistration.class);

    Gpx2OsmandLoader loader =
        new Gpx2OsmandLoader(
            parser, trackerRegistration, "target/test-send", ".*\\.gpx", 1000L, 0L, null);

    PlayerOsmAnd player1 = new PlayerOsmAnd();
    player1.setTracker(new TrackerOsmAnd("tracker-1", "http://localhost:5055"));
    loader.getPlayers().put("tracker-1", player1);

    PlayerOsmAnd player2 = new PlayerOsmAnd();
    player2.setTracker(new TrackerOsmAnd("tracker-2", "http://localhost:5055"));
    loader.getPlayers().put("tracker-2", player2);

    assertEquals(2, loader.getPlayers().size());

    loader.onPlayerFinished("tracker-1");

    assertEquals(1, loader.getPlayers().size());
    assertFalse(loader.getPlayers().containsKey("tracker-1"));
    assertTrue(loader.getPlayers().containsKey("tracker-2"));
    verify(trackerRegistration).unregisterTracker("tracker-1");

    loader.onPlayerFinished("tracker-2");

    assertEquals(0, loader.getPlayers().size());
    assertFalse(loader.getPlayers().containsKey("tracker-2"));
    verify(trackerRegistration).unregisterTracker("tracker-2");
  }
}
