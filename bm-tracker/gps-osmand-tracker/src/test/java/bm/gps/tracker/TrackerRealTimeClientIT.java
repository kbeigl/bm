package bm.gps.tracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.gps.MessageOsmand;
import bm.traccar.BaseRealTimeClientTest;
import bm.traccar.RealTimeClient;
import java.util.concurrent.TimeUnit;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * The Tracker should be independant!
 *
 * <p>Integration tests for the OsmAndTracker client component in BaseRealTimeClient context, i.e.
 * scenario. Dependencies to api and real-time-client modules are only required for integration
 * tests.
 */
@SpringBootTest(classes = {RealTimeClient.class, GpsOsmandTrackerTestConfig.class})
@ComponentScan(basePackages = {"bm.tracker.gpstracker"})
@ActiveProfiles("test")
class TrackerRealTimeClientIT extends BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(TrackerRealTimeClientIT.class);

  @Autowired TrackerRegistration registrationService;
  @Autowired private ApplicationContext applicationContext;
  @Autowired private CamelContext camel;

  @Test
  void sendMessagesWithDevices() throws Exception {
    String runnerId = scenario.runnerDevice.getUniqueId(),
        chaser1Id = scenario.chaser1Device.getUniqueId(),
        chaser2Id = scenario.chaser2Device.getUniqueId(),
        mobileId = scenario.realDevice.getUniqueId();

    // register trackers (or lookup existing singletons)
    TrackerOsmAnd runnerTracker = registrationService.registerTracker(runnerId),
        chaser1Tracker = registrationService.lookupTracker(chaser1Id),
        chaser2Tracker = registrationService.lookupTracker(chaser2Id),
        mobileTracker = registrationService.lookupTracker(mobileId);

    assertThat(runnerTracker).isNotNull();
    // get uniqueIds from trackers to be sure
    runnerTracker.sendNow(
        MessageOsmand.now(runnerTracker.getUniqueId(), 52d, 13d, 10d, 20d, 30d, null, null));

    // validate message in Tracker context

    assertThat(chaser1Tracker).isNotNull();
    chaser1Tracker.sendNow(
        MessageOsmand.now(
            chaser1Tracker.getUniqueId(), 52.01, 13.01, 10.0, 20.0, 30.0, null, null));

    assertThat(chaser2Tracker).isNotNull();
    // prepare message in app context ..
    MessageOsmand msg =
        MessageOsmand.now(chaser2Tracker.getUniqueId(), 52.02, 13.02, 10.0, 20.0, 30.0, null, null);
    // .. and send some other time
    chaser2Tracker.sendNow(msg);

    assertThat(mobileTracker).isNotNull();
    mobileTracker.sendNow(
        MessageOsmand.now(mobileTracker.getUniqueId(), 52.03, 13.03, 10.0, 20.0, 30.0, null, null));

    // use tracker setters and sendNow without message

    // test messages with wrong id

  }

  @Test
  void sendMessagesWithDevice() throws Exception {
    // pick a device present on server side or messages will be dumped!
    String runnerId = scenario.runnerDevice.getUniqueId();

    // tracker should be registered in @BeforeAll and will be used silently if exists
    logger.info("register device with uniqueId={} ", runnerId);
    TrackerOsmAnd tracker = registrationService.registerTracker(runnerId);

    // define positions according to existing tracks
    long ts1 = System.currentTimeMillis();
    MessageOsmand m1 = new MessageOsmand(runnerId, 52d, 13d, ts1, null, null, null, null, null);
    logger.info("tracker-{} sending message", runnerId);
    tracker.sendNow(m1);

    long ts2 = System.currentTimeMillis();
    MessageOsmand m2 =
        new MessageOsmand(runnerId, 52.0001, 13.0001, ts2, null, null, null, null, null);

    long ts3 = System.currentTimeMillis();
    MessageOsmand m3 =
        new MessageOsmand(runnerId, 52.0002, 13.0002, ts3, null, null, null, null, null);

    tracker.sendNow(m2);
    tracker.sendNow(m3);
  }

  @Test
  void contextTest() throws Exception {
    logger.info("Verifying that the Spring context loads correctly.");

    assertThat(scenario).isNotNull();
    assertThat(client).isNotNull();

    assertThat(controller).isNotNull();
    assertTrue(controller.isAuthenticated());
    assertEquals(4, controller.getAllUsers().size());
    assertEquals(4, controller.getAllDevices().size());
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
  }

  @Test
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

  @BeforeAll
  public void registerScenarioTrackers() {
    logger.info("--- register Scenario Trackers ---");
    try {
      if (registrationService == null) {
        logger.debug("No TrackerRegistration available in this context.");
        return;
      }

      if (scenario.realDevice != null)
        registrationService.registerTracker(scenario.realDevice.getUniqueId());

      if (scenario.runnerDevice != null)
        registrationService.registerTracker(scenario.runnerDevice.getUniqueId());

      if (scenario.chaser1Device != null)
        registrationService.registerTracker(scenario.chaser1Device.getUniqueId());

      if (scenario.chaser2Device != null)
        registrationService.registerTracker(scenario.chaser2Device.getUniqueId());

    } catch (Exception e) {
      logger.warn("Failed to register scenario trackers: {}", e.getMessage());
    }
    logger.info("--- Scenario Trackers registered ---");
    logger.info("\n\t\t********** TrackerRealTimeClientIT - start of tests **********");
  }

  /*  Destroying the tracker bean (destroySingleton) does not automatically remove a route
   *  with the same id unless you explicitly remove it from Camel (getContext().removeRoute(routeId)).
   *  TODO: if you create per-tracker routes you must remove them explicitly on bean destruction.
   *  -> already implemented in TrackerOsmAnd destroy() method, but not triggered with destroySingleton?
   */
  @AfterAll
  public void unregisterScenarioTrackers() {

    if (registrationService == null) {
      logger.debug("No TrackerRegistration available to unregister trackers.");
      return;
    }
    try {
      if (scenario.realDevice != null)
        registrationService.unregisterTracker(scenario.realDevice.getUniqueId());
      if (scenario.runnerDevice != null)
        registrationService.unregisterTracker(scenario.runnerDevice.getUniqueId());
      if (scenario.chaser1Device != null)
        registrationService.unregisterTracker(scenario.chaser1Device.getUniqueId());
      if (scenario.chaser2Device != null)
        registrationService.unregisterTracker(scenario.chaser2Device.getUniqueId());
    } catch (Exception e) {
      logger.warn(
          "Failed to unregister scenario trackers via registrationService: {}", e.getMessage());
    }
    logger.info("\n\t\t***** TrackerRealTimeClientIT - end of tests *****");
  }
}
