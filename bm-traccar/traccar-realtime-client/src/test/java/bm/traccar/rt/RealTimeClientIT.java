package bm.traccar.rt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.traccar.BaseRealTimeClientTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class RealTimeClientIT extends BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeClientIT.class);

  @Test
  void lifecycleTest() throws Exception {

    logger.info("Verifying that the Spring context loads correctly.");
    assertThat(controller).isNotNull();
    assertThat(scenario).isNotNull();
    assertThat(client).isNotNull();

    logger.info("Verifying that Scenario loads correctly.");
    assertNotNull(scenario.admin, "admin is not set");
    assertNotNull(scenario.admin.getPassword(), "admin.password is not set");
    // check BasicAuth set in Api client

    logger.info("Verifying that Controller loads correctly.");
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

    // wait for initial WebSocket messages to arrive
    Thread.sleep(10 * 1000);
  }
}
