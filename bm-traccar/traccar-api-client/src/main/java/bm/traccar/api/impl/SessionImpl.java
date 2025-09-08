package bm.traccar.api.impl;

import bm.traccar.api.Api;
import bm.traccar.generated.api.SessionApi;
import bm.traccar.generated.model.dto.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

public class SessionImpl implements Api.Session {
  private static final Logger logger = LoggerFactory.getLogger(SessionImpl.class);
  private final SessionApi sessionApi;

  public SessionImpl(SessionApi sessionApi) {
    this.sessionApi = sessionApi;
  }

  @Override
  public User createSession(String mail, String password) {
    return sessionApi.sessionPost(mail, password);
  }

  @Override
  public String createSessionGetJsessionId(String mail, String password) {
    logger.debug(" create session for {}/{}", mail, password);
    ResponseEntity<User> response = sessionApi.sessionPostWithHttpInfo(mail, password);
    String jSessionId = extractJsessionId(response);
    if (jSessionId != null) {
      logger.debug("received JSESSIONID: {}", jSessionId);
      return jSessionId;
    } else {
      logger.error("Failed to create session for user: {}", mail);
      return null;
    }
  }

  @Override
  public User getSession(String token) {
    return sessionApi.sessionGet(token);
  }

  @Override
  public String getSessionGetJsessionId(String token) {
    ResponseEntity<User> response = sessionApi.sessionGetWithHttpInfo(token);
    String jSessionId = extractJsessionId(response);
    if (jSessionId != null) {
      logger.debug("received JSESSIONID: {}", jSessionId);
      return jSessionId;
    } else return null;
  }

  private String extractJsessionId(ResponseEntity<User> response) {
    String setCookieHeader = response.getHeaders().get("Set-Cookie").get(0);
    String sessionId = setCookieHeader.split(";")[0].split("=")[1];
    return sessionId;
  }

  @Override
  public void deleteSession() {
    sessionApi.sessionDelete();
  }
}
