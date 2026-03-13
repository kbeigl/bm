package bm.traccar.rt;

import static org.assertj.core.api.Assertions.assertThat;

import bm.gps.MessageOsmand;
import bm.gps.tracker.GpsOsmandTrackerConfig;
import bm.gps.tracker.TrackerOsmAnd;
import bm.gps.tracker.TrackerRegistration;
import bm.traccar.BaseRealTimeClientTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * The Tracker should be independant!
 *
 * <p>Integration tests for the OsmAndTracker client component in BaseRealTimeClient context, i.e.
 * scenario. Dependencies to api and real-time-client modules are only required for integration
 * tests.
 */
@ActiveProfiles("test")
@Import(GpsOsmandTrackerConfig.class)
class TrackerRealTimeClientIT extends BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(TrackerRealTimeClientIT.class);

  @Autowired TrackerRegistration registrationService;
  TrackerOsmAnd runnerTracker, chaser1Tracker, chaser2Tracker, mobileTracker;

  @Test
  void tracker2server2realtime() throws Exception {
    logger.info("\n\t\t********** tracker2server2realtime() **********");

    controller
        .getDeviceById(0L)
        .ifPresent(d -> logger.info("stateManager device: {}", d.getName()));

    // use tracker setters and sendNow without message
    runnerTracker.sendNow(
        // get uniqueIds from trackers for validation
        // test messages with wrong id
        MessageOsmand.now(runnerTracker.getUniqueId(), 52d, 13d, 10d, 20d, 30d, null, null));

    // wait for websocket message processing and
    // check if messages are received in realtime client

    sleep(10 * 1000); // wait for messages to be processed
  }

  @Test
  void sendMessagesWithDevices() throws Exception {
    logger.info("\n\t\t********** sendMessagesWithDevices() **********");

    runnerTracker.sendNow(
        // get uniqueIds from trackers for validation
        MessageOsmand.now(runnerTracker.getUniqueId(), 52d, 13d, 10d, 20d, 30d, null, null));

    chaser1Tracker.sendNow(
        MessageOsmand.now(chaser1Tracker.getUniqueId(), 52.01, 13.01, 10d, 20d, 30d, null, null));

    // prepare message in app context ..
    MessageOsmand msg =
        MessageOsmand.now(chaser2Tracker.getUniqueId(), 52.02, 13.02, 10.0, 20.0, 30.0, null, null);
    // .. and send some other time
    chaser2Tracker.sendNow(msg);

    mobileTracker.sendNow(
        MessageOsmand.now(mobileTracker.getUniqueId(), 52.03, 13.03, 10.0, 20.0, 30.0, null, null));

    sleep(5000);
  }

  @Test
  void sendMessagesWithDevice() throws Exception {
    logger.info("\n\t\t********** sendMessagesWithDevice() **********");

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
  void reRegisterOrLookupExistingDevices() throws Exception {
    logger.info("\n\t\t********** registerOrLookupExistingDevices() **********");

    String runnerId = scenario.runnerDevice.getUniqueId(),
        chaser1Id = scenario.chaser1Device.getUniqueId(),
        chaser2Id = scenario.chaser2Device.getUniqueId(),
        mobileId = scenario.realDevice.getUniqueId();

    // REGISTER trackers to get a reference to existing singletons
    // or create new ones if not registered yet
    runnerTracker = registrationService.registerTracker(runnerId);
    // or LOOKUP existing trackers
    chaser1Tracker = registrationService.lookupTracker(chaser1Id);
    chaser2Tracker = registrationService.lookupTracker(chaser2Id);
    mobileTracker = registrationService.lookupTracker(mobileId);

    assertThat(runnerTracker).isNotNull();
    assertThat(chaser1Tracker).isNotNull();
    assertThat(chaser2Tracker).isNotNull();
    assertThat(mobileTracker).isNotNull();
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
        mobileTracker = registrationService.registerTracker(scenario.realDevice.getUniqueId());

      if (scenario.runnerDevice != null)
        runnerTracker = registrationService.registerTracker(scenario.runnerDevice.getUniqueId());

      if (scenario.chaser1Device != null)
        chaser1Tracker = registrationService.registerTracker(scenario.chaser1Device.getUniqueId());

      if (scenario.chaser2Device != null)
        chaser2Tracker = registrationService.registerTracker(scenario.chaser2Device.getUniqueId());

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
      // loop over all devices and unregister trackers if they exist
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
