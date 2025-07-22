package bm.traccar.api;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import bm.traccar.generated.model.dto.User;
import bm.traccar.ws.PositionProcessor;
import bm.traccar.ws.TraccarWsClientRoute;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class SessionIT {

  @Value("${traccar.web.serviceAccountToken}")
  private String virtualAdmin;

  @Value("${traccar.user.name}")
  private String name;

  @Value("${traccar.user.password}")
  private String password;

  @Value("${traccar.user.email}")
  private String mail;

  @Autowired private ApiService api;
  @Autowired protected ProducerTemplate producer;

  @Test
  public void createSessionForUser() {
    api.setBearerToken(virtualAdmin);
    User user = api.users.createUserWithCredentials(name, password, mail);
    int userId = user.getId();
    // ----------------------------------------------------
    System.out.println("  create session for: " + mail + "/" + password);
    ResponseEntity<User> response = api.createSessionPostWithHttpInfo(mail, password);
    // System.out.println(response.getBody());
    if (checkHttpStatusCode(response.getStatusCode())) {
      User sessionUser = response.getBody();
      System.out.println(" Session created for: " + sessionUser.getEmail());
      String setCookieHeader = response.getHeaders().get("Set-Cookie").get(0);
      String sessionId = setCookieHeader.split(";")[0].split("=")[1];
      System.out.println("received: " + "JSESSIONID: " + sessionId);
      // call route traccarWebSocketConnectionRoute
      producer.sendBodyAndHeader("direct:connectTraccarWebSocket", null, "JSESSIONID", sessionId);
      // System.out.println("WebSocket connection initiated with JSESSIONID: " + sessionId);
    } else {
      System.out.println("Failed to create session for user: " + mail);
    }
    // create api.session. method ..
    // ----------------------------------------------------
    api.users.deleteUser(userId);
  }

  @Test
  public void createSessionIdsForUser() {

    api.setBearerToken(virtualAdmin);
    User user = api.users.createUserWithCredentials(name, password, mail);
    int userId = user.getId();
    // ----------------------------------------------------

    System.out.println("create session for: " + mail + "/" + password);
    ResponseEntity<User> response = api.createSessionPostWithHttpInfo(mail, password);
    // System.out.println(response.getBody());

    if (checkHttpStatusCode(response.getStatusCode())) {
      System.out.println("Session created successfully for user: " + mail);
    } else {
      System.out.println("Failed to create session for user: " + mail);
    }

    // for (Map.Entry<String, List<String>> entry : response.getHeaders().entrySet()) {
    //   System.out.println(entry.getKey() + ": " + entry.getValue());
    // }
    // extract JSESSIONID
    // response.getHeaders().get("Set-Cookie").forEach(System.out::println);
    // improve to sort out JSESSIONID cookie explicitly
    String setCookieHeader = response.getHeaders().get("Set-Cookie").get(0);
    System.out.println("created session Set-Cookie: " + setCookieHeader);

    // ------------------------------------------

    System.out.println("create another session for: " + mail + "/" + password);
    response = api.createSessionPostWithHttpInfo(mail, password);

    if (checkHttpStatusCode(response.getStatusCode())) {
      System.out.println("Session created successfully for user: " + mail);
    } else {
      System.out.println("Failed to create session for user: " + mail);
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

    // ----------------------------------------------------
    api.users.deleteUser(userId);
  }

  /** Test platonic API methods from interface for User DTO */
  @Test
  public void createUserAndSessionIdWithHttpInfo() {

    api.setBearerToken(virtualAdmin);
    User user = api.users.createUserWithCredentials(name, password, mail);
    int userId = user.getId();
    // ----------------------------------------------------
    // .setBasicAuth(mail, password);
    // User sessionUser = api.session.createSession(mail, password);
    // System.out.println(sessionUser);
    // ResponseSpec reponseSpec = api.createSessionPostWithResponseSpec(mail,
    // password);
    // System.out.println(reponseSpec.toString());

    // create method via api.session without setting BasicAuth ...
    ResponseEntity<User> response = api.createSessionPostWithHttpInfo(mail, password);
    System.out.println("SessionPostWithHttpInfo - response headers: " + response.getHeaders());

    // ----------------------------------------------------
    api.users.deleteUser(userId);
  }

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
