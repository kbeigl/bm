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

    // a new JSESSIONID is always created
    String jSession1 = api.session.getSessionGetJsessionId(virtualAdmin);
    String jSession2 = api.session.getSessionGetJsessionId(virtualAdmin);

    // handle two connections with different JSESSIONID for the same user (virtualAdmin) ?

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
   * Works for virtual admin. Clarify usage with (null) and (token)!
  @Test
  public void getSessionSuperUser() {
    User sessionUser = api.session.getSession(virtualAdmin);
    logger.info("get session for SuperUser: {}/{} ", sessionUser.getName(), sessionUser.getEmail());
    assertEquals("Service Account", sessionUser.getName());
    assertEquals("none", sessionUser.getEmail());
  }
   */

  //  @Test
  //  public void getJsessionIdForSuperUser() {
  //    String jSessionId = api.session.getSessionGetJsessionId(virtualAdmin);
  //    logger.info("received JSESSIONID: {}", jSessionId);
  //    assertEquals(true, jSessionId != null);
  //  }

  // @Test
  //  public void createGetDeleteSuperUserSession() {
  //	String jSessionId = api.session.createSessionGetJsessionId(virtualAdmin);
  //	logger.debug("received JSESSIONID: {}", jSessionId);
  //	assertEquals(true, jSessionId != null);
  //	api.session.deleteSession();
  //	logger.debug("deleted session for super user");
  //  }

  /**
   * Test getCurrentUser() - get the currently authenticated user without passing parameters. This
   * corresponds to GET /api/session without parameters and returns the user based on current
   * authentication (BasicAuth, BearerToken, or JSESSIONID).
   */
  @Test
  public void getCurrentUser() {
    // Test with BasicAuth
    api.setBasicAuth(userMail, userPassword);
    User currentUser = api.session.getCurrentUser();
    logger.info("Current user via BasicAuth: {}/{}", currentUser.getName(), currentUser.getEmail());
    assertEquals(userMail, currentUser.getEmail());
    assertEquals(userName, currentUser.getName());

    // Test with BearerToken (virtual admin)
    api.setBearerToken(virtualAdmin);
    User virtualAdminUser = api.session.getCurrentUser();
    logger.info(
        "Current user via BearerToken: {}/{}",
        virtualAdminUser.getName(),
        virtualAdminUser.getEmail());
    assertEquals("Service Account", virtualAdminUser.getName());
    assertEquals("none", virtualAdminUser.getEmail());

    // Test with different user
    api.setBasicAuth(adminMail, adminPassword);
    User adminUser = api.session.getCurrentUser();
    logger.info("Current user via BasicAuth: {}/{}", adminUser.getName(), adminUser.getEmail());
    assertEquals(adminMail, adminUser.getEmail());
  }
}
