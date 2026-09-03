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

import bm.traccar.generated.api.UsersApi;
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

  HttpBasicAuth getBasicAuth();

  void setBearerToken(String token);

  void setBasePath(String host);

  String getAuthentication();

  Api.Server getServerApi();

  interface Server { // extends Api ?
    bm.traccar.generated.model.dto.Server getServerInfo(); // GET

    bm.traccar.generated.model.dto.Server updateServer(
        bm.traccar.generated.model.dto.Server server); // PUT
  }

  // Api.Session getSessionApi();

  interface Session {

    User createSession(String mail, String password); // POST

    String createSessionGetJsessionId(String mail, String password); // POST

    /**
     * Get the currently authenticated user based on the active authentication method.
     *
     * <p>This method automatically detects the current authentication type and retrieves the user
     * information accordingly:
     *
     * <ul>
     *   <li>For <b>BasicAuth</b>: Creates a session to retrieve user info (required by Traccar)
     *   <li>For <b>BearerToken</b>: Retrieves the session information associated with the token
     * </ul>
     *
     * <p><b>Session Creation:</b> When using BasicAuth, this method creates a session because
     * Traccar's GET /session endpoint only supports token query parameters or existing session
     * cookies - it does not support raw BasicAuth headers. When you access /session in a browser,
     * you already have a JSESSIONID cookie from logging into the web UI.
     *
     * <p><b>WebSocket Impact:</b> If you're using BasicAuth and have an active WebSocket connection
     * with the same credentials, calling this method creates a new session which may affect the
     * WebSocket. To avoid this, use a BearerToken instead.
     *
     * @return the current user based on active authentication (BasicAuth or BearerToken)
     * @throws ApiException if no authentication is set or if the user cannot be retrieved
     * @see <a href="https://www.traccar.org/api-reference/#tag/Session/operation/getSession">GET
     *     /session</a>
     */
    User getCurrentUser(); // GET without parameters

    User getSession(String token); // GET

    String getSessionGetJsessionId(String token); // GET

    void deleteSession(); // DELETE
  }

  Api.Users getUsersApi();

  interface Users {

    User createUser(User user) throws ApiException; // POST

    User updateUser(Long id, User user) throws ApiException; // PUT

    void deleteUser(Long id) throws ApiException; // DELETE

    /**
     * Get a single user by ID
     *
     * <p>Note: This method is implemented using usersGet with userId parameter as the generated API
     * doesn't yet have a usersIdGet method corresponding to GET /users/{id} as listed in the
     * OpenAPI specification <a
     * href="https://www.traccar.org/api-reference/#tag/Users/operation/getUsersId">getUsersId</a>.
     */
    User getUser(Long id) throws ApiException; // GET

    /**
     * @see UsersApi#usersGet(String, Integer, Integer, String)
     */
    List<User> getUsers(String userId, Integer limit, Integer offset, String keyword)
        throws ApiException;

    User getUserById(String userId) throws ApiException; // GET

    List<User> getAllUsers() throws ApiException; // GET

    /** Search keyword filter (searches name, email) */
    List<User> searchAllUsers(String keyword);

    // check role - userId - Can only be used by admin or manager users
    // clarify managedId lookup
    // List<User> getAllUsers(String userId);
    // List<User> searchAllUsers(String userId, String keyword);

    // helper methods below based on generic calls above ------------

    /** Create a user or admin with credentials */
    User createUserWithCredentials(String name, String pwd, String mail, Boolean admin);

    /** Create a manager user with credentials and user limit not 0 */
    User createManagerWithCredentials(String name, String pwd, String mail, Integer userLimit);

    /** Create a manager user with credentials, user limit not 0 and device limit */
    User createManagerWithCredentials(
        String name, String pwd, String mail, Integer userLimit, Integer deviceLimit);

    // helpers to check User Roles (does not map to REST API)

    /** User role enumeration for Traccar users */
    enum TraccarRole {
      REG_USER,
      MANAGER,
      ADMIN,
      VIRTUAL_ADMIN
    }

    /**
     * Get the traccar role of a user
     *
     * <p>Can be used in all *Api methods to check if the user has the right role to perform an
     * action. For example, only ADMIN and MANAGER users can create new users, while REG_USER can
     * only read their own data.
     *
     * @param user the user to check
     * @return the role of the user (REG_USER, MANAGER, ADMIN, or VIRTUAL_ADMIN)
     */
    TraccarRole getTraccarRole(User user);

    //    @Deprecated // use getRole(user) instead
    //    boolean isAdmin(User user);
    //    @Deprecated // use getRole(user) instead
    //    boolean isManager(User user);
    //    @Deprecated // use getRole(user) instead
    //    boolean isRegularUser(User user);
  }

  Api.Devices getDevicesApi();

  interface Devices {

    Device createDevice(Device device); // POST

    Device updateDevice(Long deviceId, Device device); // PUT

    void deleteDevice(Long deviceId); // DELETE

    /* currently no parameters and used by ITests for prototyping.
     * parameters can be added as needed,
     * but we can also use the generic getDevices and filter on the client side.
     * For example, we can add a userId parameter to get only devices of a specific user,
     * The latter is more flexible and does not require changes to the API,
     * but it can be less efficient if there are many devices.
     */
    List<Device> getDevices(); // GET

    // helper methods below based on generic calls above ------------

    // Device createDeviceForUser(String name, String uniqueId, String userMail);
  }

  // Api.Permissions getPermissionsApi();

  interface Permissions {

    void createPermission(Permission permission); // POST

    void deletePermission(Permission permission); // DELETE
  }
}
