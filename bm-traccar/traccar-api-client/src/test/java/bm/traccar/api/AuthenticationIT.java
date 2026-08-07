package bm.traccar.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import bm.traccar.generated.model.dto.User;
import bm.traccar.invoke.ApiClient;
import bm.traccar.invoke.auth.HttpBasicAuth;
import bm.traccar.invoke.auth.HttpBearerAuth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AuthenticationIT extends BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(AuthenticationIT.class);

  @Autowired // get client from IoC	??
  private ApiClient apiClient;

  @Test
  public void demonstrateAuthentication() {

    // 1. Using Bearer Token for admin operations
    api.setBearerToken(virtualAdmin);
    logger.info("Authenticated: {}", api.getAuthentication());

    List<User> allUsers = api.users.getAllUsers();
    logger.info("Found {} users", allUsers.size());

    // 2. Switch to Basic Auth for user-specific operations
    api.setBasicAuth("manager@domain.com", "managerPass");
    logger.info("Authenticated: {}", api.getAuthentication());

    // Create a new user as manager
    User newUser =
        api.users.createUserWithCredentials("employee", "pass123", "employee@domain.com", false);

    // 3. Login as the new user
    api.setBasicAuth("employee@domain.com", "pass123");

    // Get session for WebSocket connection
    String jsessionId = api.session.createSessionGetJsessionId("employee@domain.com", "pass123");
    logger.info("JSESSIONID for WebSocket: {}", jsessionId);

    // api.session.getSessionGetJsessionId(jsessionId);

    //    User sessionUser = api.session.getSession(jsessionId);
    //    logger.info("Authenticated: {}", sessionUser);

    // 4. Switch back to service account for cleanup
    api.setBearerToken(virtualAdmin);
    api.users.deleteUser(newUser.getId());
  }

  @Test
  public void switchAuthentication() {
    // 'login' as mail/pwd
    api.setBasicAuth(adminMail, adminPassword);
    showCredentials();
    List<User> users = api.users.getAllUsers();
    logger.info("admin users: {}", users);
    assertNotNull(users, "nothing returned from server");

    // delete user as user! suicidal?
    // api.users.deleteUser(adminId);

    // change back to SuperUserAccess
    api.setBearerToken(virtualAdmin);
    showCredentials();
    users = api.users.getAllUsers();
    logger.info("virtualAdmin users: {}", users);
    assertNotNull(users, "nothing returned from server");
  }

  /**
   * Traccar only accepts one authentication and here BASIC_AUTH is preferred over API_KEY. Therefor
   * API_KEY is unset in case two authentications are set.
   */
  // use to initialize ApiService. or rather in setters token/user
  @Test
  public void createAdminUserAndLoginHttpAuth() {
    // who is logged in?
    showCredentials();

    // switch Auth method
    HttpBearerAuth ApiKey = (HttpBearerAuth) apiClient.getAuthentication("ApiKey");
    String nul = null;
    ApiKey.setBearerToken(nul);

    // login as mail/pwd ...
    HttpBasicAuth BasicAuth = (HttpBasicAuth) apiClient.getAuthentication("BasicAuth");
    BasicAuth.setUsername(userMail);
    BasicAuth.setPassword(userPassword);

    showCredentials();

    List<User> users = api.users.getAllUsers();
    logger.info("'user' users: {}", users);
    assertNotNull(users, "nothing returned from server");

    // change back to SuperUserAccess via apiClient!
    apiClient.setUsername(null);
    apiClient.setPassword(null);
    apiClient.setBearerToken(virtualAdmin);

    users = api.users.getAllUsers();
    logger.info("virtualAdmin users: {}", users);
    assertNotNull(users, "nothing returned from server");
  }

  /** Authentications required to GET actual credentials. Set credentials via apiClient. */
  private void showCredentials() {
    // apiClient.getAuthentications(); // check how many / create one for each user?
    HttpBearerAuth ApiKey = (HttpBearerAuth) apiClient.getAuthentication("ApiKey");
    HttpBasicAuth BasicAuth = (HttpBasicAuth) apiClient.getAuthentication("BasicAuth");

    // if usr/pwd = null then authenticated by ApiKey.BearerToken
    logger.info("ApiKey.BearerToken={}", ApiKey.getBearerToken());
    logger.info("         BasicAuth={}/{}", BasicAuth.getUsername(), BasicAuth.getPassword());

    // Use the new whoIsAuthenticated() method
    logger.info("whoIsAuthenticated: {}", api.getAuthentication());
  }
}
