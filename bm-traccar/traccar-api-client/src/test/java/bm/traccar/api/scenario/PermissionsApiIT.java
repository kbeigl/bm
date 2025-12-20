package bm.traccar.api.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bm.traccar.api.Api;
import bm.traccar.generated.model.dto.Permission;
import bm.traccar.generated.model.dto.User;
import java.util.List;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

// PermissionsApiIT only makes sense with a scenario
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PermissionsApiIT extends BaseScenarioTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseScenarioTest.class);

  @Value("${traccar.web.serviceAccountToken}")
  protected String virtualAdmin;

  @Autowired private Api api;
  private Api.Users usersApi;

  // private Api.Devices devicesApi;

  // Tests should be forward compatible to a constantly changing scenario

  /** Check Users for different roles. */
  //  @Test
  void roleTests() {
    usersApi = api.getUsersApi();
    api.setBearerToken(virtualAdmin);
    List<User> userList = usersApi.getAllUsers();
    logger.info("-- all {} server account roles -- ", userList.size());

    for (User u : userList) {
      assertEquals( // cross check getAllUsers vs getUserById
          u,
          usersApi.getUserById(u.getId().toString()),
          "getUserById returned different user than in userList");

      // implement roles in userApi
      if (usersApi.isAdmin(u)) logger.info("  admin: {}", u.getEmail());
      else if (usersApi.isManager(u)) logger.info("manager: {}", u.getEmail());
      else logger.info("regular: {}", u.getEmail());
    }
    logger.info(""); // breakpoint

    // now login to counter check
    //    // admin
    //    api.setBasicAuth(scenario.admin.getEmail(), scenario.admin.getPassword());
    //    String adminId = scenario.admin.getId().toString();
    //    logger.info("-- all admin{} Users --", adminId);
    //    userList = usersApi.getUsers(null);
    //    listUsers(userList);
    //    listDevices(adminId);
    //
    //    // manager
    //    api.setBasicAuth(scenario.manager.getEmail(), scenario.manager.getPassword());
    //    String managerId = scenario.manager.getId().toString();
    //    logger.info("-- all manager{} Users --", managerId);
    //    userList = usersApi.getUsers(null);
    //    listUsers(userList);
  }

  //  @Test
  void permissionTests() {
    // now we can run permission tests - TOBE integrated in ScenarioLoader
    Permission permission = new Permission();
    logger.info("Create Permission: {}", permission);
    //    permission.setUserId(Long.valueOf(adminId));
  }

  // ========== Device will be moved to another IT ==========

  /**
   * This listing depends on the role of the user making the call.
   *
   * <p>null - returns a list of the user's devices all boolean <br>
   * Can only be used by admins or managers to fetch all entities <br>
   * userId integer - Standard users can use this only with their own userId<br>
   * id integer - To fetch one or more devices. <br>
   * Multiple params can be passed like id=31&id=42 <br>
   * uniqueId string - To fetch one or more devices. <br>
   * Multiple params can be passed like uniqueId=333331&uniqieId=44442
   *
   * @param userId
   */
  private void listDevices(String userId) {
    // whoAmI()
    //    List<Device> deviceList = devicesApi.getDevices(null);
    //    if (deviceList.size() == 0) {
    //      logger.info("No devices found for User-{}!", userId);
    //      return;
    //    }
    //
    //    deviceList.forEach(
    //        d ->
    //            logger.info(
    //                "Device{}: {} \towned by User{}",
    //                d.getId(),
    //                d.getName(),
    //                devicesApi.getDevices(d.getId().toString()).size()));
  }
}
