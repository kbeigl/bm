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
package bm.traccar.api.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.api.ApiException;
import bm.traccar.api.BaseIntegrationTest;
import bm.traccar.generated.model.dto.Permission;
import bm.traccar.generated.model.dto.User;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PermissionsApiIT extends BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseIntegrationTest.class);

  // currently we are focused on users and permissions.
  // use BaseScenarioTest to create a scenario with users and devices for testing.

  @Test
  void permissionTests() {
    // Created user@domain.com (id=835) and admin@domain.com (id=836)
    // try setting admin user- and deviceLimit
    // api.setBearerToken(virtualAdmin);
    api.setBasicAuth(adminMail, adminPassword);
    createTestUsers();
    try {
      // getAuthentication() returns the userId of the authenticated user (adminId)
      api.getUsersApi()
          .getUsers(null, null, null, null)
          .forEach(u -> logger.info("User: {} (id={})", u.getEmail(), u.getId()));

      // manager@domain.com (id=876)  ***
      //    hide@domain.com (id=877)   *
      //    seek@domain.com (id=878)   *
      //  SELECT * FROM TC_USER_USER;
      //    USERID  	MANAGEDUSERID
      //    	876			877
      //    	876			878

      // counter check with admin user list
      // also in the TC_USER_USER table? does user limit matter?
      Permission permission = new Permission();
      // permission.setUserId(Long.valueOf(adminId));
      permission.setUserId(manager.getId());
      permission.setManagedUserId(hide.getId());
      logger.info("Create Permission: {}", permission);

      // NOTE: Traccar's Permission constructor requires exactly two keys in the JSON body
      // (owner + property, e.g. userId + managedUserId). The generated DTO would serialize
      // all other fields as null, which Traccar rejects with "Invalid permission".
      // ApiConfig therefore configures the ApiClient's ObjectMapper with Include.NON_NULL,
      // so user-to-user (and user-to-device) permissions work via the REST API.
      api.getPermissionsApi().createPermission(permission);

      // the manager now manages 'hide' but not 'seek': GET /users?userId=<manager>
      Set<Long> managed =
          api.getUsersApi().getUsers(String.valueOf(manager.getId()), null, null, null).stream()
              .map(User::getId)
              .collect(Collectors.toSet());
      assertEquals(Set.of(hide.getId()), managed, "manager manages hide only");

      // and the link can be removed again
      api.getPermissionsApi().deletePermission(permission);
      managed =
          api.getUsersApi().getUsers(String.valueOf(manager.getId()), null, null, null).stream()
              .map(User::getId)
              .collect(Collectors.toSet());
      assertTrue(managed.isEmpty(), "manager manages nobody after deletePermission");

      // an incomplete permission (owner only) is still rejected by the server
      Permission invalid = new Permission();
      invalid.setUserId(manager.getId());
      ApiException exception =
          assertThrows(
              ApiException.class,
              () -> api.getPermissionsApi().createPermission(invalid),
              "Expected ApiException for a permission without property");
      assertEquals(ApiException.APIEX_BAD_REQUEST, exception.getMessage());
      assertTrue(
          exception.getCause() instanceof HttpClientErrorException,
          "Expected HttpClientErrorException as cause, but got: " + exception.getCause());
      assertTrue(
          exception.getCause().getMessage().contains("Invalid permission"),
          "Expected error message to contain 'Invalid permission', but got: "
              + exception.getCause().getMessage());
    } finally {
      deleteTestUsers();
    }
  }

  // passwords should be kept in test context
  String mgrPw = "manager", hidePw = "hide", seekPw = "seek";
  User manager = null, hide = null, seek = null;

  // @BeforeAll ?
  void createTestUsers() {
    // leftovers from aborted runs
    for (String mail : new String[] {"manager@domain.com", "hide@domain.com", "seek@domain.com"})
      deleteUserIfExists(mail);
    // create manager and two regular users hide & seek
    // move to BaseIntegrationTest to be used by other tests ?
    manager =
        api.getUsersApi().createManagerWithCredentials("manager", mgrPw, "manager@domain.com", 4);
    hide = api.getUsersApi().createUserWithCredentials("hide", hidePw, "hide@domain.com");
    seek = api.getUsersApi().createUserWithCredentials("seek", seekPw, "seek@domain.com");
    logger.info("Created Test Users");
  }

  private void deleteTestUsers() {
    // cleanup: delete manager and users for this test (only)
    api.setBasicAuth(adminMail, adminPassword);
    if (hide != null) api.getUsersApi().deleteUser(hide.getId());
    if (seek != null) api.getUsersApi().deleteUser(seek.getId());
    if (manager != null) api.getUsersApi().deleteUser(manager.getId());
    logger.info("Deleted Test Users");
  }
}
