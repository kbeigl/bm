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

import bm.traccar.api.impl.DevicesImpl;
import bm.traccar.api.impl.PermissionsImpl;
import bm.traccar.api.impl.ServerImpl;
import bm.traccar.api.impl.SessionImpl;
import bm.traccar.api.impl.UsersImpl;
import bm.traccar.generated.api.DevicesApi;
import bm.traccar.generated.api.PermissionsApi;
import bm.traccar.generated.api.ServerApi;
import bm.traccar.generated.api.SessionApi;
import bm.traccar.generated.api.UsersApi;
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
    HttpBasicAuth basicAuth = (HttpBasicAuth) apiClient.getAuthentication("BasicAuth");
    if (basicAuth != null) {
      return basicAuth;
    }
    return null;
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
      UsersApi usersApi,
      SessionApi sessionApi,
      PermissionsApi permissionsApi,
      DevicesApi devicesApi,
      ServerApi serverApi) {

    this.apiClient = apiClient;
    // sub interface  = new sub interface implementation (wrapping generated API)
    this.users = new UsersImpl(usersApi);
    this.session = new SessionImpl(sessionApi, apiClient);
    this.permissions = new PermissionsImpl(permissionsApi);

    this.server = new ServerImpl(serverApi);
    this.devices = new DevicesImpl(devicesApi);
  }

  // currently public for convenience (api.users)
  // might be privated ..
  public final Api.Users users;
  public final Api.Session session;
  public final Api.Permissions permissions;

  public final Api.Devices devices;
  public final Api.Server server;

  // consider removing these access methods

  // @Override public Api.Session getSessionApi() { return session; }
  // @Override public Api.Permissions getPermissionsApi() { return permissions; }

  @Override
  public Api.Users getUsersApi() {
    return users;
  }

  @Override
  public Api.Devices getDevicesApi() {
    return devices;
  }

  @Override
  public Api.Server getServerApi() {
    return server;
  }
}
