package bm.traccar.api.scenario;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.api.BaseIntegrationTest;
import bm.traccar.generated.model.dto.Permission;
import bm.traccar.generated.model.dto.User;
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
    // getAuthentication() returns the userId of the authenticated user (adminId)
    api.users.getAllUsers().forEach(u -> logger.info("User: {} (id={})", u.getEmail(), u.getId()));

    // manager@domain.com (id=876)	***
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

    // NOTE: Traccar's API does NOT support creating user-to-user management permissions
    // through the standard /permissions endpoint, even with adminAuth authentication.
    // The UI allows this through "Connections" but it's not exposed via the REST API.
    //
    // Attempting to create userId + managedUserId permission:
    // This will fail with "Invalid permission" because Traccar's Permission validation
    // at Permission.java:56 rejects this combination when created via API.

    // Assert that the expected exception is thrown
    HttpClientErrorException exception =
        assertThrows(
            HttpClientErrorException.class,
            () -> api.permissions.createPermission(permission),
            "Expected HttpClientErrorException when creating user-to-user permission");

    // Verify the error message contains "Invalid permission"
    assertTrue(
        exception.getMessage().contains("Invalid permission"),
        "Expected error message to contain 'Invalid permission', but got: "
            + exception.getMessage());

    logger.warn(
        "User-to-user permission creation failed as expected - not supported via Traccar REST API. "
            + "Use the web UI 'Connections' feature to manage user relationships.");

    deleteTestUsers();
  }

  // passwords should be kept in test context
  String mgrPw = "manager", hidePw = "hide", seekPw = "seek";
  User manager = null, hide = null, seek = null;

  // @BeforeAll ?
  void createTestUsers() {
    // create manager and two regular users hide & seek
    // move to BaseIntegrationTest to be used by other tests ?
    manager = api.users.createManagerWithCredentials("manager", mgrPw, "manager@domain.com", 4);
    hide = api.users.createUserWithCredentials("hide", hidePw, "hide@domain.com", false);
    seek = api.users.createUserWithCredentials("seek", seekPw, "seek@domain.com", false);
    logger.info("Created Test Users");
  }

  private void deleteTestUsers() {
    // cleanup: delete manager and users for this test (only)
    api.users.deleteUser(hide.getId());
    api.users.deleteUser(seek.getId());
    api.users.deleteUser(manager.getId());
    logger.info("Deleted Test Users");
  }
}
