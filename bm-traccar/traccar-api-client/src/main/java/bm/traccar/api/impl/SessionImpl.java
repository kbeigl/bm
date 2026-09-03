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
package bm.traccar.api.impl;

import bm.traccar.api.Api;
import bm.traccar.api.ApiException;
import bm.traccar.generated.api.SessionApi;
import bm.traccar.generated.model.dto.User;
import bm.traccar.invoke.ApiClient;
import bm.traccar.invoke.auth.HttpBasicAuth;
import bm.traccar.invoke.auth.HttpBearerAuth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

public class SessionImpl implements Api.Session {
  private static final Logger logger = LoggerFactory.getLogger(SessionImpl.class);
  private final SessionApi sessionApi;
  private final ApiClient apiClient;

  public SessionImpl(SessionApi sessionApi, ApiClient apiClient) {
    // public SessionImpl(SessionApi sessionApi, UsersApi usersApi, ApiClient apiClient) {
    // usersApi parameter kept for backwards compatibility but no longer used
    this.sessionApi = sessionApi;
    this.apiClient = apiClient;
  }

  @Override
  public User createSession(String mail, String password) {
    return sessionApi.sessionPost(mail, password);
  }

  @Override
  public String createSessionGetJsessionId(String mail, String password) {
    logger.debug(" create session for {}", mail); // don't log password
    ResponseEntity<User> response = sessionApi.sessionPostWithHttpInfo(mail, password);

    // response.getHeaders().forEach((key, value) -> logger.debug("Header '{}': {}", key, value));
    User sessionUser = response.getBody(); // to avoid "unused" warning
    logger.debug("received session user: {}", sessionUser);

    String jSessionId = extractJsessionId(response);
    if (jSessionId != null) {
      logger.debug("received JSESSIONID: {}", jSessionId);
      return jSessionId;
    } else {
      logger.error("Failed to create session for user: {}", mail);
      return null;
    }
  }

  /**
   * Get the currently authenticated user based on the active authentication method.
   *
   * <p>This method detects the current authentication type and retrieves the user information:
   *
   * <ul>
   *   <li>For <b>BasicAuth</b>: Creates a session to retrieve user info (Traccar's GET /session
   *       requires either a token parameter or existing session cookie, not raw BasicAuth)
   *   <li>For <b>BearerToken</b>: Retrieves the session information associated with the token
   * </ul>
   *
   * <p><b>Note:</b> For BasicAuth users, this method creates a session. This is necessary because
   * Traccar's GET /session endpoint doesn't support BasicAuth headers alone - it requires either a
   * token query parameter or an existing JSESSIONID cookie.
   */
  @Override
  public User getCurrentUser() {
    // Check which authentication is active
    HttpBearerAuth apiKey = (HttpBearerAuth) apiClient.getAuthentication("ApiKey");
    HttpBasicAuth basicAuth = (HttpBasicAuth) apiClient.getAuthentication("BasicAuth");

    String bearerToken = apiKey != null ? apiKey.getBearerToken() : null;
    String username = basicAuth != null ? basicAuth.getUsername() : null;
    String password = basicAuth != null ? basicAuth.getPassword() : null;

    if (bearerToken != null) {
      // Using Bearer Token authentication - get session info for the token
      logger.debug("Getting current user via Bearer Token");
      return sessionApi.sessionGet(bearerToken);
    } else if (username != null && password != null) {
      // Using Basic Auth - create a session to get user info
      // Traccar's GET /session only works with token parameter or existing session cookie,
      // not with BasicAuth headers alone
      logger.debug("Getting current user via Basic Auth (creating session ! ): {}", username);
      return sessionApi.sessionPost(username, password);
    } else {
      throw new ApiException(
          "No authentication set. Please call setBasicAuth() or setBearerToken() first.");
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
    // if the header is absent > NullPointerException
    List<String> cookies = response.getHeaders().get("Set-Cookie");
    if (cookies == null || cookies.isEmpty()) return null;
    String[] parts = cookies.get(0).split(";")[0].split("=", 2);
    return parts.length == 2 ? parts[1] : null;
  }

  @Override
  public void deleteSession() {
    sessionApi.sessionDelete();
  }
}
