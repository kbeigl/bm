package bm.gps.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import bm.gps.tracker.TrackerOsmAnd.TrackerStatus;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * Unit test demonstrating how to use the new TrackerStatus API. This test shows how users can get
 * and set tracker status using the TrackerStatus object.
 */
class TrackerStatusUsageTest {

  @Test
  void testGetAndModifyTrackerStatus() {
    // Create a tracker
    TrackerOsmAnd tracker = new TrackerOsmAnd("test-device-123", "http://localhost:5055");

    // Get the tracker status
    TrackerStatus status = tracker.getTrackerStatus();
    assertNotNull(status, "TrackerStatus should not be null");

    // Modify status fields directly
    status.setLatitude(52.5200);
    status.setLongitude(13.4050);
    status.setAltitude(34.5);
    status.setSpeed(25.0);
    status.setBearing(180.0);
    status.setBattery(85.0);
    status.setFixTime(OffsetDateTime.now());

    // Verify the changes
    assertEquals(52.5200, status.getLatitude(), 0.0001);
    assertEquals(13.4050, status.getLongitude(), 0.0001);
    assertEquals(34.5, status.getAltitude(), 0.01);
    assertEquals(25.0, status.getSpeed(), 0.01);
    assertEquals(180.0, status.getBearing(), 0.01);
    assertEquals(85.0, status.getBattery(), 0.01);
    assertNotNull(status.getFixTime());
  }

  @Test
  void testSetTrackerStatus() {
    // Create a tracker
    TrackerOsmAnd tracker = new TrackerOsmAnd("test-device-456", "http://localhost:5055");

    // Create a new status object with desired values
    TrackerStatus newStatus = new TrackerStatus();
    newStatus.setLatitude(48.8566);
    newStatus.setLongitude(2.3522);
    newStatus.setAltitude(35.0);
    newStatus.setSpeed(15.5);
    newStatus.setBearing(90.0);
    newStatus.setBattery(75.0);
    newStatus.setFixTime(OffsetDateTime.now());

    // Set the tracker status (this copies all fields)
    tracker.setTrackerStatus(newStatus);

    // Verify the tracker has the new status
    TrackerStatus currentStatus = tracker.getTrackerStatus();
    assertEquals(48.8566, currentStatus.getLatitude(), 0.0001);
    assertEquals(2.3522, currentStatus.getLongitude(), 0.0001);
    assertEquals(35.0, currentStatus.getAltitude(), 0.01);
    assertEquals(15.5, currentStatus.getSpeed(), 0.01);
    assertEquals(90.0, currentStatus.getBearing(), 0.01);
    assertEquals(75.0, currentStatus.getBattery(), 0.01);
    assertNotNull(currentStatus.getFixTime());
  }

  @Test
  void testTrackerStatusInitialValues() {
    // Create a new tracker
    TrackerOsmAnd tracker = new TrackerOsmAnd("test-device-789", "http://localhost:5055");

    // Get the initial status
    TrackerStatus status = tracker.getTrackerStatus();

    // Verify initial values (latitude and longitude should be NaN, others should be null)
    assertEquals(Double.NaN, status.getLatitude());
    assertEquals(Double.NaN, status.getLongitude());
    assertEquals(null, status.getAltitude());
    assertEquals(null, status.getSpeed());
    assertEquals(null, status.getBearing());
    assertEquals(null, status.getBattery());
    assertEquals(null, status.getFixTime());
  }

  @Test
  void testTrackerStatusBuilderPattern() {
    // Demonstrate a fluent builder-like pattern for setting multiple values
    TrackerOsmAnd tracker = new TrackerOsmAnd("test-device-999", "http://localhost:5055");

    TrackerStatus status = tracker.getTrackerStatus();

    // Set multiple values in a fluent manner
    status.setLatitude(51.5074);
    status.setLongitude(-0.1278);
    status.setAltitude(11.0);
    status.setSpeed(30.0);
    status.setBearing(270.0);
    status.setBattery(90.0);
    status.setFixTime(OffsetDateTime.now());

    // Verify all values are set correctly
    assertEquals(51.5074, status.getLatitude(), 0.0001);
    assertEquals(-0.1278, status.getLongitude(), 0.0001);
    assertEquals(11.0, status.getAltitude(), 0.01);
    assertEquals(30.0, status.getSpeed(), 0.01);
    assertEquals(270.0, status.getBearing(), 0.01);
    assertEquals(90.0, status.getBattery(), 0.01);
    assertNotNull(status.getFixTime());
  }
}
