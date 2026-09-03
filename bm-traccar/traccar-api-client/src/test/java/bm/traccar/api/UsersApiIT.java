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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.api.Api.Users.TraccarRole;
import bm.traccar.generated.model.dto.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test <code>api/users</code> methods, @see <a
 * href="https://www.traccar.org/api-reference/#tag/Users">UsersApi</a> with ApiService. <br>
 * In database lingo create, update, delete (CRUD) User in traccar datamodel using generated
 * entities.
 */
public class UsersApiIT extends BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(UsersApiIT.class);

  // @Test
  public void threeTodoTests() {
    // role testing -----------------------------
    // api.users.getRole(adminUser));
    // user lists -------------------------------
    // check role - userId - Can only be used by admin or manager users
    // clarify managedId lookup (table!)
    // List<User> getAllUsers(String userId);
    // List<User> searchAllUsers(String userId, String keyword);
    // search tests -----------------------------
    // Search keyword filter (searches name, email) */
    // <column name="email"> <constraints nullable="false" unique="true" />
    // List<User> searchAllUsers(String keyword);
  }

  /**
   * Test the getUsers(null, null, null, null) method for different user roles (superAdmin, admin,
   * regular user, manager).
   */
  @Test
  public void getUsersTest() {

    api.setBearerToken(virtualAdmin);
    logger.info("Authenticated: {}", api.getAuthentication());
    listUsers(api.users.getUsers(null, null, null, null));

    // regular user does not have a user list. See Settings -> Users.
    // Only Admins and Managers can see the user list.
    api.setBasicAuth(userMail, userPassword);
    logger.info("Authenticated: {}", api.getAuthentication());
    listUsers(api.users.getUsers(null, null, null, null));

    api.setBasicAuth(adminMail, adminPassword);
    logger.info("Authenticated: {}", api.getAuthentication());
    listUsers(api.users.getUsers(null, null, null, null));

    // move to BaseIntegrationTest to be used by other tests ?
    String mgrMail = "manager@domain.com", mgrPassword = "manager";
    User manager = api.users.createManagerWithCredentials("manager", mgrPassword, mgrMail, 4);
    logger.info("manager user created with id {}", manager.getId());

    api.setBasicAuth(mgrMail, mgrPassword);
    logger.info("Authenticated: {}", api.getAuthentication());
    listUsers(api.users.getUsers(null, null, null, null));

    // now the admin can also see the manager
    api.setBasicAuth(adminMail, adminPassword);
    logger.info("Authenticated: {}", api.getAuthentication());
    listUsers(api.users.getUsers(null, null, null, null));

    // clean up this test (only)
    logger.info("Deleted User {} (id={})", manager.getEmail(), manager.getId());
    api.users.deleteUser(manager.getId());
  }

  // managerUserLimit conclusion
  // the created users by the manager do NOT belong to the manager, but to the admin user.
  // login as admin to see created users AND devices
  // the userLimit is NOT enforced via REST API createUser call as manager.
  // clarify via permissions management !!
  @Test
  public void managerUserLimit() {
    // usersApi.isAdmin(....);
    // create manager with user limit AS ADMIN
    api.setBasicAuth(adminMail, adminPassword);
    // explicitly use interface method to test ApiService delegation
    // usersApi = api.getUsersApi();
    String mgrPassword = "manager";

    User manager =
        api.users.createManagerWithCredentials("manager", mgrPassword, "manager@domain.com", 2);

    // create regular users AS MANAGER
    User user1, user2, user3 = null;

    if (api.users.getTraccarRole(manager) == TraccarRole.MANAGER) {
      // if (api.users.isManager(manager)) {
      api.setBasicAuth(manager.getName(), mgrPassword);
      user1 = api.users.createUserWithCredentials("user-1", "user-1-pw", "user-1-mail", false);
      user2 = api.users.createUserWithCredentials("user-2", "user-2-pw", "user-2-mail", false);
      // This should fail - limit reached
      try {
        user3 = api.users.createUserWithCredentials("user-3", "user-3-pw", "user-3-mail", false);
      } catch (ApiException e) {
        logger.info("Expected exception creating user beyond limit: {}", e.getMessage());
      }
      // assertTrue(api.users.isRegularUser(user1), "user1 is not a regular user as expected");
      assertTrue(
          api.users.getTraccarRole(user1) == TraccarRole.REG_USER,
          "user1 is not a regular user as expected");

      // clean up AS ADMIN
      // manager does not have permission to delete users!!
      api.setBasicAuth(adminMail, adminPassword);
      api.users.deleteUser(manager.getId());
      api.users.deleteUser(user1.getId());
      api.users.deleteUser(user2.getId());
      if (user3 != null) api.users.deleteUser(user3.getId());
    }
  }

  /** Test platonic API methods from interface for User DTO */
  @Test
  public void createUpdateDeleteUser() {
    api.setBearerToken(virtualAdmin);

    // get nr of users, assert users++, back to nr
    int userNr = api.users.getAllUsers().size();

    // create / receive user
    User user = new User();
    // newUser.setId(5); // do not use!, generated by DB
    user.setName("user-1");
    user.setEmail("email-1"); // email syntax is not validated !
    user.setPassword("pw-1"); // can't login UI without a password (useful!), backend can without

    User newUser = api.users.createUser(user);
    // matching method names !!
    assertEquals(userNr + 1, api.users.getAllUsers().size());

    // returns the generated id (asserts not null)
    Long userId = newUser.getId();

    // update user
    newUser.setEmail("email-1-b");

    User putUser = api.users.updateUser(userId, newUser);
    assertEquals(userNr + 1, api.users.getAllUsers().size());

    // delete user
    api.users.deleteUser(putUser.getId());
    assertEquals(userNr, api.users.getAllUsers().size());
  }

  /**
   * Due to API (generated code) inconsistency the id is Long here, but restricted to Integer range.
   * Values out of Integer range will throw an ApiException and the server is not called (and must
   * not be running for test).
   */
  @Test
  public void doNotUpdateDeleteUser() {
    api.setBearerToken(virtualAdmin);

    User user = new User(); // won't reach server
    // Long values out of Integer range will throw an ApiException
    Long longUserId = (long) (Integer.MAX_VALUE + 1L);

    // update user should fail without server call
    try {
      api.users.updateUser(longUserId, user);
    } catch (ApiException e) {
      assertEquals(ArithmeticException.class, e.getCause().getClass());
    }
    logger.error("User with id={] was not updated on server!", longUserId);

    // delete user should fail without server call
    try {
      api.users.deleteUser(longUserId);
    } catch (ApiException e) {
      assertEquals(ArithmeticException.class, e.getCause().getClass());
    }
    logger.error("User with id={] was not deleted on server!", longUserId);
  }

  @Test
  public void convertLongToInt() {
    // true positive test
    Long longVal = 12345L;
    Integer intVal = ApiHelper.toInt(longVal);
    assertEquals(longVal, 12345L);

    // true negative test
    longVal = Integer.MAX_VALUE + 1L;
    try {
      intVal = ApiHelper.toInt(longVal);
    } catch (ApiException e) {
      assertEquals(ArithmeticException.class, e.getCause().getClass());
    }
    logger.error("Long value {} was not converted to Integer!", longVal);
  }

  /**
   * Test the new getUser(id) method according to Traccar API reference
   * https://www.traccar.org/api-reference/#tag/Users/operation/getUsersId
   */
  @Test
  public void getUserByIdTest() {
    api.setBearerToken(virtualAdmin);

    // Create a test user
    User testUser =
        api.users.createUserWithCredentials("testUser", "testPw", "test@example.com", false);
    logger.info("Created test user with id: {}", testUser.getId());

    User fetchedUser = api.users.getUserById(testUser.getId().toString());
    assertEquals(testUser.getId(), fetchedUser.getId());
    assertEquals(testUser.getEmail(), fetchedUser.getEmail());
    assertEquals(testUser.getName(), fetchedUser.getName());
    logger.info("Successfully fetched user by id: {}", fetchedUser.getId());

    // Clean up
    api.users.deleteUser(testUser.getId());
  }

  private void listUsers(List<User> users) {
    if (users.isEmpty()) logger.info("No users found");
    for (User u : users)
      logger.info(
          "User: {} {} {} {} {}",
          u.getEmail(),
          u.getAdministrator(),
          u.getDeviceLimit(),
          u.getUserLimit(),
          u.getId());
  }
}
