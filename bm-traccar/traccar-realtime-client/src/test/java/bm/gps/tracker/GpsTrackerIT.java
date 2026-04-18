package bm.gps.tracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bm.gps.MessageOsmand;
import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test for gps-tracker using the full scenario setup from BaseRealTimeClientTest for a
 * full end-to-end test of the tracker component in the real-time client context.
 */
public class GpsTrackerIT extends BaseGpsTrackerIT {

  private static final Logger logger = LoggerFactory.getLogger(GpsTrackerIT.class);

  @Test
  void sendMessagesWithDevices() throws Exception {
    logger.info("\n\t\t********** sendMessagesWithDevices() **********");
    // list of trackers to be processed
    List<TrackerOsmAnd> trackers =
        List.of(runnerTracker, mobileTracker, chaser1Tracker, chaser2Tracker);
    // a tracker is represented as a Device with positionId before sending messages
    Map<Device, Long> devices = new LinkedHashMap<>();
    // if (!trackers.isEmpty()) {
    for (TrackerOsmAnd tracker : trackers) {
      controller
          .findDeviceByUniqueId(tracker.getUniqueId())
          .ifPresent(device -> devices.put(device, device.getPositionId()));
    }
    // send a single message from subsequent trackers
    runnerTracker.sendNow(
        MessageOsmand.now(runnerTracker.getUniqueId(), 52d, 13d, 10d, 20d, 30d, null, null));
    chaser1Tracker.sendNow(
        MessageOsmand.now(chaser1Tracker.getUniqueId(), 52.01, 13.01, 10d, 20d, 30d, null, null));
    // wait a second
    sleep(1000);
    MessageOsmand msg =
        MessageOsmand.now(chaser2Tracker.getUniqueId(), 52.02, 13.02, 10.0, 20.0, 30.0, null, null);
    chaser2Tracker.sendNow(msg);
    mobileTracker.sendNow(
        MessageOsmand.now(mobileTracker.getUniqueId(), 52.03, 13.03, 10.0, 20.0, 30.0, null, null));

    awaitPositions(devices);
  }

  // await processing of all messages in controller
  private void awaitPositions(Map<Device, Long> devices) {
    // wait for processing as analyzed in singleMsg2gts2realtime()
    // wait for positionId to be updated for all devices
    sleep(4000);
    while (true) {
      boolean allUpdated = true;
      for (Map.Entry<Device, Long> entry : devices.entrySet()) {
        Device device = entry.getKey();
        Long currentPositionId = entry.getValue();
        if (currentPositionId == device.getPositionId()) {
          allUpdated = false;
          break;
        }
        logger.info("DeviceStatus: {}", getDeviceStatus(device));
        controller
            .getLatestPositionForDevice(device.getId())
            .ifPresent(
                pos ->
                    logger.info(
                        "Latest Position for Device {}: id={}, fixTime={}, serverTime={}",
                        device.getUniqueId(),
                        pos.getId(),
                        pos.getFixTime(),
                        pos.getServerTime()));
      }
      if (allUpdated) break;
      sleep(1000);
    }
    // determine latency (after fix?)
  }

  /**
   * Anatomy of a single message traveling from tracker to controller via server:
   *
   * <p>Tracker sending a single message and observe the real-time updates in the controller. This
   * verifies that the message is processed, the device is created/updated, and the position is
   * eventually available in the controller and device status is updated to 'online'.
   *
   * <p>Note: This test relies on observing logs and state changes in the controller, as the
   * processing is asynchronous. We check the device status and position updates after sending the
   * message, and we can also check the timestamps to understand how the system processes the time
   * data from the GPS message.
   */
  @Test
  void singleMsg2gts2realtime() throws Exception {
    logger.info("\t********** singleMsg2gts2realtime() **********");

    // uniqueId links tracker and controller - in test environment only!
    String uniqueId = runnerTracker.getUniqueId();

    // references can be fetched anytime, even before sending -> app init
    Optional<Device> sendingDeviceOpt = controller.findDeviceByUniqueId(uniqueId);
    Device sendingDevice = sendingDeviceOpt.orElse(null);
    assertNotNull(sendingDevice, "Device should be created in controller on app init");
    logger.debug("Fetched RTM Device reference with uniqueId {} before sending.", uniqueId);
    logger.info("Status before sending: {}", getDeviceStatus(sendingDevice));

    // clarify timestamps and timezones: ------------------
    // local/log 16:32:55.940 vs fix 2026-04-27T14:32:55Z
    // standardize bm framework: user friendly local vs UTC time
    // device time (gps fix time) vs server time (message receipt time)

    // prepare message with GPS fix in app context ... ----
    long devFixTime = System.currentTimeMillis() / 1000;
    OffsetDateTime devFixTimeFormat = // derive central method for GPS toolong
        OffsetDateTime.ofInstant(Instant.ofEpochSecond(devFixTime), ZoneOffset.UTC);
    logger.info("GPS fix {} ts: {}", devFixTimeFormat, devFixTime);
    MessageOsmand msg =
        new MessageOsmand(uniqueId, 52.02, 13.02, devFixTime, 0.0, 0.0, 0.0, null, null);
    //  MessageOsmand.now(uniqueId, 52.02, 13.02,        , 10.0, 20.0, 30.0, null, null);
    assertEquals(devFixTime, msg.timestamp()); // ;)

    // .. and send some other time ========================
    sleep(3000);
    long devSentTime = System.currentTimeMillis() / 1000;
    OffsetDateTime devSentTimeFormat =
        OffsetDateTime.ofInstant(Instant.ofEpochSecond(devSentTime), ZoneOffset.UTC);
    logger.info("Sent at {} ts: {}", devSentTimeFormat, devSentTime);

    runnerTracker.sendNow(msg);

    // === receive Device =================================
    // device status and lastUpdate update *almost* immediately
    sleep(500);
    // wait for processing
    // DeviceProcessor - received 1 device event message/s.
    // RealTimeManager - addOrUpdateDevice: device(id=3040 / uniqueId=10)
    // DeviceProcessor - Updated Device (id=3040 / uniqueId=10)
    logger.info(" Status after sending: {}", getDeviceStatus(sendingDevice));
    assertEquals(
        "online", sendingDevice.getStatus(), "Device not 'online' after receiving message");

    // assert that sendingDevice.getLastUpdate() is close to devSentTime (e.g. within 5s)
    OffsetDateTime lastUpdate = sendingDevice.getLastUpdate();
    assertNotNull(lastUpdate, "Device lastUpdate should hold 'received' timestamp");

    long lastUpdateEpoch = lastUpdate.toEpochSecond();
    long diffSeconds = Math.abs(lastUpdateEpoch - devSentTime);
    assertEquals(
        0,
        diffSeconds / 1,
        "Device lastUpdate should be one second of send time. diff: " + diffSeconds + "s");

    // conclude: dev.getLastUpdate() is (close to ) message send/receipt time, not GPS fix time

    // === receive Position ===============================
    // await position processor to receive and update position (local test ~ 3-4s)
    Position pos = awaitPositionForDevice(sendingDevice.getId(), 5000L);
    assertNotNull(pos, "Position not available in controller after processing message");
    logger.info("Fetched RTM Position: {}", pos);

    assertEquals(pos.getDeviceTime(), pos.getFixTime(), "Position deviceTime should match fixTime");

    /* conclude:
     * OsmAnd message timestamp is used as deviceTime and fixTime in Position,
     * and device lastUpdate is the message receipt time in controller,
     * which is later than the GPS fix time in the message.
     * This means that the controller reflects the time of receiving the message,
     * not the time of the GPS fix, for device status updates.
     * The position timestamps are derived from the message timestamp, not the receipt time.
     * This is how the system processes and represents time data from GPS messages.
     *
     * We could transfer the deviceTime/fixTime to the device lastUpdate,
     * The current design allows us to distinguish between when the GPS fix was taken
     * and when the message was processed by the controller (device lastUpdate)
     */

    OffsetDateTime expectedFixTime =
        OffsetDateTime.ofInstant(Instant.ofEpochSecond(devFixTime), ZoneOffset.UTC);
    assertEquals(expectedFixTime, pos.getFixTime(), "Position fixTime <unequal> message timestamp");

    // finally the device position should be updated to the new position
    assertNotNull(
        sendingDevice.getPositionId(), "Device positionId should be set after position update");
    assertEquals(
        pos.getId(),
        sendingDevice.getPositionId(),
        "Device should reference the latest position id");
    assertEquals(
        pos.getServerTime(),
        sendingDevice.getLastUpdate(),
        "Device update and Position serverTime should match");

    logger.info(" Status after  update: {}", getDeviceStatus(sendingDevice));
  }

  // controller candidate: onPositionUpdate(deviceId) ? rather not !
  private Position awaitPositionForDevice(long deviceId, long timeoutMs)
      throws InterruptedException {
    long start = System.currentTimeMillis();
    while ((System.currentTimeMillis() - start) < timeoutMs) {
      Optional<Position> posOpt = controller.getLatestPositionForDevice(deviceId);
      if (posOpt.isPresent()) return posOpt.get();
      Thread.sleep(100L);
    }
    return null;
  }

  private String getDeviceStatus(Device device) {
    if (device == null) {
      return "Device is null";
    }
    return String.format(
        "Device(%s,  id=%s): %s / %s / %s",
        device.getUniqueId(),
        device.getId(),
        device.getStatus(),
        device.getPositionId(),
        device.getLastUpdate());
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
}
