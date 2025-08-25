package bm.traccar.api;

import bm.traccar.generated.api.DevicesApi;
import bm.traccar.generated.api.SessionApi;
import bm.traccar.generated.api.UsersApi;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.User;
import bm.traccar.invoke.ApiClient;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
  private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

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
  @Override
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
  @Override
  public void setBearerToken(String token) {
    apiClient.setUsername(null);
    apiClient.setPassword(null);
    apiClient.setBearerToken(token);
  }

  @Autowired private SessionApi sessionApi;

  Session session =
      new Api.Session() {

        // create session
        /**
         * This method always creates a new session for the user, i.e. resets the session, which
         * plays a role, if the sessionId is passed to other consumers. In the simplest case the
         * wscat command.
         *
         * <p>see {@link SessionApi#sessionPost(mail, password)}
         */
        @Override
        public User createSession(String mail, String password) {
          return sessionApi.sessionPost(mail, password);
        }

        /** see {@link SessionApi#sessionPostWithHttpInfo(mail, password)} */
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

        // fetch session
        @Override
        public User getSession(String token) {
          return sessionApi.sessionGet(token);
        }

        @Override
        public String getSessionGetJsessionId(String token) {

          /* the current JSESSIONID should probably be sent in a Cookie header !!
           *
           * GET /api/session: This is the status check action.
           * Its purpose is to retrieve information about the current, active session.
           * For this request to be successful, you must already be authenticated
           * and include a valid JSESSIONID in your request headers.
           * The server will use the JSESSIONID you provided to look up your session details
           * and return the user information. It does not generate a new JSESSIONID.
           */

          ResponseEntity<User> response = sessionApi.sessionGetWithHttpInfo(token);
          String jSessionId = extractJsessionId(response);
          if (jSessionId != null) {
            logger.debug("received JSESSIONID: {}", jSessionId);
            return jSessionId;
          } else return null;
        }

        private String extractJsessionId(ResponseEntity<User> response) {
          // optimistic
          String setCookieHeader = response.getHeaders().get("Set-Cookie").get(0);
          // maybe return setCookieHeader directly ?
          String sessionId = setCookieHeader.split(";")[0].split("=")[1];
          return sessionId;
        }

        // close session
        @Override
        public void deleteSession() {
          sessionApi.sessionDelete();
        }

        // TODO, required ?
        // @Override
        // public void deleteSessionWithHttpInfo() {
        //   ResponseEntity<Void> response = sessionApi.sessionDeleteWithHttpInfo();
        // }
      };

  @Autowired private UsersApi usersApi;

  Users users =
      new Api.Users() {

        /** see {@link UsersApi#usersPost(User)} */
        @Override
        public User createUser(User user) {
          return usersApi.usersPost(user);
        }

        /**
         * Due to API inconsistency the id is Long here, but Integer in the generated code. Therfor
         * the developer has to provide a Long, which needs to be in the Integer range.
         *
         * <p>see {@link UsersApi#usersIdPut(Integer, User)}
         */
        @Override
        public User updateUser(Long id, User user) {
          Integer integerId = ApiHelper.toInt(id);
          return usersApi.usersIdPut(integerId, user);
        }

        /**
         * Due to API inconsistency the id is Long here, but Integer in the generated code. Therfor
         * the developer has to provide a Long, which needs to be in the Integer range.
         *
         * see {@link UsersApi#usersIdDelete(Integer) */
        @Override
        public void deleteUser(Long id) {
          Integer integerId = ApiHelper.toInt(id);
          usersApi.usersIdDelete(integerId);
        }

        /** see {@link UsersApi#usersGet(String)} */
        @Override
        public List<User> getUsers(String userId) {
          return usersApi.usersGet(userId);
        }

        // helper methods below based on generic calls above

        @Override
        public User createUserWithCredentials(String usr, String pwd, String mail, Boolean admin) {
          User user = new User();
          user.setName(usr);
          user.setEmail(mail);
          user.setPassword(pwd);
          user.setAdministrator(admin);
          // try {
          return createUser(user);
          // } catch (Exception e) {
          //   System.err.println("Error creating user: " + e.getMessage());
          //   return null; // or handle exception as needed
          // }
        }
        /*
         * add convenience methods for Entities User createUserWithCredentials(String usr, String pwd, String mail) { add
         * ROLES Initial simple implementation. User getSuperUser()
         */
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

        /**
         * Note that the device is created for the User set in the authentication.
         *
         * <p>see {@link DevicesApi#devicesPost(Device)}
         */
        @Override
        public Device createDevice(Device device) {
          return devicesApi.devicesPost(device);
        }

        /** see {@link DevicesApi#devicesIdPut(Integer, Device)} */
        @Override
        public Device updateDevice(Long deviceId, Device device) {
          Integer integerId = ApiHelper.toInt(deviceId);
          return devicesApi.devicesIdPut(integerId, device);
        }

        /** see {@link DevicesApi#devicesIdDelete(Integer)} */
        @Override
        public void deleteDevice(Long deviceId) {
          Integer integerId = ApiHelper.toInt(deviceId);
          devicesApi.devicesIdDelete(integerId);
        }

        /** see {@link DevicesApi#devicesGet(Boolean, Integer, Integer, String)} */
        @Override
        public List<Device> getDevices(String userId) {
          return devicesApi.devicesGet(null, null, null, userId);
        }
      };
}
