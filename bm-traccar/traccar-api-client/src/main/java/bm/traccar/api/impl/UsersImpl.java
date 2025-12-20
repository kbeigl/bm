package bm.traccar.api.impl;

import bm.traccar.api.Api;
import bm.traccar.api.ApiHelper;
import bm.traccar.generated.api.UsersApi;
import bm.traccar.generated.model.dto.User;
import java.util.List;

public class UsersImpl implements Api.Users {
  private final UsersApi usersApi;

  public UsersImpl(UsersApi usersApi) {
    this.usersApi = usersApi;
  }

  @Override
  public User createUser(User user) {
    return usersApi.usersPost(user);
  }

  @Override
  public User updateUser(Long id, User user) {
    Integer integerId = ApiHelper.toInt(id);
    return usersApi.usersIdPut(integerId, user);
  }

  @Override
  public void deleteUser(Long id) {
    Integer integerId = ApiHelper.toInt(id);
    usersApi.usersIdDelete(integerId);
  }

  // getCurrentUser() whoAmI()

  /* @deprecated use getUserById or getAllUsers */
  @Deprecated
  public List<User> getUsers(String userId) {
    return usersApi.usersGet(userId);
  }

  /**
   * workaround for getUserById missing in generated api and usersGet(userId) does not work as
   * expected. Check yaml, openapi generator and integer id.
   */
  @Override
  public User getUserById(String userId) {
    List<User> users = usersApi.usersGet(null);
    for (User u : users) {
      if (u.getId().toString().equals(userId)) {
        return u;
      }
    }
    return null;
  }

  @Override
  public List<User> getAllUsers() {
    return usersApi.usersGet(null);
  }

  // getCurrentUser()

  @Override
  public User createUserWithCredentials(String name, String pwd, String mail, Boolean admin) {
    User user = new User();
    user.setName(name);
    user.setEmail(mail);
    user.setPassword(pwd);
    user.setAdministrator(admin);
    return createUser(user);
  }

  // helper to check User Roles
  @Override
  public boolean isAdmin(User user) {
    return user != null && Boolean.TRUE.equals(user.getAdministrator());
  }

  @Override
  public boolean isManager(User user) {
    if (user.getUserLimit() != null && user.getUserLimit() != 0) return true;
    return false;
  }

  @Override
  public boolean isRegularUser(User user) {
    return !isAdmin(user) && !isManager(user);
  }
}
