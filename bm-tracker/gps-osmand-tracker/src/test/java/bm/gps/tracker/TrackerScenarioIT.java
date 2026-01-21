package bm.gps.tracker;

import static org.assertj.core.api.Assertions.assertThat;

import bm.gps.MessageOsmand;
import bm.traccar.BaseRealTimeClientTest;
import bm.traccar.RealTimeClient;
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
 * Tracker should be independant!
 *
 * <p>Although this is an integration test, the RealTimeClient is not started here. The focus is on
 * testing the Tracker component in a scenario with devices (and users?).
 */
@SpringBootTest(classes = {RealTimeClient.class, GpsOsmandTrackerTestConfig.class})
@ComponentScan(basePackages = {"bm.gps.tracker"})
@ActiveProfiles("test")
class TrackerScenarioIT extends BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(TrackerScenarioIT.class);

  @Autowired TrackerRegistration registrationService;

  @Test
  void sendMessagesWithDevices() throws Exception {
    TrackerOsmAnd trackerReal, trackerRunner, trackerChaser1, trackerChaser2;

    logger.info("Setup scenario devices");

    // how to lookup tracker by name + device id ?
    trackerReal = registrationService.registerTracker(scenario.realDevice.getUniqueId());
    trackerRunner = registrationService.registerTracker(scenario.runnerDevice.getUniqueId());

    MessageOsmand msg =
        MessageOsmand.now( // msg should be created by device with id
            scenario.realDevice.getUniqueId(), 52.0, 13.0, 10.0, 20.0, 30.0, null, null);
    trackerReal.sendNow(msg);

    trackerRunner.sendNow(
        MessageOsmand.now(
            scenario.runnerDevice.getUniqueId(), 52.01, 13.01, 0.0, 0.0, 0.0, null, null));

    trackerChaser1 = registrationService.registerTracker(scenario.chaser1Device.getUniqueId());
    trackerChaser1.sendNow(
        MessageOsmand.now(
            scenario.chaser1Device.getUniqueId(), 52.02, 13.02, 0.0, 0.0, 0.0, null, null));

    trackerChaser2 = registrationService.registerTracker(scenario.chaser2Device.getUniqueId());
    trackerChaser2.sendNow(
        MessageOsmand.now(
            scenario.chaser2Device.getUniqueId(), 52.03, 13.03, 0.0, 0.0, 0.0, null, null));

    // listAllBeansInTestContext();

    // now use API to verify that positions are received and stored for devices
    //    api.getDevicesApi()
    //        .getDevices(null)
    //        .forEach(
    //            device -> {
    //              try {
    //                logger.info("\nDevice.id{}: {} ", device.getId(), device);
    //                // assertThat(positions.size()).isGreaterThan(0);
    //              } catch (Exception e) {
    //                logger.error(
    //                    "Error retrieving positions for Device.id{} ({}): {}",
    //                    device.getId(),
    //                    device.getName(),
    //                    e.getMessage());
    //              }
    //            });
  }

  // @Test
  void contextLoadTest() throws Exception {
    logger.info("Verifying that the Spring context loads correctly.");
    assertThat(controller).isNotNull();
    assertThat(scenario).isNotNull();
    assertThat(client).isNotNull();
  }
}
