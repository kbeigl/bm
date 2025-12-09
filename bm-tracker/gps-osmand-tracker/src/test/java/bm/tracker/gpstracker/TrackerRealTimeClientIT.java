package bm.tracker.gpstracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.BaseRealTimeClientTest;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.User;
import bm.tracker.gpstracker.model.GpsMessage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the OsmAndTracker client component in BaseRealTimeClient context, i.e.
 * scenario.
 *
 * <p>Dependencies to api and real-time-client modules are only required for integration tests. The
 * Tracker should only depend on generated model classes.
 *
 * <p>Although this is an integration test, the RealTimeClient is not started here. The focus is on
 * testing the Tracker component in a scenario with devices (and users?).
 */
@SpringBootTest(
    classes = {
      bm.traccar.RealTimeClient.class,
      bm.tracker.gpstracker.GpsOsmandTrackerTestConfig.class
    })
@ComponentScan(basePackages = {"bm.tracker.gpstracker"})
@ActiveProfiles("test")
class TrackerRealTimeClientIT extends BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(TrackerRealTimeClientIT.class);

  @Autowired TrackerRegistrationService registrationService;

  @Test
  void sendMessagesWithDevices() throws Exception {

    logger.info("Setup scenario devices for {} ", scenario.admin);
    // devices from scenarioLoader from DB. will be RT client later
    Device adminDevice, managerDevice, hideDevice, seekDevice;
    OsmAndTracker client = registrationService.registerTracker("tracker-10" + System.nanoTime());
    // listAllBeansInTestContext();

    // define positions according to existing tracks
    GpsMessage m1 = new GpsMessage();
    m1.setId("10");
    m1.setLat(52.0);
    m1.setLon(13.0);
    m1.setTimestamp(System.currentTimeMillis());
    client.send(m1);

    GpsMessage m2 = new GpsMessage();
    m2.setId("10");
    m2.setLat(52.0001);
    m2.setLon(13.0001);
    m2.setTimestamp(System.currentTimeMillis());

    GpsMessage m3 = new GpsMessage();
    m3.setId("10");
    m3.setLat(52.0002);
    m3.setLon(13.0002);
    m3.setTimestamp(System.currentTimeMillis());

    client.send(m2);
    client.send(m3);
  }

  private User admin;

  // @Test
  void contextTest() throws Exception {
    logger.info("Verifying that the Spring context loads correctly.");
    assertThat(controller).isNotNull();
    assertThat(scenario).isNotNull();
    assertThat(client).isNotNull();
  }

  // kick in RT when clients are ready
  // @Test
  void controllerLoginTest() throws Exception {

    admin = new User();
    // use scenario.props !
    admin.setName("admin");
    admin.setPassword("admin");
    admin.setEmail("admin@scenario.com");

    if (controller.loginAndInitialize(admin)) {
      logger.info("RealTimeClient login and initialization test passed.");

      assertNotNull(scenario.admin, "admin is not set");
      assertNotNull(scenario.admin.getPassword(), "admin.password is not set");
      // check BasicAuth set in Api client
      controller
          .getCurrentUser()
          .ifPresentOrElse(
              user -> {
                logger.info("Logged in user: {} (id={})", user.getEmail(), user.getId());
                assertThat(user.getEmail()).isEqualTo(scenario.admin.getEmail());
              },
              () -> {
                throw new AssertionError("No user is logged in after initialization.");
              });
      assertTrue(controller.isAuthenticated());
      assertEquals(4, controller.getAllUsers().size());
      assertEquals(4, controller.getAllDevices().size());

      // go through all controller methods

    } else {
      logger.error("RealTimeClient login and initialization test failed.");
    }
    // wait for initial WebSocket messages to arrive
    Thread.sleep(10 * 1000);
    controller.shutdown(); // clean up WebSocket route
  }
}
