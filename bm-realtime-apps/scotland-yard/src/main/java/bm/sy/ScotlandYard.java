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

import bm.sy.init.GameInit;
import bm.sy.init.GameProps;
import bm.traccar.api.Api;
import bm.traccar.api.Api.Users;
import bm.traccar.api.Api.Users.TraccarRole;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.User;
import bm.traccar.scenario.Scenario;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/** Scotland Yard as a Traccar realtime scenario */
public class ScotlandYard extends Scenario {

  // realtime properties - read only, logged at startup
  public static final String PROP_HOST = "traccar.host";
  public static final String PROP_USER = "traccar.name";
  public static final String PROP_EMAIL = "traccar.email";
  public static final String PROP_PASSWORD = "traccar.password";

  // scotland yard properties
  public static final String PROP_DETECTIVES = "scotland-yard.nrOfDetectives";
  public static final String PROP_ROUNDS = "scotland-yard.nrOfRounds";
  public static final String PROP_ROUND_MILLIS = "scotland-yard.roundMillis";

  public static final int DEFAULT_DETECTIVES = 2;
  public static final int DEFAULT_ROUNDS = 22;
  public static final long DEFAULT_ROUND_MILLIS = 100;

  /** Password of all users created by the scenario. */
  public static final String PLAYER_PASSWORD = GameInit.PLAYER_PASSWORD;

  public static final String PREFIX = GameInit.PREFIX;
  public static final String MISTER_X = GameInit.MISTER_X;
  public static final String DETECTIVES = GameInit.DETECTIVES;
  public static final String MANAGER = GameInit.MANAGER;

  private final GameProps gameProps;
  private final Properties properties;
  private final int nrOfDetectives;
  private final int nrOfRounds;
  private final long roundMillis;

  // the user hosting the game, logged in to the realtime service
  private User traccarAccount;
  private TraccarRole hostRole;

  // game initialization and player management
  private GameInit gameInit;

  /** Scenario with the settings of {@code application.properties} on the classpath. */
  public ScotlandYard() {
    this(new Properties());
  }

  /**
   * Scenario with explicit settings. Missing keys fall back to {@code application.properties} on
   * the classpath and finally to the defaults.
   */
  public ScotlandYard(Properties settings) {
    this.gameProps = new GameProps(settings);
    this.properties = gameProps.getProperties();
    this.nrOfDetectives = gameProps.intProperty(PROP_DETECTIVES, DEFAULT_DETECTIVES);
    this.nrOfRounds = gameProps.intProperty(PROP_ROUNDS, DEFAULT_ROUNDS);
    this.roundMillis = gameProps.longProperty(PROP_ROUND_MILLIS, DEFAULT_ROUND_MILLIS);
    if (nrOfDetectives < 1) {
      throw new IllegalArgumentException(PROP_DETECTIVES + " must be >= 1: " + nrOfDetectives);
    }
    if (nrOfRounds < 1) {
      throw new IllegalArgumentException(PROP_ROUNDS + " must be >= 1: " + nrOfRounds);
    }
    if (roundMillis < 0) {
      throw new IllegalArgumentException(PROP_ROUND_MILLIS + " must be >= 0: " + roundMillis);
    }
  }

  @Override
  public String getName() {
    return "Scotland Yard Game";
  }

  // ------------------------------------------------------------------ init
  // readablity
  private Api api;
  private Users userApi;

  @Override
  public void initScenario() throws Exception {
    // set for full life cycle of the game, including cleanup
    api = context.getApi();
    userApi = api.getUsersApi();

    // login has succeeded at this point
    traccarAccount = context.getRealtimeUser();
    if (traccarAccount == null) {
      throw new IllegalStateException("no user is logged in to the realtime service");
    }
    hostRole = userApi.getTraccarRole(traccarAccount);
    gameProps.logProperties(
        logger, traccarAccount, hostRole, nrOfDetectives, nrOfRounds, roundMillis);

    gameInit = new GameInit(context, traccarAccount, nrOfDetectives);

    switch (hostRole) {
      case VIRTUAL_ADMIN -> // fail fast ?
          logger.warn(
              "host {} is a virtual admin - not recommended for production",
              traccarAccount.getName());
      case ADMIN -> gameInit.initGameAsAdmin();
      case MANAGER -> gameInit.initGameAsManager();
      // regular user cannot host a game, but play it after above setups
      case REG_USER -> gameInit.playGameInRole();
    }

    // done for admin and manager -> exit

    System.out.println(
        "\n\tWaiting for the players to send their position from a valid start station ..\n");
  }

  /**
   * The devices owned by a player, read while authenticated as the player. Traccar's {@code GET
   * /devices} only returns the devices of the authenticated user (even for admins).
   */
  public List<Device> getPawnsOf(User player) {
    if (gameInit != null) {
      return gameInit.getPawnsOf(player);
    }
    List<Device> pawns = new ArrayList<>();
    try {
      api.runAs(player, () -> pawns.addAll(context.getApi().getDevicesApi().getDevices()));
    } catch (Exception e) {
      throw new IllegalStateException("cannot read pawns of " + player.getName(), e);
    }
    return pawns;
  }

  // ------------------------------------------------------------------- run

  @Override
  public void runScenario() throws Exception {
    logger.info("Starting {} with {} rounds ...", getName(), nrOfRounds);

    // TODO: Start GPS players for all pawns (trackers are registered)
    // TODO: move the pawns along the GameBoard
    // TODO: Verify via RealTimeController
    // - the game manager sees all pawns
    // - Mister X only sees his pawn, the detectives only theirs

    for (int round = 1; round <= nrOfRounds; round++) {
      logger.debug("round {} of {}", round, nrOfRounds);
      Thread.sleep(roundMillis); // simulate game time
    }

    logger.info("{} completed after {} rounds", getName(), nrOfRounds);
  }

  // -------------------------------------------------------------- shutdown

  @Override
  public void shutdownScenario() throws Exception {
    // NOTE: keep game for later analysis
    // write separate distinct clean method to delete game
    logger.info("Cleaning up {} ...", getName());
    if (gameInit != null) {
      gameInit.cleanup();
    }
    logger.info("{} cleanup complete", getName());
  }

  // -------------------------------------------------------------- accessors

  public int getNrOfDetectives() {
    return nrOfDetectives;
  }

  public int getNrOfRounds() {
    return nrOfRounds;
  }

  public long getRoundMillis() {
    return roundMillis;
  }

  public Properties getProperties() {
    return properties;
  }

  public Optional<TraccarRole> getHostRole() {
    return Optional.ofNullable(hostRole);
  }

  public Optional<GameInit> getGameInit() {
    return Optional.ofNullable(gameInit);
  }

  public Optional<String> getPlayerRole() {
    return gameInit != null ? gameInit.getPlayerRole() : Optional.empty();
  }

  public List<Device> getMyPawns() {
    return gameInit != null ? gameInit.getMyPawns() : Collections.emptyList();
  }

  public User getGameManager() {
    return gameInit != null ? gameInit.getGameManager() : null;
  }

  public User getMisterX() {
    return gameInit != null ? gameInit.getMisterX() : null;
  }

  public User getDetectives() {
    return gameInit != null ? gameInit.getDetectives() : null;
  }

  public Device getMisterXpawn() {
    return gameInit != null ? gameInit.getMisterXpawn() : null;
  }

  public List<Device> getDetectivePawns() {
    return gameInit != null ? gameInit.getDetectivePawns() : Collections.emptyList();
  }

  public List<User> getCreatedUsers() {
    return gameInit != null ? gameInit.getCreatedUsers() : Collections.emptyList();
  }

  public List<Device> getCreatedDevices() {
    return gameInit != null ? gameInit.getCreatedDevices() : Collections.emptyList();
  }

  public GameProps getGameProps() {
    return gameProps;
  }

  // ------------------------------------------------------------ properties

  /** Load {@code application.properties} from the classpath, empty if not present. */
  static Properties loadProperties() {
    return GameProps.loadProperties();
  }

  static String mask(String secret) {
    return GameProps.mask(secret);
  }
}
