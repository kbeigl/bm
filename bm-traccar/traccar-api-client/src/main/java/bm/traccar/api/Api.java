package bm.traccar.api;

import bm.traccar.generated.model.dto.*;
import bm.traccar.invoke.auth.HttpBasicAuth;
import java.util.List;

/**
 * This interface defines the platonic OpenAPI methods provided by the (generated) API from the
 * traccar yaml definition. It can be replaced by any other implementation. Higher level methods are
 * implemented in the ApiService based on these platonic methods.
 *
 * <p>Java Developer friendly method names subdivided into sub-APIs. Hide REST implementation and
 * provide traccar entities for Client applications. Nested interfaces mimic the API method call URL
 * Every subAPI offers the existing methods of the http methods PUT, GET, POST, DELETE, etc. as
 * defined in the OpenAPI specification.
 *
 * <p>Note that the ApiException is a RuntimeException and it's catching is optional, but advised.
 */
public interface Api {

  // interface Auth and conection handling
  // authentication and other ApiClient methods
  void setBasicAuth(String mail, String password);

  // String whoAmI() - mail/name and password
  HttpBasicAuth getBasicAuth();

  void setBearerToken(String token);

  void setBasePath(String host);

  //     apiClient.getAuthentication();
  //     apiClient.getUsername(); // mail
  //     apiClient.getPassword(); // password
  //   return session with mail, if set

  Api.Server getServerApi();

  interface Server { // extends Api ?
    bm.traccar.generated.model.dto.Server getServerInfo(); // GET

    bm.traccar.generated.model.dto.Server updateServer(
        bm.traccar.generated.model.dto.Server server); // PUT
  }

  Api.Session getSessionApi();

  interface Session {

    User createSession(String mail, String password); // POST

    String createSessionGetJsessionId(String mail, String password); // POST

    User getSession(String token); // GET

    String getSessionGetJsessionId(String token); // GET

    void deleteSession(); // DELETE
  }

  Api.Users getUsersApi();

  interface Users {

    User createUser(User user) throws ApiException; // POST

    User updateUser(Long id, User user) throws ApiException; // PUT

    void deleteUser(Long id) throws ApiException; // DELETE

    // @Deprecated
    // List<User> getUsers(String userId) throws ApiException; // GET

    User getUserById(String userId) throws ApiException; // GET

    //   maybe getAllMyUsers and whoAmI()
    List<User> getAllUsers() throws ApiException; // GET

    // helper methods below based on generic calls above ------------
    // handle ApiException ?

    User createUserWithCredentials(String name, String pwd, String mail, Boolean admin);

    // *** apply default Implementation to avoid code duplication ***

    // Return single user by id (maps to GET /users/{id})
    //    default User getUserById(Long id) throws ApiException {
    //      if (id == null) return null;
    //      // delegate to the string-based getUserById (implemented by UsersImpl)
    //      return getUserById(id.toString());
    //    }

    // helper to check User Roles (does not map to REST API)
    boolean isAdmin(User user);

    boolean isManager(User user);

    boolean isRegularUser(User user);
  }

  Api.Devices getDevicesApi();

  interface Devices {

    Device createDevice(Device device); // POST

    Device updateDevice(Long deviceId, Device device); // PUT

    void deleteDevice(Long deviceId); // DELETE

    List<Device> getDevices(String userId); // GET

    // helper methods below based on generic calls above ------------

    // Device createDeviceForUser(String name, String uniqueId, String userMail);
  }

  Api.Permissions getPermissionsApi();

  interface Permissions {

    void createPermission(Permission permission); // POST

    void deletePermission(Permission permission); // DELETE
  }
}
