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
package bm.sy.init;

import bm.traccar.api.Api;
import bm.traccar.api.Api.Users;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Permission;
import bm.traccar.generated.model.dto.User;
import bm.traccar.rtapp.RealTimeContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles initialization of Scotland Yard game users and pawns depending on the host's role. */
public class GameInit {

  private static final Logger logger = LoggerFactory.getLogger(GameInit.class);

  public static final String PLAYER_PASSWORD = "scotland-yard";

  public static final String PREFIX = "sy-";
  public static final String MISTER_X = "misterX";
  public static final String DETECTIVES = "detectives";
  public static final String MANAGER = "game-manager";

  private final RealTimeContext context;
  private final Api api;
  private final Users userApi;
  private final User host;
  private final int nrOfDetectives;

  private User gameManager;
  private User misterX;
  private User detectives;

  private Device misterXpawn;
  private final List<Device> detectivePawns = new ArrayList<>();

  private String playerRole;
  private final List<Device> myPawns = new ArrayList<>();

  private final List<User> createdUsers = new ArrayList<>();
  private final List<Device> createdDevices = new ArrayList<>();

  public GameInit(RealTimeContext context, User host, int nrOfDetectives) {
    this.context = Objects.requireNonNull(context, "context");
    this.host = Objects.requireNonNull(host, "host");
    this.nrOfDetectives = nrOfDetectives;
    this.api = context.getApi();
    this.userApi = api.getUsersApi();
  }

  /**
   * An admin creates a dedicated game manager and then creates the players <i>as the manager</i>.
   * Users created by a manager are managed by that manager automatically - Traccar's REST API does
   * not allow to link users afterwards.
   */
  public void initGameAsAdmin() throws Exception {
    long timestamp = System.currentTimeMillis();
    int pawns = 1 + nrOfDetectives;

    String name = PREFIX + MANAGER + "-" + timestamp;
    gameManager =
        userApi.createManagerWithCredentials(
            name, PLAYER_PASSWORD, name + "@example.com", 2, pawns);
    createdUsers.add(gameManager);
    logger.info("created game manager {} (id={})", gameManager.getName(), gameManager.getId());
    context.getApi().runAs(gameManager, () -> createPlayers(timestamp));
  }

  /** A manager hosts the game himself and creates the players as managed users. */
  public void initGameAsManager() throws Exception {
    gameManager = host;
    logger.info("host {} acts as game manager", gameManager.getName());
    createPlayers(System.currentTimeMillis());
  }

  /**
   * Welcomes a regular user (player) in his game role (Mister X or Detectives) and lists his
   * assigned pawns.
   */
  public void playGameInRole() throws Exception {
    List<Device> pawns = api.getDevicesApi().getDevices();
    playerRole = determineRole(host, pawns);
    myPawns.clear();
    myPawns.addAll(pawns);

    logger.info("Welcome player '{}' in role: {}", host.getName(), playerRole);
    System.out.printf("%nWelcome %s! You are playing as %s.%n", host.getName(), playerRole);

    if ("Mister X".equalsIgnoreCase(playerRole)) {
      misterX = host;
      if (!pawns.isEmpty()) {
        misterXpawn = pawns.get(0);
      }
    } else if ("Detectives".equalsIgnoreCase(playerRole)) {
      detectives = host;
      detectivePawns.clear();
      detectivePawns.addAll(pawns);
    }

    if (pawns.isEmpty()) {
      logger.warn("Player '{}' has no pawns assigned", host.getName());
      System.out.println("You have no pawns assigned.");
    } else {
      logger.info("Pawns for {} ({}):", host.getName(), pawns.size());
      System.out.printf("Your pawns (%d):%n", pawns.size());
      for (Device pawn : pawns) {
        logger.info(
            "  - Pawn: {} (id={}, uniqueId={})", pawn.getName(), pawn.getId(), pawn.getUniqueId());
        System.out.printf(
            "  - %s (id=%d, uniqueId=%s)%n", pawn.getName(), pawn.getId(), pawn.getUniqueId());
      }
    }
  }

  /**
   * Create the players and their pawns <i>as the game manager</i>:
   *
   * <ol>
   *   <li>the player is created as a regular user, hence managed by the manager
   *   <li>the pawns are created by the manager and a local tracker is registered for each pawn
   *   <li>the pawns are linked to the player with a device permission
   * </ol>
   */
  private void createPlayers(long timestamp) throws Exception {
    logger.info("creating players and pawns ...");

    // Mister X with one pawn
    String xName = PREFIX + MISTER_X + "-" + timestamp;
    misterX = userApi.createUserWithCredentials(xName, PLAYER_PASSWORD, xName + "@example.com");
    createdUsers.add(misterX);
    logger.info("created player {} (id={})", misterX.getName(), misterX.getId());
    misterXpawn = createPawn(misterX, xName);

    // the detectives player with one pawn for each detective
    String dName = PREFIX + DETECTIVES + "-" + timestamp;
    detectives = userApi.createUserWithCredentials(dName, PLAYER_PASSWORD, dName + "@example.com");
    createdUsers.add(detectives);
    logger.info("created player {} (id={})", detectives.getName(), detectives.getId());
    for (int i = 1; i <= nrOfDetectives; i++) {
      detectivePawns.add(createPawn(detectives, PREFIX + "detective-" + i + "-" + timestamp));
    }

    logger.info(
        "Scotland Yard Game scenario setup: manager {}, Mister X {}, {} detectives with {} pawns",
        gameManager.getName(),
        misterX.getName(),
        nrOfDetectives,
        detectivePawns.size());
  }

  /** Create a pawn with tracker (owned by the current user) and link it to the player. */
  private Device createPawn(User player, String uniqueId) throws Exception {
    Device pawn = context.getService().createDeviceWithTracker(uniqueId, uniqueId, "virtual");
    createdDevices.add(pawn);
    context.getService().getOrCreateTracker(uniqueId);

    Permission permission = new Permission();
    permission.setUserId(player.getId());
    permission.setDeviceId(pawn.getId());
    context.getApi().getPermissionsApi().createPermission(permission);
    logger.info("pawn {} (id={}) assigned to {}", uniqueId, pawn.getId(), player.getName());
    return pawn;
  }

  public List<Device> getPawnsOf(User player) {
    List<Device> pawns = new ArrayList<>();
    try {
      api.runAs(player, () -> pawns.addAll(context.getApi().getDevicesApi().getDevices()));
    } catch (Exception e) {
      throw new IllegalStateException("cannot read pawns of " + player.getName(), e);
    }
    return pawns;
  }

  public void cleanup() {
    for (Device device : createdDevices) {
      try {
        context.getTrackerRegistration().unregisterTracker(device.getUniqueId());
        context.getApi().getDevicesApi().deleteDevice(device.getId());
        logger.info("Deleted pawn: {} (id={})", device.getUniqueId(), device.getId());
      } catch (Exception e) {
        logger.warn("Failed to delete device {}: {}", device.getName(), e.getMessage());
      }
    }
    createdDevices.clear();
    detectivePawns.clear();
    misterXpawn = null;

    List<User> usersToDelete = new ArrayList<>(createdUsers);
    Collections.reverse(usersToDelete);
    for (User user : usersToDelete) {
      try {
        userApi.deleteUser(user.getId());
        logger.info("Deleted user: {}", user.getName());
      } catch (Exception e) {
        logger.warn("Failed to delete user {}: {}", user.getName(), e.getMessage());
      }
    }
    createdUsers.clear();
    misterX = null;
    detectives = null;
    gameManager = null;
  }

  public static String determineRole(User user, List<Device> pawns) {
    String name = user.getName() != null ? user.getName().toLowerCase() : "";
    String email = user.getEmail() != null ? user.getEmail().toLowerCase() : "";

    if (name.contains("mister") || email.contains("mister")) {
      return "Mister X";
    }
    if (name.contains("detective") || email.contains("detective")) {
      return "Detectives";
    }

    for (Device pawn : pawns) {
      String pawnName = pawn.getName() != null ? pawn.getName().toLowerCase() : "";
      String uniqueId = pawn.getUniqueId() != null ? pawn.getUniqueId().toLowerCase() : "";
      if (pawnName.contains("mister") || uniqueId.contains("mister")) {
        return "Mister X";
      }
      if (pawnName.contains("detective") || uniqueId.contains("detective")) {
        return "Detectives";
      }
    }

    return "Player";
  }

  public User getGameManager() {
    return gameManager;
  }

  public User getMisterX() {
    return misterX;
  }

  public User getDetectives() {
    return detectives;
  }

  public Device getMisterXpawn() {
    return misterXpawn;
  }

  public List<Device> getDetectivePawns() {
    return Collections.unmodifiableList(detectivePawns);
  }

  public Optional<String> getPlayerRole() {
    return Optional.ofNullable(playerRole);
  }

  public List<Device> getMyPawns() {
    return Collections.unmodifiableList(myPawns);
  }

  public List<User> getCreatedUsers() {
    return Collections.unmodifiableList(createdUsers);
  }

  public List<Device> getCreatedDevices() {
    return Collections.unmodifiableList(createdDevices);
  }
}
