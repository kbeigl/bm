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
import bm.traccar.api.ApiErrorCode;
import bm.traccar.api.ApiException;
import bm.traccar.api.ApiHelper;
import bm.traccar.generated.api.UsersApi;
import bm.traccar.generated.model.dto.User;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * See <a href="https://www.traccar.org/api-reference/#tag/Users">UsersApi</a> with ApiService. <br>
 * In database lingo create, update, delete (CRUD) User in traccar datamodel using generated
 * entities.
 */
@Component
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
  //  @Override
  //  public User getUser(Long id) {
  //    // Use the userId parameter of usersGet to fetch a specific user
  //    // According to API docs, this should return the user with the given ID
  //    List<User> users = usersApi.usersGet(id.toString(), null, null, null);
  //    if (users != null && !users.isEmpty()) return users.get(0);
  //    // If not found, throw an exception matching the API behavior
  //    throw new ApiException("User with id " + id + " not found");
  //  }

  /**
   * workaround for getUser missing in generated api and usersGet(userId) does not work as expected.
   * Check yaml, openapi generator and integer id.
   *
   * <p>Search is now on client side, not server side. This is not efficient for large user lists.
   * Use with caution. Apply limit and offset if possible. The generated usersGet(userId) does not
   * work as expected, so we fetch all users and filter by ID.
   */
  @Override
  public User getUser(Long userId) {
    List<User> users = getUsers(null, null, null, null);
    if (users != null) {
      for (User u : users) {
        if (Objects.equals(u.getId(), userId)) return u;
      }
    }
    throw new ApiException(ApiErrorCode.NOT_FOUND, "User with id " + userId + " not found");
  }

  /**
   * Directly call the generated api method usersGet with all parameters. Use null for optional
   * parameters.
   *
   * @see UsersApi#usersGet(String, Integer, Integer, String)
   */
  //  userId  Can only be used by admin or manager users
  //  limit   Limit the number of returned results
  //  offset  Offset for pagination
  //  keyword Search keyword filter (searches name, email)
  @Override
  public List<User> getUsers(String userId, Integer limit, Integer offset, String keyword) {
    return usersApi.usersGet(userId, limit, offset, keyword);
  }

  @Override
  public List<User> searchAllUsers(String keyword) {
    return getUsers(null, null, null, keyword);
  }

  @Override
  public User createUserWithCredentials(String name, String pwd, String mail) {
    return createUserWithCredentials(name, pwd, mail, false, null, null);
  }

  @Override
  public User createAdminWithCredentials(String name, String pwd, String mail) {
    return createUserWithCredentials(name, pwd, mail, true, null, null);
  }

  @Override
  public User createManagerWithCredentials(
      String name, String pwd, String mail, Integer userLimit) {
    return createManagerWithCredentials(name, pwd, mail, userLimit, null);
  }

  @Override
  public User createManagerWithCredentials(
      String name, String pwd, String mail, Integer userLimit, Integer deviceLimit) {
    if (userLimit != null && userLimit == 0) {
      throw new IllegalArgumentException(
          "userLimit must be -1 or a positive integer for a manager");
    }
    return createUserWithCredentials(name, pwd, mail, false, userLimit, deviceLimit);
  }

  private User createUserWithCredentials(
      String name, String pwd, String mail, Boolean admin, Integer userLimit, Integer deviceLimit) {
    User user = new User();
    user.setName(name);
    user.setEmail(mail);
    user.setPassword(pwd);
    user.setAdministrator(admin);
    user.setUserLimit(userLimit);
    user.setDeviceLimit(deviceLimit);
    return createUser(user);
  }

  @Override
  // top down check: virtual Admin > Admin > Manager > Regular User
  public TraccarRole getTraccarRole(User user) {
    if (user == null) return TraccarRole.REG_USER; // may be misleading
    // VIRTUAL_ADMIN detection with hardcoded id 9000000000000000000L
    // i.e. Traccar server implemetation detail, not in API spec. Use with caution.
    if (Boolean.TRUE.equals(user.getAdministrator()) && (user.getId() == 9000000000000000000L))
      return TraccarRole.VIRTUAL_ADMIN;
    if (Boolean.TRUE.equals(user.getAdministrator())) return TraccarRole.ADMIN;
    if (user.getUserLimit() != null && user.getUserLimit() != 0) return TraccarRole.MANAGER;
    return TraccarRole.REG_USER;
  }
}
