package bm.traccar.api.scenario;

import bm.traccar.api.Api;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Server;
import bm.traccar.generated.model.dto.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * This class takes care of getting the scenario properties via ScenarioProperties class and creates
 * the (Test) Scenario on the Traccar Server.
 *
 * <p>A little ETL, reading the scenario.props, transforming props into Traccar DTOs and loading
 * them to the Traccar Server. i.e. setup a minimal, but strict scenario with users and devices (for
 * testing).
 *
 * <p>The setup is done in multiple steps, each requiring specific permissions. Therefor the
 * scenario setup is done line by line to explore the server behavior.
 */
@Component // @Singleton ?
public class ScenarioLoader {
  private static final Logger logger = LoggerFactory.getLogger(ScenarioLoader.class);

  @Value("${traccar.web.serviceAccountToken}")
  protected String virtualAdmin;

  @Autowired protected Api api;
  @Autowired private ScenarioProperties props;

  // admin is not part of the scenario!
  // Each scenario must have a manager account with a view on all Traccar Objects.
  public User admin, manager, hide, seek;

  // consider: setupScenario(adminMail, adminPassword);

  public void setupScenario() {

    api.setBearerToken(virtualAdmin);
    setupServer();

    // virtualAdmin creates actual server admin ===========
    ScenarioProperties.User user = props.getUser().get(0);
    admin =
        api.getUsersApi()
            .createUserWithCredentials(user.name, user.password, user.email, user.administrator);
    // for test purposes ONLY we set the password for simplicity
    admin.setPassword(user.password);
    logger.info("Created User.id{}: {}/{}", admin.getId(), admin.getName(), admin.getPassword());

    // continue as admin who is not part of the scenario!
    api.setBasicAuth(admin.getEmail(), admin.getPassword());
    createScenarioUsers();
    createScenarioDevices();
  }

  /**
   * This method can only be executed by an admin with rights to create users and devices.
   *
   * <p>throw noPermissionException ?
   */
  private void createScenarioUsers() {
    // start at 1, admin is created separately
    for (int i = 1; i < props.getUser().size(); i++) {
      ScenarioProperties.User u = props.getUser().get(i);
      User createdUser =
          api.getUsersApi().createUserWithCredentials(u.name, u.password, u.email, false);

      logger.info(
          "Created User.id{}: {}/{}", createdUser.getId(), createdUser.getName(), u.password);
      switch (i) {
        case 1: // manager attributes
          manager = createdUser;
          //  0: Cannot create any users/devices.  (default)
          // -1: Can create an unlimited number of users/devices.
          //  N: Can create up to N users/devices.
          manager.setUserLimit(2);
          manager.setDeviceLimit(3);
          manager = api.getUsersApi().updateUser(manager.getId(), manager);
          // server does not return password, set here for further use
          manager.setPassword(u.password);
          break;
        case 2:
          hide = createdUser;
          // IF the user wants to create his own devices
          hide.setDeviceLimit(1);
          hide = api.getUsersApi().updateUser(hide.getId(), hide);
          hide.setPassword(u.password);
          break;
        case 3:
          seek = createdUser;
          // IF the user wants to add own devices
          seek.setDeviceLimit(3);
          seek = api.getUsersApi().updateUser(hide.getId(), hide);
          seek.setPassword(u.password);
          break;
      }
    }
  }

  // createDevicesForUser(..);

  public Device realDevice, runnerDevice, chaser1Device, chaser2Device;

  /** This method can only be executed by an admin with rights to create users and devices. */
  private void createScenarioDevices() {
    for (int i = 0; i < props.getDevice().size(); i++) {
      ScenarioProperties.Device d = props.getDevice().get(i);
      Device createdDevice =
          api.getDevicesApi()
              .createDevice(new Device().name(d.name).uniqueId(d.uniqueId).model(d.model));
      logger.info("Created Device.id{}: {}", createdDevice.getId(), createdDevice.getName());
      switch (i) {
        case 0:
          // add real device to manager
          realDevice = createdDevice;
          break;
        case 1:
          runnerDevice = createdDevice;
          break;
        case 2:
          chaser1Device = createdDevice;
          break;
        case 3:
          chaser2Device = createdDevice;
          break;
      }
    }
  }

  /** This actually does not belong to the scenario itself, it's a prerequisite. */
  private void setupServer() {
    logger.info("--- Setup scenario on server ---");
    Server server = api.getServerApi().getServerInfo();
    if (!server.getRegistration()) {
      server.setRegistration(true);
      api.getServerApi().updateServer(server);
    }
  }

  /** Remove all scenario users and devices from the server. */
  // delete users > imply devices and relations?
  public void teardownScenario() {

    /*
     * scenario.setup    is called from @BeforeAll
     * scenario.teardown is called from @AfterAll  in BaseScenarioTest
     *
     * The problem is that after setupScenario this instance is different! Therefor the admin
     * user is null. So we take the admin credentials from the properties and hope the scenario was
     * not changed.
     */
    // .setBasicAuth(admin.getEmail(), props.getUser().get(0).password);
    api.setBasicAuth(props.getUser().get(0).email, props.getUser().get(0).password);

    logger.info("--- tear down scenario on server ---");
    // Remove devices
    for (int i = 0; i < props.getDevice().size(); i++) {
      ScenarioProperties.Device d = props.getDevice().get(i);
      try {
        // Find device by uniqueId and delete
        for (Device device : api.getDevicesApi().getDevices(null)) {
          if (device.getUniqueId().equals(d.uniqueId)) {
            api.getDevicesApi().deleteDevice(device.getId());
            logger.info("Deleted Device.id{}: {}", device.getId(), device.getName());
            break;
          }
        }
      } catch (Exception e) {
        logger.warn("Failed to delete device {}: {}", d.uniqueId, e.getMessage());
      }
    }
    // Remove users (except admin)
    for (int i = 1; i < props.getUser().size(); i++) {
      ScenarioProperties.User u = props.getUser().get(i);
      try {
        // Find user by email and delete
        for (User user : api.getUsersApi().getAllUsers()) {
          if (user.getEmail().equals(u.email)) {
            api.getUsersApi().deleteUser(user.getId());
            logger.info("Deleted User.id{}: {}", user.getId(), user.getEmail());
            break;
          }
        }
      } catch (Exception e) {
        logger.warn("Failed to delete user {}: {}", u.email, e.getMessage());
      }
    }
    // Remove admin user
    ScenarioProperties.User adminProp = props.getUser().get(0);
    try {
      for (User user : api.getUsersApi().getAllUsers()) {
        if (user.getEmail().equals(adminProp.email)) {
          api.getUsersApi().deleteUser(user.getId());
          logger.info("Deleted Admin User.id{}: {}", user.getId(), user.getEmail());
          break;
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to delete admin user {}: {}", adminProp.email, e.getMessage());
    }
  }

  // public void loadScenario(String adminMail, String adminPassword) {} from server
  // public void saveScenario(String adminMail, String adminPassword) {} to harddisc
}
