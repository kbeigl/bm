package bm.traccar.api;

import bm.traccar.generated.model.dto.*;
import java.util.List;

/**
 * This interface defines the platonic OpenAPI methods provided by the (generated) API from the
 * traccar yaml definition. It can be replaced by any other implementation. Higher level methods are
 * implemented in the ApiService based on these platonic methods.
 *
 * <p>Java Developer friendly method names subdivided into sub-APIs. Hide REST implementation and
 * provide traccar entities for Client application. Nested interfaces mimic the API method call URL
 * Every subAPI offers the existing methods of the http methods PUT, GET, POST, DELETE, etc. as
 * defined in the OpenAPI specification.
 *
 * <p>Note that the ApiException is a RuntimeException and it's catching is optional, but advised.
 */
public interface Api {

  // authentication methods
  void setBasicAuth(String mail, String password);

  void setBearerToken(String token);

  // singular according to spec
  interface Session {
    @Deprecated
    User createSession(String mail, String password);

    User createAndOpenSession(String mail, String password); // POST
  }

  interface Users {

    User createUser(User user) throws ApiException; // POST

    User updateUser(Integer id, User user) throws ApiException; // PUT

    void deleteUser(Integer id) throws ApiException; // DELETE

    List<User> getUsers(String userId) throws ApiException; // GET

    // helper methods below based on generic calls above ------------
    // handle ApiException ?

    User createUserWithCredentials(String usr, String pwd, String mail, Boolean admin);
  }

  interface Devices {

    Device createDevice(Device device); // POST

    Device updateDevice(int newDeviceId, Device newDevice); // PUT

    void deleteDevice(int newDeviceId); // DELETE

    List<Device> getDevices(String userId); // GET

    // helper methods below based on generic calls above ------------

    // Device createDeviceForUser(String name, String uniqueId, String userMail);
  }
}
