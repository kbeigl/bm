package bm.traccar.rt;

import bm.gps.MessageOsmand;
import bm.gps.tracker.GpsOsmandTrackerConfig;
import bm.gps.tracker.TrackerOsmAnd;
import bm.gps.tracker.TrackerRegistration;
import bm.traccar.BaseRealTimeClientTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test is only sending messages with devices, but no assertions yet. This is more of a manual test
 * to check if messages are received in the Traccar server and if the WebSocket route is working.
 */
@ActiveProfiles("test")
@Import(GpsOsmandTrackerConfig.class)
class TrackerMessagesIT extends BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(TrackerMessagesIT.class);

  @Autowired TrackerRegistration registrationService;

  @Test
  void sendMessagesWithDevices() throws Exception {
    TrackerOsmAnd trackerReal, trackerRunner, trackerChaser1, trackerChaser2;

    logger.info("Setup scenario devices");
    // lookup tracker by name + device id ?
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

    // what is the websocket resend period ?
    sleep(5000);
  }
}
