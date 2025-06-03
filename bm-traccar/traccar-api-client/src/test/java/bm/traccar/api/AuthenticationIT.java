package bm.traccar.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import bm.traccar.generated.model.dto.User;
import bm.traccar.invoke.ApiClient;
import bm.traccar.invoke.auth.HttpBasicAuth;
import bm.traccar.invoke.auth.HttpBearerAuth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(classes = {ApiService.class})
@TestPropertySource("classpath:application.properties")
@Import(ApiConfig.class)
// test without AOP ?
// @Import({ApiConfig.class, ApiAspect.class})
// @ExtendWith(ClientExceptionHandler.class)
public class AuthenticationIT {

  @Value("${traccar.web.serviceAccountToken}")
  private String virtualAdmin;

  // add and distinguish {traccar.apikey}
  @Value("${traccar.user.name}")
  private String name;

  @Value("${traccar.user.password}")
  private String password;

  @Value("${traccar.user.email}")
  private String mail;

  @Autowired private ApiService api;
  @Autowired // get client from IoC
  private ApiClient apiClient;

  /*
   * TODO Authentication and User management - userId Can only be used by admin or manager users - create
   * virtualAdmin, admin/s, manager/s and users - find and get virtualAdmin, admins, managers and users - login/logout
   * > return 'role'
   */

  @Test
  public void createAdminUserAndLoginApiClient() {
    api.setBearerToken(virtualAdmin);

    User user1 = createUserWithCredentials(name, password, mail);
    assertNotNull(user1, "admin user NOT created");

    // upgrade user to admin
    user1.setAdministrator(true);
    User updatedUser = api.users.updateUser(user1.getId(), user1);

    // switch Auth method, login as mail/pwd
    api.setBasicAuth(mail, password);

    showCredentials();

    List<User> users = api.users.getUsers(null);
    assertNotNull(users, "nothing returned from server");

    // delete admin !!
    api.users.deleteUser(updatedUser.getId());

    // change back to SuperUserAccess
    api.setBearerToken(virtualAdmin);

    users = api.users.getUsers(null);
    assertNotNull(users, "nothing returned from server");
  }

  /**
   * Traccar only accepts one authentication and here BASIC_AUTH is preferred over API_KEY. Therefor
   * API_KEY is unset in case two authentications are set.
   */
  // use to initialize ApiService. or rather in setters token/user
  @Test
  public void createAdminUserAndLoginHttpAuth() {
    showCredentials();

    // implicit authentication test
    List<User> users = api.users.getUsers(null);
    assertNotNull(users, "nothing returned from server");

    User user1 = createUserWithCredentials(name, password, mail);
    assertNotNull(user1, "admin user NOT created");

    // upgrade user to admin
    user1.setAdministrator(true);
    User updatedUser = api.users.updateUser(user1.getId(), user1);

    // switch Auth method
    HttpBearerAuth ApiKey = (HttpBearerAuth) apiClient.getAuthentication("ApiKey");
    String nul = null;
    ApiKey.setBearerToken(nul);

    // RuntimeException: No API key authentication configured!
    // would be the third authentication, not required
    // apiClient.setApiKey(null);
    // apiClient.setApiKeyPrefix(null);

    // login as mail/pwd ...
    HttpBasicAuth BasicAuth = (HttpBasicAuth) apiClient.getAuthentication("BasicAuth");
    BasicAuth.setUsername(mail);
    BasicAuth.setPassword(password);

    showCredentials();

    users = api.users.getUsers(null);
    assertNotNull(users, "nothing returned from server");

    // delete admin !!
    api.users.deleteUser(updatedUser.getId());

    // change back to SuperUserAccess via apiClient!
    apiClient.setUsername(null);
    apiClient.setPassword(null);
    apiClient.setBearerToken(virtualAdmin);

    users = api.users.getUsers(null);
    assertNotNull(users, "nothing returned from server");
  }

  private User createUserWithCredentials(String usr, String pwd, String mail) {
    User user = new User();
    user.setName(usr);
    user.setEmail(mail);
    user.setPassword(pwd);
    return api.users.createUser(user);
  }

  /** Authentications required to GET actual credentials. Set credentials via apiClient. */
  private void showCredentials() {
    // apiClient.getAuthentications(); // check how many / create one for each user?
    HttpBearerAuth ApiKey = (HttpBearerAuth) apiClient.getAuthentication("ApiKey");
    HttpBasicAuth BasicAuth = (HttpBasicAuth) apiClient.getAuthentication("BasicAuth");

    // if usr/pwd = null then authenticated by ApiKey.BearerToken
    System.out.println("ApiKey.BearerToken=" + ApiKey.getBearerToken());
    System.out.println(
        "         BasicAuth=" + BasicAuth.getUsername() + "/" + BasicAuth.getPassword());
  }
}
