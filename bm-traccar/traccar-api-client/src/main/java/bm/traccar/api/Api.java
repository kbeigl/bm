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
 *
 * <p>Note that the ApiException is a RuntimeException and it's catching is optional, but advised.
 */
public interface Api {

  interface Users {

    User createUser(User user) throws ApiException;

    User updateUser(Integer id, User user) throws ApiException;

    void deleteUser(Integer id) throws ApiException;

    List<User> getUsers(String userId) throws ApiException;

    // helper methods below based on generic calls above

    User createUserWithCredentials(String usr, String pwd, String mail); // throws ApiException ?
  }

  interface Devices {

    List<Device> getDevices(String userId);
  }
}
