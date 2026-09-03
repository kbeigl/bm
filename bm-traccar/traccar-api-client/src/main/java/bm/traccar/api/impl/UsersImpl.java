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
package bm.traccar.api.impl;

import bm.traccar.api.Api;
import bm.traccar.api.ApiException;
import bm.traccar.api.ApiHelper;
import bm.traccar.generated.api.UsersApi;
import bm.traccar.generated.model.dto.User;
import java.util.List;

/**
 * See <a href="https://www.traccar.org/api-reference/#tag/Users">UsersApi</a> with ApiService. <br>
 * In database lingo create, update, delete (CRUD) User in traccar datamodel using generated
 * entities.
 */
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

  /**
   * Get a single user by ID. Currently implemented using usersGet with userId parameter as the
   * generated API doesn't yet have a usersIdGet method corresponding to GET /users/{id}
   */
  @Override
  public User getUser(Long id) {
    // Use the userId parameter of usersGet to fetch a specific user
    // According to API docs, this should return the user with the given ID
    List<User> users = usersApi.usersGet(id.toString(), null, null, null);
    if (users != null && !users.isEmpty()) return users.get(0);
    // If not found, throw an exception matching the API behavior
    throw new ApiException("User with id " + id + " not found");
  }

  /**
   * workaround for getUserById missing in generated api and usersGet(userId) does not work as
   * expected. Check yaml, openapi generator and integer id.
   */
  @Override
  public User getUserById(String userId) {
    List<User> users = getAllUsers();
    for (User u : users) {
      if (u.getId().toString().equals(userId)) return u;
    }
    return null;
  }

  /**
   * Directly call the generated api method usersGet with all parameters. Use null for optional
   * parameters.
   *
   * @see UsersApi#usersGet(String, Integer, Integer, String)
   */
  @Override
  public List<User> getUsers(String userId, Integer limit, Integer offset, String keyword) {
    return usersApi.usersGet(userId, limit, offset, keyword);
  }

  // gets all users seen by the current user(?)
  @Override
  public List<User> getAllUsers() {
    return getUsers(null, null, null, null);
  }

  @Override
  public List<User> searchAllUsers(String keyword) {
    return getUsers(null, null, null, keyword);
  }

  @Override
  public User createUserWithCredentials(String name, String pwd, String mail, Boolean admin) {
    User user = new User();
    user.setName(name);
    user.setEmail(mail);
    user.setPassword(pwd);
    user.setAdministrator(admin);
    return createUser(user);
  }

  @Override
  public User createManagerWithCredentials(
      String name, String pwd, String mail, Integer userLimit) {
    if (userLimit == null || userLimit != 0) {
      User manager = new User();
      manager.setName(name);
      manager.setEmail(mail);
      manager.setPassword(pwd);
      manager.setAdministrator(false);
      manager.setUserLimit(userLimit);
      return createUser(manager);
    } else {
      throw new IllegalArgumentException(
          "userLimit must be -1 or a positive integer for a manager");
    }
  }

  @Override
  public User createManagerWithCredentials(
      String name, String pwd, String mail, Integer userLimit, Integer deviceLimit) {
    if (userLimit == null || userLimit != 0) {
      User manager = new User();
      manager.setName(name);
      manager.setEmail(mail);
      manager.setPassword(pwd);
      manager.setAdministrator(false);
      manager.setUserLimit(userLimit);
      manager.setDeviceLimit(deviceLimit);
      return createUser(manager);
    } else {
      throw new IllegalArgumentException(
          "userLimit must be -1 or a positive integer for a manager");
    }
  }

  //  @Deprecated // use getRole(user) instead
  //  public boolean isAdmin(User user) {}
  //  public boolean isManager(User user) {}
  //  public boolean isRegularUser(User user) {}

  @Override
  // top down check: Admin > Manager > Regular User
  public TraccarRole getTraccarRole(User user) {
    if (user == null) return TraccarRole.REG_USER; // may be misleading
    // VIRTUAL_ADMIN detection with hardcoded id 9000000000000000000L
    if (Boolean.TRUE.equals(user.getAdministrator()) && (user.getId() == 9000000000000000000L))
      return TraccarRole.VIRTUAL_ADMIN;
    if (Boolean.TRUE.equals(user.getAdministrator())) return TraccarRole.ADMIN;
    if (user.getUserLimit() != null && user.getUserLimit() != 0) return TraccarRole.MANAGER;
    return TraccarRole.REG_USER;
  }
}
