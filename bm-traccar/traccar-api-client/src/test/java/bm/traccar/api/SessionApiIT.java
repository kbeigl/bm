package bm.traccar.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import bm.traccar.generated.model.dto.User;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionApiIT extends BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(SessionApiIT.class);

  @Test
  public void getSessionGetJsessionIdTwice() {

    // make sure no authentication is set
    api.setBearerToken(null);
    api.setBasicAuth(null, null);

    // always a new JSESSIONID is created
    String jSession1 = api.session.getSessionGetJsessionId(virtualAdmin);
    String jSession2 = api.session.getSessionGetJsessionId(virtualAdmin);
    assertNotEquals(jSession1, jSession2);

    // org.springframework.web.client.HttpClientErrorException$BadRequest:
    //    400 Bad Request
    // api.session.getSessionGetJsessionId("invalidToken");
  }

  /**
   * Creates a session for User and provide JSESSIONID for websocket connection.
   *
   * <p>As we know the User it is not extracted from the response. Only the JSESSIONID is returned
   * and can be used to establish a WebSocket connection.
   */
  @Test
  public void createSessionGetJsessionId() {
    String jSessionId = api.session.createSessionGetJsessionId(userMail, userPassword);
    logger.info("received JSESSIONID: {}", jSessionId);
    assertEquals(true, jSessionId != null);
  }

  /**
   * Creates a session for User.
   *
   * <p>Note that this method does not provide JSESSIONID.
   */
  @Test
  public void createSessionGetUser() {
    User sessionUser = api.session.createSession(userMail, userPassword);
    logger.info("get session for User: {}/{} ", sessionUser.getName(), sessionUser.getEmail());
    assertEquals(userMail, sessionUser.getEmail());
    assertEquals(userName, sessionUser.getName());
  }

  /*
   * 'virtualAdmin' is a token. i.e. {traccar.web.serviceAccountToken}
   * This works for virtual admin, but usage with (null) and (token) is yet unclear.
   */
  @Test
  public void getSessionSuperUser() {
    User sessionUser = api.session.getSession(virtualAdmin);
    logger.info("get session for SuperUser: {}/{} ", sessionUser.getName(), sessionUser.getEmail());
    assertEquals("Service Account", sessionUser.getName());
    assertEquals("none", sessionUser.getEmail());
  }

  @Test
  public void getJsessionIdForSuperUser() {
    String jSessionId = api.session.getSessionGetJsessionId(virtualAdmin);
    logger.info("received JSESSIONID: {}", jSessionId);
    assertEquals(true, jSessionId != null);
  }

  @Test
  public void createGetDeleteSuperUserSession() {

    //	String jSessionId = api.session.createSessionGetJsessionId(virtualAdmin);
    //	logger.debug("received JSESSIONID: {}", jSessionId);
    //	assertEquals(true, jSessionId != null);
    //	api.session.deleteSession();
    //	logger.debug("deleted session for super user");

  }
}
