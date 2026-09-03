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
package bm.scenario.mgm;

import static org.assertj.core.api.Assertions.assertThat;

import bm.gps.player.BaseGpsPlayerIT;
import bm.gps.player.PlayerOsmAnd;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for the tracking scenario with multiple users and GPS players.
 *
 * <p>This test sets up a complete scenario with four trackers playing GPX tracks simultaneously to
 * simulate a realistic tracking scenario.
 */
class ScenarioIT extends BaseGpsPlayerIT {

  private static final Logger logger = LoggerFactory.getLogger(ScenarioIT.class);
  private List<PlayerOsmAnd> allPlayers;

  // @org.junit.jupiter.api.Test
  void runPlayersForTwoMinutes() throws InterruptedException {
    logger.info("\t********** Running scenario for 2 minutes **********");
    // sleep(120000);
    sleep(10000);
    logger.info("\t********** 2 minute scenario run completed **********");
  }

  // @org.junit.jupiter.api.Test
  void runPlayersForOneMinute() throws InterruptedException {
    logger.info("\t********** Running scenario for a minute **********");
    // sleep(60000);
    sleep(10000);
    logger.info("\t********** 1 minute scenario run completed **********");
  }

  /** Initializes and starts all four GPS players once before all tests. */
  @BeforeAll
  void startAllPlayers() throws Exception {
    logger.info("\t********** Initializing and starting all four GPS players **********");

    allPlayers =
        List.of(
            createPlayer(runnerTracker, "gpx/RGB-BUELL-NE-070528.gpx"), // 161
            createPlayer(chaser1Tracker, "gpx/RGB-BUELL-NE-070813.gpx"), // 490
            createPlayer(chaser2Tracker, "gpx/RGB-BUELL-NE-070902.gpx"), // 652
            createPlayer(mobileTracker, "gpx/RGB-BUELL-NE-080524.gpx")); // 988

    // Start playback for all players
    for (PlayerOsmAnd player : allPlayers) {
      assertThat(player.playOsmAndTrack()).isTrue();
      logger.info("Started playback for tracker: {}", player.getTracker().getUniqueId());
    }

    logger.info("\t********** All four players are now playing **********");
  }

  /**
   * Gets all the active players in the scenario.
   *
   * @return the list of all players
   */
  protected List<PlayerOsmAnd> getAllPlayers() {
    return allPlayers;
  }

  @AfterAll
  void stopAllPlayers() {
    if (allPlayers == null || allPlayers.isEmpty()) {
      return;
    }

    logger.info("\t********** Stopping all GPS players gracefully **********");
    for (PlayerOsmAnd player : allPlayers) {
      player.stopOsmAndTrack();
      logger.info("Stopped playback for tracker: {}", player.getTracker().getUniqueId());
    }
  }
}
