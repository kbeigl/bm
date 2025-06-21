package bm.traccar.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.generated.model.dto.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * Demo Exception handling via AOP
 *
 * <p>AOP is used to handle Exceptions for generated ApiClient and actual *Api.methods in ApiAspect
 * class.
 */
@SpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(classes = {ApiService.class})
@TestPropertySource("classpath:application.properties")
// remove ApiAspect to remove AOP
@Import({ApiConfig.class, ApiAspect.class})
// without !! ClientExceptionHandler.class
public class AspectIT {

  @Value("${traccar.web.serviceAccountToken}")
  private String virtualAdmin;

  @Value("${traccar.host}")
  private String host;

  @Value("${traccar.user.name}")
  private String name;

  @Value("${traccar.user.password}")
  private String password;

  @Value("${traccar.user.email}")
  private String email;

  @Autowired private ApiService api;

  private String invalidHost = "http://localhoRst/api";

  /*
   * - get/delete non existing user/id - login with wrong credentials
   */

  @Test
  public void createAdminUserAndLoginApiClient() {

    api.setBearerToken(virtualAdmin);

    User user = api.users.createUserWithCredentials(name, password, email);
    assertNotNull(user, "admin user NOT created");

    // upgrade user to admin
    user.setAdministrator(true);
    User updatedUser = api.users.updateUser(user.getId(), user);

    // switch Auth method, login as mail/pwd
    api.setBasicAuth(email, password);

    List<User> users = api.users.getUsers(null);
    assertNotNull(users, "nothing returned from server");

    // delete admin as admin!!
    api.users.deleteUser(updatedUser.getId());

    api.setBearerToken(virtualAdmin);
    users = api.users.getUsers(null);
    assertNotNull(users, "nothing returned from server");
  }

  @Test
  public void serverPresentWithException() {

    api.setBearerToken(virtualAdmin);

    // create user once without catch
    User user = api.users.createUserWithCredentials(name, password, email);
    assertNotNull(user, "user NOT created");
    assertTrue(user.getName().equals(name));
    assertNotNull(user.getId(), "returned user does not have an ID?");

    int userId = user.getId();
    System.out.println("user with ID=" + userId + " was created on server");
    //  assert?

    // create same user again
    // improve: catch ApiException to handle three error messages in ApiService
    try {
      user = api.users.createUserWithCredentials(name, password, email);
    } catch (ApiException e) {
      // e.printStackTrace();
      System.err.println("User was NOT created!");
      //        System.err.println("User was NOT created due to: " + e);
      //    assert?
    }

    // clean up server for next tests
    api.users.deleteUser(userId);
  }

  /** If server is unreachable make sure to send to correct errror message. */
  @Test
  public void serverAbsentCreateUser() {

    api.getApiClient().setBasePath(invalidHost);
    api.setBearerToken(virtualAdmin);
    User user = null;
    try {
      user = api.users.createUserWithCredentials(name, password, email);
    } catch (ApiException exception) {
      if (exception.getMessage().equals(ApiException.APIEX_NO_CONNECTION)) {
        System.err.println("Cannot reach server, check host");
      }
    }
    assertNull(user, "admin user was created?");
  }

  /**
   * misspelled localhost > raise no connection > server not found <br>
   * The Exception occurs in ApiMethod then in ApiClient (reverse call order) <br>
   * invoke ApiClient.invokeAPI(..) > then Exception in UsersApi.usersPost(..)
   */
  @Test
  public void serverAbsentWithoutException() {

    api.getApiClient().setBasePath(invalidHost);
    api.setBearerToken(virtualAdmin);

    Exception exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              // without catching ApiException
              api.users.createUserWithCredentials(name, password, email);
            });
    // System.out.println("caught Exception: " + exception);
    // exception.printStackTrace();
    assertTrue(exception instanceof ApiException, "exception is not an ApiException");
    assertTrue(
        exception.getMessage().equals(ApiException.APIEX_NO_CONNECTION),
        "ApiException sent wrong message");
  }

  /** Explicit catch when server is not present */
  @Test
  public void serverAbsentWithException() {

    api.getApiClient().setBasePath(invalidHost);
    api.setBearerToken(virtualAdmin);
    try {
      api.users.createUserWithCredentials(name, password, email);
    } catch (ApiException exception) {
      // System.out.println("caught Exception: " + exception);
      // exception.printStackTrace();
      assertTrue(
          exception.getMessage().equals(ApiException.APIEX_NO_CONNECTION),
          "ApiException sent wrong message");
    }
  }
}
