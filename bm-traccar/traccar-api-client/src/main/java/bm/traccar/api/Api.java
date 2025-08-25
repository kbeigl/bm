package bm.traccar.api;

import bm.traccar.generated.model.dto.*;
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

  // interface Auth {  ..
  // authentication methods -------------------------------
  void setBasicAuth(String mail, String password);

  void setBearerToken(String token);

  // String whoAmI();
  // 	return session with mail, if set

  interface Session {

    User createSession(String mail, String password); // POST

    String createSessionGetJsessionId(String mail, String password); // POST

    User getSession(String token); // GET

    String getSessionGetJsessionId(String token); // GET

    void deleteSession(); // DELETE
  }

  interface Users {

    User createUser(User user) throws ApiException; // POST

    User updateUser(Long id, User user) throws ApiException; // PUT

    void deleteUser(Long id) throws ApiException; // DELETE

    List<User> getUsers(String userId) throws ApiException; // GET

    // helper methods below based on generic calls above ------------
    // handle ApiException ?

    User createUserWithCredentials(String usr, String pwd, String mail, Boolean admin);
  }

  interface Devices {

    Device createDevice(Device device); // POST

    Device updateDevice(Long deviceId, Device device); // PUT

    void deleteDevice(Long deviceId); // DELETE

    List<Device> getDevices(String userId); // GET

    // helper methods below based on generic calls above ------------

    // Device createDeviceForUser(String name, String uniqueId, String userMail);
  }
}
