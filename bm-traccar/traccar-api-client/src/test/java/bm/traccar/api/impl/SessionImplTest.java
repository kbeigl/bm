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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bm.traccar.api.ApiErrorCode;
import bm.traccar.api.ApiException;
import bm.traccar.generated.api.SessionApi;
import bm.traccar.generated.model.dto.User;
import bm.traccar.invoke.ApiClient;
import bm.traccar.invoke.auth.HttpBasicAuth;
import bm.traccar.invoke.auth.HttpBearerAuth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

class SessionImplTest {

  private SessionApi sessionApi;
  private ApiClient apiClient;
  private SessionImpl session;

  @BeforeEach
  void setUp() {
    sessionApi = mock(SessionApi.class);
    apiClient = mock(ApiClient.class);
    session = new SessionImpl(sessionApi, apiClient);
  }

  @Test
  void createSessionGetJsessionId_whenCookiePresent_returnsJsessionId() {
    HttpHeaders headers = new HttpHeaders();
    headers.put("Set-Cookie", List.of("JSESSIONID=node01abcxyz.node0; Path=/; HttpOnly"));
    ResponseEntity<User> response = ResponseEntity.ok().headers(headers).body(new User());

    when(sessionApi.sessionPostWithHttpInfo("user@domain.com", "pass")).thenReturn(response);

    String jSessionId = session.createSessionGetJsessionId("user@domain.com", "pass");
    assertEquals("node01abcxyz.node0", jSessionId);
  }

  @Test
  void createSessionGetJsessionId_whenCookieMissing_throwsApiException() {
    ResponseEntity<User> response = ResponseEntity.ok().body(new User());

    when(sessionApi.sessionPostWithHttpInfo("user@domain.com", "pass")).thenReturn(response);

    ApiException ex =
        assertThrows(
            ApiException.class,
            () -> session.createSessionGetJsessionId("user@domain.com", "pass"));
    assertEquals("Failed to create session for user: user@domain.com", ex.getMessage());
    assertEquals(ApiErrorCode.UNAUTHORIZED, ex.getErrorCode());
    assertEquals(401, ex.getStatusCode());
  }

  @Test
  void getSessionGetJsessionId_whenCookiePresent_returnsJsessionId() {
    HttpHeaders headers = new HttpHeaders();
    headers.put("Set-Cookie", List.of("JSESSIONID=node0token123.node0; Path=/; HttpOnly"));
    ResponseEntity<User> response = ResponseEntity.ok().headers(headers).body(new User());

    when(sessionApi.sessionGetWithHttpInfo("token123")).thenReturn(response);

    String jSessionId = session.getSessionGetJsessionId("token123");
    assertEquals("node0token123.node0", jSessionId);
  }

  @Test
  void getSessionGetJsessionId_whenCookieMissing_throwsApiException() {
    ResponseEntity<User> response = ResponseEntity.ok().body(new User());

    when(sessionApi.sessionGetWithHttpInfo("token123")).thenReturn(response);

    ApiException ex =
        assertThrows(ApiException.class, () -> session.getSessionGetJsessionId("token123"));
    assertEquals("Failed to get session for token: token123", ex.getMessage());
    assertEquals(ApiErrorCode.NOT_FOUND, ex.getErrorCode());
    assertEquals(404, ex.getStatusCode());
  }

  @Test
  void getSessionUser_whenNoAuthSet_throwsApiException() {
    when(apiClient.getAuthentication("ApiKey")).thenReturn(null);
    when(apiClient.getAuthentication("BasicAuth")).thenReturn(null);

    ApiException ex = assertThrows(ApiException.class, () -> session.getSessionUser());
    assertEquals(
        "No authentication set. Please call setBasicAuth() or setBearerToken() first.",
        ex.getMessage());
    assertEquals(ApiErrorCode.UNAUTHORIZED, ex.getErrorCode());
    assertEquals(401, ex.getStatusCode());
  }

  @Test
  void getSessionUser_whenBearerTokenSet_callsSessionGet() {
    HttpBearerAuth apiKey = mock(HttpBearerAuth.class);
    when(apiKey.getBearerToken()).thenReturn("my-bearer-token");
    when(apiClient.getAuthentication("ApiKey")).thenReturn(apiKey);

    User user = new User();
    user.setName("admin");
    when(sessionApi.sessionGet("my-bearer-token")).thenReturn(user);

    User result = session.getSessionUser();
    assertNotNull(result);
    assertEquals("admin", result.getName());
    verify(sessionApi).sessionGet("my-bearer-token");
  }

  @Test
  void getSessionUser_whenBasicAuthSet_callsSessionPost() {
    HttpBasicAuth basicAuth = mock(HttpBasicAuth.class);
    when(basicAuth.getUsername()).thenReturn("admin@domain.com");
    when(basicAuth.getPassword()).thenReturn("adminPass");
    when(apiClient.getAuthentication("ApiKey")).thenReturn(null);
    when(apiClient.getAuthentication("BasicAuth")).thenReturn(basicAuth);

    User user = new User();
    user.setName("admin");
    when(sessionApi.sessionPost("admin@domain.com", "adminPass")).thenReturn(user);

    User result = session.getSessionUser();
    assertNotNull(result);
    assertEquals("admin", result.getName());
    verify(sessionApi).sessionPost("admin@domain.com", "adminPass");
  }

  @Test
  void deleteSession_delegatesToSessionApi() {
    session.deleteSession();
    verify(sessionApi).sessionDelete();
  }
}
