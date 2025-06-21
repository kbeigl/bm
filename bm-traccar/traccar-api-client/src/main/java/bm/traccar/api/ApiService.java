package bm.traccar.api;

import bm.traccar.generated.api.DevicesApi;
import bm.traccar.generated.api.SessionApi;
import bm.traccar.generated.api.UsersApi;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.User;
import bm.traccar.invoke.ApiClient;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * This ApiService class implements the actual REST calls and returns entities, actually DTOs. The
 * ApiService is wrapping the generated ApiClient acting as sender and receiver.
 *
 * <p>Currently this Service in invoking the simplest method for each endpoint. i.e. usersGet not
 * usersGetWithHttpInfo or usersGetWithResponseSpec. No special handling for blocking- or reactive
 * clients for traccar (?)
 *
 * <p>Aspect is applied for cross cutting handling of Service..
 */
@Service // ("traccarApiService")
public class ApiService implements Api {

  // Exception handling -> AOP for all methods
  // analog to testing with ClientExceptionHandler

  @Autowired private ApiClient apiClient;

  protected ApiClient getApiClient() {
    return this.apiClient;
  }

  /**
   * Set authentication to BasicAuth with mail (i.e. identity) and password. Since traccar accepts
   * only one authentication the BearerToken is nulled.
   */
  public void setBasicAuth(String mail, String password) {
    String nul = null;
    apiClient.setBearerToken(nul);
    apiClient.setUsername(mail);
    apiClient.setPassword(password);
  }

  /**
   * Set authentication to ApiKey with token. Since traccar accepts only one authentication the
   * BasicAuth is nulled.
   */
  public void setBearerToken(String token) {
    apiClient.setUsername(null);
    apiClient.setPassword(null);
    apiClient.setBearerToken(token);
  }

  @Autowired private SessionApi sessionApi;

  //  experimental see SessionIT ---------------
  public User createSession(String mail, String password) {
    return sessionApi.sessionPost(mail, password);
  }

  public ResponseEntity<User> createSessionPostWithHttpInfo(String mail, String password) {
    return sessionApi.sessionPostWithHttpInfo(mail, password);
  }

  //  public ResponseSpec createSessionPostWithResponseSpec(String mail, String password) {
  //    return sessionApi.sessionPostWithResponseSpec(mail, password);
  //  }
  //  -------------------------------------------

  Session session =
      new Api.Session() {
        /** see {@link SessionApi#sessionPost(mail, password)} */
        @Override
        public User createSession(String mail, String password) {
          return sessionApi.sessionPost(mail, password);
        }
      };

  @Autowired private UsersApi usersApi;

  Users users =
      new Api.Users() {

        /** see {@link UsersApi#usersPost(User)} */
        @Override
        public User createUser(User user) {
          return usersApi.usersPost(user);
        }

        /** see {@link UsersApi#usersIdPut(Integer, User)} */
        @Override
        public User updateUser(Integer id, User user) {
          return usersApi.usersIdPut(id, user);
        }

        /** see {@link UsersApi#usersIdDelete(Integer) */
        @Override
        public void deleteUser(Integer id) {
          usersApi.usersIdDelete(id);
        }

        /** see {@link UsersApi#usersGet(String)} */
        @Override
        public List<User> getUsers(String userId) {
          return usersApi.usersGet(userId);
        }

        // helper methods below based on generic calls above

        @Override
        public User createUserWithCredentials(String usr, String pwd, String mail) {
          User user = new User();
          user.setName(usr);
          user.setEmail(mail);
          user.setPassword(pwd);
          return createUser(user); // throws ApiException
        }

        /*
         * add ROLES Initial simple implementation. Assuming that superAdmin is always first, i.e. check users.get(0)
         * against name from app.prop file.
         * @Override public User getSuperUser() { List<User> users = getAllUsers(null); if (users.size() > 0) { User
         * admin = users.get(0); if (admin.getName().equals(usr)) return admin; } return null; }
         */

      };

  @Autowired private DevicesApi devicesApi;
  Devices devices =
      new Api.Devices() {

        @Override
        public List<Device> getDevices(String userId) {
          return devicesApi.devicesGet(null, null, null, userId);
        }
      };

  /*
   * add convenience methods for Entities User createUserWithCredentials(String usr, String pwd, String mail) { add
   * ROLES Initial simple implementation. User getSuperUser()
   */
}
