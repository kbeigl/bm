package bm.traccar.rt;

import bm.traccar.api.Api;
import bm.traccar.api.ApiException;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.User;
import bm.traccar.ws.TraccarWebSocketRoute;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Controller encapsulates all the logic for communicating with the Traccar server.
 *
 * <p>It handles authentication, fetches the initial state via the REST API, and then connects to
 * the WebSocket to receive and process live updates, feeding everything into the thread-safe state
 * manager, i.e. the RealTimeModel.
 */
@Component // or @Service  ?
public class RealTimeController {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeController.class);

  private final RealTimeManager stateManager = RealTimeManager.getInstance();

  // private final HttpClient httpClient;
  // private final Gson gson;
  // private WebSocket webSocket;

  @Autowired protected Api api;
  @Autowired private TraccarWebSocketRoute traccarLiveConnection;

  /**
   * Authenticates with the server, loads initial data, and connects to the WebSocket.
   *
   * @return true if initialization was successful, false otherwise.
   */
  public boolean loginAndInitialize(User admin) throws Exception {

    try {
      // try bad credentials
      api.setBasicAuth(admin.getEmail(), admin.getPassword());
      loadInitialServerEntities();

    } catch (ApiException e) {
      System.err.println("Login failed: " + e.getMessage());
      return false;
    }

    // connectWebSocket
    traccarLiveConnection.loginAndConnect(admin.getEmail(), admin.getPassword());

    // after initialization was successful
    stateManager.loginUser(admin);
    logger.info(
        "User logged in: " + stateManager.getCurrentUser().map(User::getName).orElse("N/A"));

    return true;
  }

  /**
   * Before connecting to the WebSocket, load the initial set of users and devices from the server
   * via the REST API. Note that only entities that the logged-in user has permission to see will be
   * returned by the API.
   *
   * @throws ApiException
   */
  private void loadInitialServerEntities() throws ApiException {

    logger.info("--- Loading initial users from server...   ---");
    List<User> initialUsers = new ArrayList<>();
    initialUsers = api.getUsersApi().getUsers(null);
    // users from other tests are listed! -> permission management
    initialUsers.forEach(u -> logger.info("User.id{}: {}", u.getId(), u.getEmail()));
    stateManager.loadInitialUsers(initialUsers);
    logger.debug("Initial users loaded: " + stateManager.getAllUsers());

    logger.info("--- Loading initial devices from server... ---");
    List<Device> initialDevices = new ArrayList<>();
    initialDevices = api.getDevicesApi().getDevices(null);
    initialDevices.forEach(u -> logger.info("Device.id{}: {}", u.getId(), u.getName()));
    stateManager.loadInitialDevices(initialDevices);
    logger.debug("Initial devices loaded: " + stateManager.getAllDevices());

    // Positions are not loaded and only collected live after a given timestamp, i.e. game start
  }
}
