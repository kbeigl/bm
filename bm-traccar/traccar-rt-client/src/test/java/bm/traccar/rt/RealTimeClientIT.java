package bm.traccar.rt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.BaseRealTimeClientTest;
import bm.traccar.generated.model.dto.User;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class RealTimeClientIT extends BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeClientIT.class);

  private User admin;

  @Test
  void lifecycleTest() throws Exception {

    logger.info("Verifying that the Spring context loads correctly.");
    assertThat(controller).isNotNull();
    assertThat(scenario).isNotNull();
    assertThat(client).isNotNull();

    admin = new User();
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
