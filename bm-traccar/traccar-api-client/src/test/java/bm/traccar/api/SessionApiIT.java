package bm.traccar.api;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import bm.traccar.generated.model.dto.User;
import bm.traccar.ws.PositionProcessor;
import bm.traccar.ws.TraccarWsClientRoute;
import java.util.List;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

// @SpringBootTest
@CamelSpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(
    classes = {ApiService.class, TraccarWsClientRoute.class, PositionProcessor.class})
@TestPropertySource("classpath:application.properties")
@Import(ApiConfig.class)
public class SessionApiIT extends BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(SessionApiIT.class);

  @Test
  public void createSessionForUser() {

    // set BasAuth (who's logged in?)

    List<User> users = api.users.getUsers(null); // check if user exists
    User registeredUser = users.stream().filter(u -> u.getId() == userId).findFirst().orElse(null);
    // upgrade user to admin for device creation
    registeredUser.setAdministrator(true);
    User updatedUser = api.users.updateUser(userId, registeredUser);

    // create Device

    // ----------------------------------------------------
    logger.info("create session for {}/{}", userMail, userPassword);
    User sessionUser = api.session.createAndOpenSession(userMail, userPassword);

    System.out.println("trigger event and observe live ...");
    //    wait for five minutes to observe live events
    //    try {
    //      Thread.sleep(5 * 60 * 1000); // 5 minutes
    //    } catch (InterruptedException e) {
    //      Thread.currentThread().interrupt();
    //      System.out.println("Thread interrupted while waiting for live events.");
    //    }
  }

  @Test
  public void createSessionIdsForUser() {

    System.out.println("create session for: " + userMail + "/" + userPassword);
    ResponseEntity<User> response = api.createSessionPostWithHttpInfo(userMail, userPassword);
    // System.out.println(response.getBody());

    if (checkHttpStatusCode(response.getStatusCode())) {
      System.out.println("Session created successfully for user: " + userMail);
    } else {
      System.out.println("Failed to create session for user: " + userMail);
    }

    String setCookieHeader = response.getHeaders().get("Set-Cookie").get(0);
    System.out.println("created session Set-Cookie: " + setCookieHeader);

    // ------------------------------------------

    System.out.println("create another session for: " + userMail + "/" + userPassword);
    response = api.createSessionPostWithHttpInfo(userMail, userPassword);

    if (checkHttpStatusCode(response.getStatusCode())) {
      System.out.println("Session created successfully for user: " + userMail);
    } else {
      System.out.println("Failed to create session for user: " + userMail);
    }

    // extract !NEW! JSESSIONID
    // response.getHeaders().get("Set-Cookie").forEach(System.out::println);
    String setNewCookieHeader = response.getHeaders().get("Set-Cookie").get(0);
    System.out.println("created new session Set-Cookie: " + setNewCookieHeader);

    assertNotEquals(
        setCookieHeader,
        setNewCookieHeader,
        "The JSESSIONID should be different for each session created.");

    // create api.session. method ..

  }

  /** Test platonic API methods from interface for User DTO */
  @Test
  public void createUserAndSessionIdWithHttpInfo() {

    // .setBasicAuth(mail, password);
    // User sessionUser = api.session.createSession(mail, password);
    // System.out.println(sessionUser);
    // ResponseSpec reponseSpec = api.createSessionPostWithResponseSpec(mail,
    // password);
    // System.out.println(reponseSpec.toString());

    // create method via api.session without setting BasicAuth ...
    ResponseEntity<User> response = api.createSessionPostWithHttpInfo(userMail, userPassword);
    System.out.println("SessionPostWithHttpInfo - response headers: " + response.getHeaders());

    // ----------------------------------------------------
    // api.users.deleteUser(userId);
  }

  @Deprecated // see Aspect implementation
  private boolean checkHttpStatusCode(HttpStatusCode sc) {
    if (sc.is2xxSuccessful() || sc.is3xxRedirection()) return true;
    // TODO these two respones are not considered yet
    else if (sc.is1xxInformational()) System.out.println("Informational response received: " + sc);
    else if (sc.is3xxRedirection()) System.out.println("Redirection response received: " + sc);
    // else Error > return false;
    else if (sc.is4xxClientError()) System.out.println("Client error occurred: " + sc);
    else if (sc.is5xxServerError()) System.out.println("Server error occurred: " + sc);
    else System.out.println("Failed to create session: " + sc);
    return false;
  }
}
