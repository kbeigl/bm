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

  @Override
  public List<User> getUsers(String userId) {
    return usersApi.usersGet(userId);
  }

  @Override
  public User createUserWithCredentials(String usr, String pwd, String mail, Boolean admin) {
    User user = new User();
    user.setName(usr);
    user.setEmail(mail);
    user.setPassword(pwd);
    user.setAdministrator(admin);
    return createUser(user);
  }
}
