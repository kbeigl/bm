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
package bm.traccar.api.roles;

import bm.traccar.api.Api.Users.TraccarRole;
import bm.traccar.api.BaseIntegrationTest;
import bm.traccar.generated.model.dto.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for user roles and permissions in the Traccar API.
 *
 * <p>This test validates superAdmin, admin, manager, and regular user roles with regard to their
 * permissions and limits.
 *
 * <p>As Traccar is a Tracking System we will use User, Device and Permission APIs to relate
 * entities to roles and explore different scenarios.
 */
public class RolesIT extends BaseIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(RolesIT.class);

  /* see and move relevant code from traccar-realtime-service ITests */

  // different user roles (superAdmin, admin, regular user, manager
  @Test
  public void createUsersHierarchyTest() {
    // no virtualAdmin involved
    api.setBasicAuth(adminMail, adminPassword); // session ----------
    logSessionInfo();
    logVisibleUsers(api.users.getUsers(null, null, null, null));

    String mgrMail = "scenarioMgr@domain.com", mgrPassword = "scenarioMgr";
    // 4 users, on runner and 1 to 3 chasers
    User manager = api.users.createManagerWithCredentials("scenarioMgr", mgrPassword, mgrMail, 4);
    logger.info("Created User {} (id={})", manager.getEmail(), manager.getId());
    // now the admin can also see the manager
    logVisibleUsers(api.users.getUsers(null, null, null, null));

    api.setBasicAuth(mgrMail, mgrPassword); // session ------------------
    logSessionInfo();
    // add role check in createUser.. methods
    User scenarioRunner =
        api.users.createUserWithCredentials(
            "scenarioRunner", "scenarioRunner", "scenarioRunner@domain.com", false);
    User scenarioChaser =
        api.users.createUserWithCredentials(
            "scenarioChaser", "scenarioChaser", "scenarioChaser@domain.com", false);

    logVisibleUsers(api.users.getUsers(null, null, null, null));

    // clean up this test (only)
    api.users.deleteUser(scenarioChaser.getId());
    api.users.deleteUser(scenarioRunner.getId());
    api.users.deleteUser(manager.getId());
    logger.info("Deleted Test Users");
  }

  private void logVisibleUsers(List<User> users) {
    if (users != null && !users.isEmpty()) {
      logger.info("Visible users:");
      for (User u : users) {
        TraccarRole role = api.users.getTraccarRole(u);
        logger.info("  User: {} (id={}) has role {}", u.getEmail(), u.getId(), role);
      }
    } else {
      logger.warn("No visible users found for the current session.");
    }
  }

  /**
   * Log the current session information, including authentication type and user role.
   *
   * <p>Candidate for (combined) API method.
   *
   * <p>Note that this method is using users and session APIs to retrieve the current user and their
   * role, which may not be available in all contexts.
   */
  private void logSessionInfo() {
    logger.info("Authenticated: {}", api.getAuthentication());
    User loggedUser = api.session.getCurrentUser();
    if (loggedUser != null) {
      TraccarRole role = api.users.getTraccarRole(loggedUser);
      logger.info(
          "Current session user: {} (id={}) has role {}",
          loggedUser.getEmail(),
          loggedUser.getId(),
          role);
      // current session is not authenticated ?
    } else logger.warn("Current session user is null, cannot determine role");
  }
}
