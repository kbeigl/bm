package bm.traccar.api;

import bm.traccar.api.impl.DevicesImpl;
import bm.traccar.api.impl.ServerImpl;
import bm.traccar.api.impl.SessionImpl;
import bm.traccar.api.impl.UsersImpl;
import bm.traccar.generated.api.DevicesApi;
import bm.traccar.generated.api.ServerApi;
import bm.traccar.generated.api.SessionApi;
import bm.traccar.generated.api.UsersApi;
import bm.traccar.invoke.ApiClient;
import bm.traccar.invoke.auth.HttpBasicAuth;
import org.springframework.beans.factory.annotation.Autowired;
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
  // private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

  @Autowired private ApiClient apiClient;

  protected ApiClient getApiClient() {
    return this.apiClient;
  }

  /**
   * Set authentication to BasicAuth with mail (i.e. identity) and password. Since traccar accepts
   * only one authentication the BearerToken is nulled.
   */
  @Override
  public void setBasicAuth(String mail, String password) {
    String nul = null;
    apiClient.setBearerToken(nul);
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

  /*
   * constructor injection makes dependencies explicit and objects immutable.
   */
  public ApiService(
      SessionApi sessionApi, UsersApi usersApi, DevicesApi devicesApi, ServerApi serverApi) {
    // sub interface  = new sub interface implementation (wrapping generated API)
    this.session = new SessionImpl(sessionApi);
    this.users = new UsersImpl(usersApi);
    this.devices = new DevicesImpl(devicesApi);
    this.server = new ServerImpl(serverApi);
  }

  public final Api.Users users;
  public final Api.Devices devices;
  public final Api.Session session;
  public final Api.Server server;

  @Override
  public Api.Users getUsersApi() {
    return users;
  }

  @Override
  public Api.Devices getDevicesApi() {
    return devices;
  }

  @Override
  public Api.Session getSessionApi() {
    return session;
  }

  @Override
  public Api.Server getServerApi() {
    return server;
  }
}
