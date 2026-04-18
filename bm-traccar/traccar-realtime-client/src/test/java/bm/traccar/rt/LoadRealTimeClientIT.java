package bm.traccar.rt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.gps.tracker.TrackerOsmAnd;
import bm.gps.tracker.TrackerRegistration;
import bm.traccar.BaseRealTimeClientTest;
import java.util.concurrent.TimeUnit;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
class LoadRealTimeClientIT extends BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(LoadRealTimeClientIT.class);

  @Autowired TrackerRegistration registrationService;
  @Autowired private ApplicationContext applicationContext;
  @Autowired private CamelContext camel;

  @Test
  void lifecycleTest() throws Exception {

    logger.info("Verifying that the Spring context loads correctly.");
    assertThat(controller).isNotNull();
    assertThat(scenario).isNotNull();
    // assertThat(client).isNotNull();

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
    sleep(3000);
  }

  //  @Test
  void routeLifecycleTest() throws Exception {
    String uniqueId = "123456789"; // scenario.runnerDevice.getUniqueId();
    String safeId = uniqueId == null ? "" : uniqueId.replaceAll("[^A-Za-z0-9_-]", "_");
    String routeId = "send-osmand-route-" + safeId;

    // ensure no route initially (or previous runs cleaned up)
    assertThat(camel.getRouteController().getRouteStatus(routeId)).isNull();

    // register tracker twice and assert same instance and only one route exists
    TrackerOsmAnd t1 = registrationService.registerTracker(uniqueId);
    TrackerOsmAnd t2 = registrationService.registerTracker(uniqueId);
    assertThat(t1).isSameAs(t2);

    // allow camel to start the route
    TimeUnit.MILLISECONDS.sleep(50);

    long count = camel.getRoutes().stream().filter(r -> routeId.equals(r.getId())).count();
    assertEquals(1, count, "Expected exactly one route with id " + routeId);
    assertThat(camel.getRouteController().getRouteStatus(routeId)).isNotNull();

    // destroy the singleton and assert the route is removed
    ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
    DefaultListableBeanFactory dlbf = (DefaultListableBeanFactory) cac.getBeanFactory();
    String beanName = "tracker-" + uniqueId;
    if (dlbf.containsSingleton(beanName)) dlbf.destroySingleton(beanName);

    // allow Camel time to remove the route via @PreDestroy
    TimeUnit.MILLISECONDS.sleep(100);
    assertThat(camel.getRouteController().getRouteStatus(routeId)).isNull();
  }
}
