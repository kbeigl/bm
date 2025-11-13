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
 * This is a small ETL, reading the scenario.props, tranforming props into Traccar DTOs and loading
 * them to the Traccar Server.
 *
 * <p>Setup a minimal, but strict scenario with users and devices (for testing).
 *
 * <p>This class takes care of getting the scenario properties via ScenarioProperties class and
 * creates the Test Scenario on the Traccar Server.
 */
@Component // @Singleton ?
public class ScenarioLoader {
  private static final Logger logger = LoggerFactory.getLogger(ScenarioLoader.class);

  @Value("${traccar.web.serviceAccountToken}")
  protected String virtualAdmin;

  @Autowired protected Api api;
  @Autowired private ScenarioProperties props;

  public User admin;
  User manager, hide, seek;

  // setupScenario(adminMail, adminPassword);

  /** Each scenario must have an admin account with a view on all Traccar Objects. */
  public void setupScenario() {

    // set initial authentication - outside ?
    api.setBearerToken(virtualAdmin);
    setupServer();
    admin = createAdminUser(props.getUser().get(0));

    // continue as admin who is not part of the scenario!
    api.setBasicAuth(admin.getEmail(), props.getUser().get(0).password);
    createScenarioUsers();

    // create devices for users - as admin or user ?
    // one user could control all detective-devices
    createScenarioDevices();

    // virtualAdmin is still authorized in extending tests!

  }

  // createDevicesForUser(..);

  Device adminDevice, managerDevice, hideDevice, seekDevice;

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
          adminDevice = createdDevice;
          break;
        case 1:
          managerDevice = createdDevice;
          break;
        case 2:
          hideDevice = createdDevice;
          break;
        case 3:
          seekDevice = createdDevice;
          break;
      }
    }
  }

  /** This method can only be executed by an admin with rights to create users and devices. */
  private void createScenarioUsers() {
    for (int i = 1; i < props.getUser().size(); i++) { // start at 1, admin is created separately
      ScenarioProperties.User u = props.getUser().get(i);
      User createdUser =
          api.getUsersApi().createUserWithCredentials(u.name, u.password, u.email, false);
      logger.info("Created User.id{}: {}", createdUser.getId(), createdUser.getName());
      // Optionally assign to fields if needed
      switch (i) {
        case 1:
          manager = createdUser;
          break;
        case 2:
          hide = createdUser;
          break;
        case 3:
          seek = createdUser;
          break;
      }
    }
  }

  private User createAdminUser(ScenarioProperties.User user) {
    admin =
        api.getUsersApi()
            .createUserWithCredentials(user.name, user.password, user.email, user.administrator);
    // provide permissions
    //  0: Cannot create any users/devices.  (default)
    // -1: Can create an unlimited number of users/devices.
    //  N: Can create up to N users/devices.
    // this can also be applied to managers
    admin.setUserLimit(5);
    admin.setDeviceLimit(5);
    api.getUsersApi().updateUser(admin.getId(), admin);
    logger.info("Created Admin User.id{}: {}", admin.getId(), admin.getName());
    // for test purposes we will set the password
    admin.setPassword(user.password);
    return admin;
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

    // never know who's calling
    // api.setBearerToken(virtualAdmin);
    api.setBasicAuth(admin.getEmail(), props.getUser().get(0).password);

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
        for (User user : api.getUsersApi().getUsers(null)) {
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
      for (User user : api.getUsersApi().getUsers(null)) {
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
