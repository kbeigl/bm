package bm.traccar.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.generated.model.dto.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;

/**
 * Demo Exception handling via AOP
 *
 * <p>AOP is used to handle Exceptions in one place for generated ApiClient and actual *Api.methods
 * in ApiAspect class.
 */
// move up to BaseIntegrationTest ?
// -> different behavior for various ITests
@Import(ApiAspect.class)
public class AspectIT extends BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(AspectIT.class);

  @Value("${traccar.host}")
  private String host;

  private String invalidHost = "http://localhoRst/api";

  /** Test for ApiException.APIEX_UNAUTHORIZED <br> */
  @Test
  public void unauthorizedUser() {

    // reset from previous tests with invalid host ?
    // api.getApiClient().setBasePath(host);

    api.setBasicAuth(userMail, userPassword);
    List<User> users = api.getUsersApi().getUsers(null);
    // this only returns users as admin
    assertEquals(0, users.size());

    // bad password
    api.setBasicAuth(userMail, userPassword + "XXX");
    try {
      users = api.getUsersApi().getUsers(null);
    } catch (ApiException e) {
      logger.error("caught ApiException: {}", e.getMessage());
      assertTrue(
          e.getMessage().equals(ApiException.APIEX_UNAUTHORIZED),
          "ApiException sent wrong message");
    }
    // reset to valid user
    api.setBasicAuth(userMail, userPassword);
  }

  @Test
  public void userLoginApiClient() {

    // api.getUsersApi().getUsers returns null instead of empty list !?
    // switch Auth method, login as mail/pwd
    api.setBasicAuth(adminMail, adminPassword);
    List<User> users = api.getUsersApi().getUsers(null);
    // assertEquals(users.size(), 0);

    api.setBearerToken(virtualAdmin);
    users = api.getUsersApi().getUsers(null);
    // assertEquals(users.size(), 1);
  }

  /**
   * misspelled localhost > raise no connection > server not found <br>
   * The Exception occurs in ApiMethod then in ApiClient (reverse call order) <br>
   * invoke ApiClient.invokeAPI(..) > then Exception in UsersApi.usersPost(..)
   */
  //  @Test
  public void serverAbsentWithoutException() {

    // TODO
    // changeing the BasePath causes problems with the ApiClient
    // (in consecutive tests or BaseIntegrationTest @After)
    // teardown method throws HttpClientErrorException: 405 METHOD_NOT_ALLOWED
    api.setBasePath(invalidHost);

    // does this make sense without a valid host? move up?
    api.setBearerToken(virtualAdmin);

    Exception exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              // without catching ApiException
              api.getUsersApi().createUserWithCredentials(userName, userPassword, userMail, false);
            });
    logger.error("caught ApiException: {}", exception.getMessage());
    assertTrue(exception instanceof ApiException, "exception is not an ApiException");
    assertTrue(
        exception.getMessage().equals(ApiException.APIEX_NO_CONNECTION),
        "ApiException sent wrong message");

    // reset host for consecutive tests
    api.setBasePath(host);
  }

  /** Explicit catch when server is not present */
  //  @Test
  public void serverAbsentWithException() {

    // TODO
    // changeing the BasePath causes problems with the ApiClient
    // (in consecutive tests or BaseIntegrationTest @After)
    api.setBasePath(invalidHost);

    // does this make sense without a valid host? move up?
    api.setBearerToken(virtualAdmin);
    try {
      api.getUsersApi().createUserWithCredentials(userName, userPassword, userMail, false);
    } catch (ApiException exception) {
      logger.error("caught ApiException: {}", exception.getMessage());
      assertTrue(
          exception.getMessage().equals(ApiException.APIEX_NO_CONNECTION),
          "ApiException sent wrong message");
    }
    // reset host for consecutive tests
    api.setBasePath(host);
  }

  /**
   * This represents problems with POST requests, which can have many causes on serverside. For
   * example, if the user already exists, i.e. database PK problem.
   */
  @Test
  public void createExistingUser() {

    try { // .. to create existing user again
      api.getUsersApi().createUserWithCredentials(userName, userPassword, userMail, false);
    } catch (ApiException e) {
      if (e.getMessage().equals(ApiException.APIEX_BAD_REQUEST)) {
        logger.error("ApiException caught: {}", e.getMessage());
        logger.error("User '{}' already exists!", userMail);
      } else {
        logger.error("Unexpected ApiException: {}", e.getMessage());
      }
      assertTrue(
          e.getMessage().equals(ApiException.APIEX_BAD_REQUEST), "ApiException sent wrong message");
    }
  }
}
