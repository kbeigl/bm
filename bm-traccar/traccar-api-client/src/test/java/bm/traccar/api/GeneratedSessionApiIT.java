package bm.traccar.api;

import bm.traccar.generated.api.SessionApi;
import bm.traccar.generated.model.dto.User;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

/** Simple AttributesApi test with explicit HttpStatusCodeException handling. */
@Import(ApiAspect.class)
public class GeneratedSessionApiIT extends BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(GeneratedSessionApiIT.class);
  @Autowired private SessionApi sessionApi;

  @Test
  public void sessionPostWithHttpInfo() {
    logger.info(" create session for {}/{}", adminMail, adminPassword);
    ResponseEntity<User> response = sessionApi.sessionPostWithHttpInfo(adminMail, adminPassword);
    // logger.info("Response: {}", response);
    logger.info("Body: {}", response.getBody());
    logger.info("Status: {} - {}", response.getStatusCode(), response.getStatusCode().value());
    logger.info("Headers: {}", response.getHeaders());

    if (checkHttpStatusCode(response.getStatusCode())) {
      String setCookieHeader = response.getHeaders().get("Set-Cookie").get(0);
      logger.info("created session Set-Cookie: " + setCookieHeader);
    } else {
      logger.error("Failed to create session for user: " + adminMail);
    }
  }

  /**
   * Create a session for admin user and return the User object with user id. The session is created
   * but not closed. This method does not provide JSESSIONID.
   */
  @Test
  public void sessionPost() {
    User sessionUser = sessionApi.sessionPost(adminMail, adminPassword);
    logger.info(" created session {}/{} for user: ", adminMail, adminPassword, sessionUser);
  }

  /*
   * verify that Aspect is handling this and throwing ApiException
   */
  @Deprecated
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
