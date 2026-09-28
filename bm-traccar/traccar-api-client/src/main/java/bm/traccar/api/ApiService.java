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

import bm.traccar.invoke.ApiClient;
import bm.traccar.invoke.auth.HttpBasicAuth;
import bm.traccar.invoke.auth.HttpBearerAuth;
import org.springframework.stereotype.Service;

/**
 * This ApiService class implements the actual REST calls and returns entities, actually DTOs. The
 * ApiService is wrapping the generated ApiClient acting as sender and receiver.
 *
 * <p>Aspect is applied for cross cutting handling of Service..
 *
 * <p>The Api and ApiService are pure REST clients and do not handle any WebSocket connections nor
 * Camel Routing as these are separate concerns!
 */
@Service // ("traccarApiService")
public class ApiService implements Api {

  private final ApiClient apiClient;

  protected ApiClient getApiClient() {
    return this.apiClient;
  }

  /**
   * Set authentication to BasicAuth with mail (i.e. identity) and password. Since traccar accepts
   * only one authentication the BearerToken is nulled.
   */
  @Override
  public void setBasicAuth(String mail, String password) {
    apiClient.setBearerToken((String) null);
    apiClient.setUsername(mail);
    apiClient.setPassword(password);
  }

  /**
   * Return current BasicAuth or null if not set. BasicAuth provides name/pw currently logged in.
   */
  @Override
  public HttpBasicAuth getBasicAuth() { // getLoginUser()
    return (HttpBasicAuth) apiClient.getAuthentication("BasicAuth");
  }

  /**
   * Set authentication to ApiKey with token. Since traccar accepts only one authentication the
   * BasicAuth is nulled.
   */
  @Override
  public void setBearerToken(String token) {
    apiClient.setUsername(null);
    apiClient.setPassword(null);
    apiClient.setBearerToken(token);
  }

  @Override
  public void setBasePath(String host) {
    apiClient.setBasePath(host);
  }

  /**
   * Return which authentication is currently active. Since traccar accepts only one authentication,
   * either ApiKey.BearerToken or BasicAuth will be set.
   *
   * <p>Currently used for testing and logging purposes. Could be refined for better usage in
   * scenario code.
   *
   * <p>Caution: The BasicAuth password is returned in plain text. Use with care!
   */
  @Override
  public String getAuthentication() {
    HttpBearerAuth apiKey = (HttpBearerAuth) apiClient.getAuthentication("ApiKey");
    HttpBasicAuth basicAuth = (HttpBasicAuth) apiClient.getAuthentication("BasicAuth");

    String bearerToken = apiKey != null ? apiKey.getBearerToken() : null;
    String username = basicAuth != null ? basicAuth.getUsername() : null;
    String password = basicAuth != null ? basicAuth.getPassword() : null;

    if (bearerToken != null) return "ApiKey.BearerToken=" + bearerToken;
    if (username != null || password != null) return "BasicAuth=" + username + "/ *****";
    // dont reveal password
    return null;
  }

  /* constructor injection makes dependencies explicit and objects immutable. */
  public ApiService(
      ApiClient apiClient,
      Api.Server server,
      Api.Session session,
      Api.Users users,
      Api.Devices devices,
      Api.Permissions permissions) {
    this.apiClient = apiClient;
    this.server = server;
    this.session = session;
    this.users = users;
    this.devices = devices;
    this.permissions = permissions;
  }

  // sub-APIs are only accessible via the Api getters (single public entry point)
  private final Api.Server server;
  private final Api.Session session;
  private final Api.Users users;
  private final Api.Devices devices;
  private final Api.Permissions permissions;

  @Override
  public Api.Server getServerApi() {
    return server;
  }

  @Override
  public Api.Session getSessionApi() {
    return session;
  }

  @Override
  public Api.Users getUsersApi() {
    return users;
  }

  @Override
  public Api.Devices getDevicesApi() {
    return devices;
  }

  @Override
  public Api.Permissions getPermissionsApi() {
    return permissions;
  }
}
