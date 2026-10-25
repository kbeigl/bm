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
package bm.sy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.api.Api.Users.TraccarRole;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.User;
import bm.traccar.scenario.BaseScenarioTest;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the {@link ScotlandYard} scenario against a running Traccar server.
 *
 * <p>The scenario is executed phase by phase, so the game setup can be verified after {@code init}
 * and the cleanup after {@code shutdown}.
 */
public class ScotlandYardIT extends BaseScenarioTest {

  private static final int DETECTIVES = 3;

  @Test
  void adminHostsGameWithThreeDetectives() throws Exception {
    Properties props = new Properties();
    props.setProperty(ScotlandYard.PROP_DETECTIVES, String.valueOf(DETECTIVES));
    props.setProperty(ScotlandYard.PROP_ROUNDS, "3");
    // temp
    props.setProperty(ScotlandYard.PROP_ROUND_MILLIS, "50");
    ScotlandYard game = new ScotlandYard(props);

    // ---------------------------------------------------------------- init
    scenarioRunner.init(game, realTimeContext);

    assertEquals(TraccarRole.ADMIN, game.getHostRole().orElseThrow(), "test admin hosts the game");

    // users
    User manager = game.getGameManager();
    User misterX = game.getMisterX();
    User detectives = game.getDetectives();
    assertNotNull(manager);
    assertNotNull(misterX);
    assertNotNull(detectives);
    assertNotEquals(admin.getId(), manager.getId(), "admin creates a dedicated game manager");
    assertEquals(3, game.getCreatedUsers().size(), "manager, Mister X, detectives");

    assertEquals(TraccarRole.MANAGER, api.getUsersApi().getTraccarRole(manager));
    assertEquals(TraccarRole.REG_USER, api.getUsersApi().getTraccarRole(misterX));
    assertEquals(TraccarRole.REG_USER, api.getUsersApi().getTraccarRole(detectives));

    // users exist on the server
    assertEquals(manager.getName(), api.getUsersApi().getUser(manager.getId()).getName());
    assertEquals(misterX.getName(), api.getUsersApi().getUser(misterX.getId()).getName());
    assertEquals(detectives.getName(), api.getUsersApi().getUser(detectives.getId()).getName());

    // the players were created and managed by manager
    Set<Long> managed =
        api.getUsersApi().getUsers(String.valueOf(manager.getId()), null, null, null).stream()
            .map(User::getId)
            .collect(Collectors.toSet());
    assertEquals(Set.of(misterX.getId(), detectives.getId()), managed, "managed by game manager");

    // pawns
    Device xPawn = game.getMisterXpawn();
    List<Device> dPawns = game.getDetectivePawns();
    assertNotNull(xPawn);
    assertEquals(DETECTIVES, dPawns.size());
    assertEquals(1 + DETECTIVES, game.getCreatedDevices().size());
    assertTrue(xPawn.getUniqueId().contains(ScotlandYard.MISTER_X));
    for (int i = 0; i < DETECTIVES; i++) {
      assertTrue(dPawns.get(i).getUniqueId().contains("detective-" + (i + 1)));
    }

    // pawns exist on the server (owned by the players) and have a local tracker
    Set<String> xIds =
        game.getPawnsOf(misterX).stream().map(Device::getUniqueId).collect(Collectors.toSet());
    Set<String> dIds =
        game.getPawnsOf(detectives).stream().map(Device::getUniqueId).collect(Collectors.toSet());
    assertEquals(Set.of(xPawn.getUniqueId()), xIds, "Mister X owns exactly his pawn");
    assertEquals(
        dPawns.stream().map(Device::getUniqueId).collect(Collectors.toSet()),
        dIds,
        "detectives own exactly their pawns");
    assertTrue(
        api.getDevicesApi().getDevices().stream()
            .map(Device::getUniqueId)
            .noneMatch(id -> id.startsWith(ScotlandYard.PREFIX)),
        "admin does not own any pawn");
    for (Device pawn : game.getCreatedDevices()) {
      assertTrue(
          realTimeContext.getService().hasTracker(pawn.getUniqueId()),
          pawn.getUniqueId() + " has tracker");
    }

    // the host is still authenticated as admin after creating players
    assertEquals(admin.getEmail(), api.getBasicAuth().getUsername());

    // ------------------------------------------------ playGameInRole (REG_USER)
    // test regular user Mister X playing in his role
    ScotlandYard misterXGame = new ScotlandYard(props);
    realTimeContext.setRealtimeUser(misterX);
    api.setBasicAuth(misterX.getEmail(), ScotlandYard.PLAYER_PASSWORD);
    scenarioRunner.init(misterXGame, realTimeContext);

    assertEquals(TraccarRole.REG_USER, misterXGame.getHostRole().orElseThrow());
    assertEquals("Mister X", misterXGame.getPlayerRole().orElseThrow());
    assertEquals(1, misterXGame.getMyPawns().size());
    assertNotNull(misterXGame.getMisterXpawn());
    assertEquals(xPawn.getUniqueId(), misterXGame.getMisterXpawn().getUniqueId());

    // test regular user Detectives playing in their role
    ScotlandYard detectivesGame = new ScotlandYard(props);
    realTimeContext.setRealtimeUser(detectives);
    api.setBasicAuth(detectives.getEmail(), ScotlandYard.PLAYER_PASSWORD);
    scenarioRunner.init(detectivesGame, realTimeContext);

    assertEquals(TraccarRole.REG_USER, detectivesGame.getHostRole().orElseThrow());
    assertEquals("Detectives", detectivesGame.getPlayerRole().orElseThrow());
    assertEquals(DETECTIVES, detectivesGame.getMyPawns().size());
    assertEquals(DETECTIVES, detectivesGame.getDetectivePawns().size());

    // restore admin authentication for the host game run and shutdown
    realTimeContext.setRealtimeUser(admin);
    api.setBasicAuth(admin.getEmail(), admin.getName());

    // ----------------------------------------------------------------- run
    long start = System.currentTimeMillis();
    scenarioRunner.run(game);
    assertTrue(System.currentTimeMillis() - start >= 3 * 50L, "3 rounds of 50 ms");

    // ------------------------------------------------------------ shutdown
    List<String> pawnIds =
        game.getCreatedDevices().stream().map(Device::getUniqueId).collect(Collectors.toList());
    List<Long> userIds =
        game.getCreatedUsers().stream().map(User::getId).collect(Collectors.toList());

    scenarioRunner.shutdown(game);

    assertTrue(game.getCreatedUsers().isEmpty());
    assertTrue(game.getCreatedDevices().isEmpty());

    // pawns: the owners are gone, so verify the local trackers are unregistered
    for (String uniqueId : pawnIds) {
      assertFalse(realTimeContext.getService().hasTracker(uniqueId), uniqueId + " unregistered");
    }
    // users are deleted from the server (admin lists all users)
    Set<Long> remainingUsers =
        api.getUsersApi().getUsers(null, null, null, null).stream()
            .map(User::getId)
            .collect(Collectors.toSet());
    for (Long id : userIds) {
      assertFalse(remainingUsers.contains(id), "user " + id + " deleted from server");
    }
  }

  @Test
  void executeWithDefaults() throws Exception {
    Properties props = new Properties();
    props.setProperty(ScotlandYard.PROP_ROUND_MILLIS, "10");
    ScotlandYard game = new ScotlandYard(props);

    executeScenario(game);

    assertEquals(ScotlandYard.DEFAULT_DETECTIVES, game.getNrOfDetectives());
    assertTrue(game.getCreatedUsers().isEmpty(), "cleaned up");
    assertTrue(game.getCreatedDevices().isEmpty(), "cleaned up");
  }
}
