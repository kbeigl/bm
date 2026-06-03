package bm.gps.tracker;

import bm.gps.GeoTools;
import bm.gps.MessageOsmand;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.OffsetDateTime;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OsmAndTracker is a client @Component that sends GPS messages to the OsmAnd server. We are using
 * the Device (i.e. OsmAnd model) as the tracker status at any given time.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * TrackerOsmAnd tracker = new TrackerOsmAnd("device-123", "http://localhost:5055");
 *
 * // Get the tracker status and modify it directly
 * TrackerStatus status = tracker.getTrackerStatus();
 * status.setLatitude(52.5200);
 * status.setLongitude(13.4050);
 * status.setAltitude(34.5);
 * status.setSpeed(25.0);
 * status.setBearing(180.0);
 * status.setBattery(85.0);
 * status.setFixTime(OffsetDateTime.now());
 *
 * // Send the current status as a message
 * tracker.sendNow();
 * }</pre>
 */
@Component
public class TrackerOsmAnd {

  private static final Logger logger = LoggerFactory.getLogger(TrackerOsmAnd.class);

  /**
   * A fixed host to send messages to and the device this tracker is associated with. Can only be
   * set with constructor and uniqueId is used for all messages.
   */
  // final for runtime ?
  private String osmandHost, uniqueId;

  // Route-id sanitization: safeId uses replacement of non-alphanumeric chars with '_'
  // to make route ids/endpoints safe.
  private String safeId;

  /**
   * Public inner class to encapsulate tracker status attributes. This approach provides flexibility
   * for mathematical functions of motion and physical laws.
   */
  public static class TrackerStatus {
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    // nullable values
    private Double altitude;
    private Double speed;
    private Double bearing;
    private Double battery;
    private OffsetDateTime fixTime;

    public double getLatitude() {
      return latitude;
    }

    public void setLatitude(double latitude) {
      this.latitude = latitude;
    }

    public double getLongitude() {
      return longitude;
    }

    public void setLongitude(double longitude) {
      this.longitude = longitude;
    }

    public Double getAltitude() {
      return altitude;
    }

    public void setAltitude(Double altitude) {
      this.altitude = altitude;
    }

    public Double getSpeed() {
      return speed;
    }

    public void setSpeed(Double speed) {
      this.speed = speed;
    }

    public Double getBearing() {
      return bearing;
    }

    public void setBearing(Double bearing) {
      this.bearing = bearing;
    }

    public Double getBattery() {
      return battery;
    }

    public void setBattery(Double battery) {
      this.battery = battery;
    }

    public OffsetDateTime getFixTime() {
      return fixTime;
    }

    public void setFixTime(OffsetDateTime fixTime) {
      this.fixTime = fixTime;
    }
  }

  /*
   * The user can set individual status attributes as desired. The Tracker always creates one OsmAnd
   * record at one time and transmitts it asap.
   *
   * <p>Using primitives provides more technical flexibility than creating a new MessageOsmand
   * object for each update.
   *
   * <p>This approach leaves possibilities for mathematical functions of motion, i.e. physical laws.
   * For example, we can add a distance threshold to only send messages when the device has moved a
   * certain distance, or an angle threshold to only send messages when the device has changed
   * direction significantly.
   *
   * <p>Types should harmonize with Device and Position Models.
   */
  private final TrackerStatus status = new TrackerStatus();

  // the actual tracker sending endpoint
  @Autowired private ProducerTemplate tracker;
  @Autowired private CamelContext camel;

  public TrackerOsmAnd() {}

  public TrackerOsmAnd(String uniqueId, String osmandHost) {
    this.uniqueId = uniqueId;
    this.osmandHost = osmandHost;
    this.safeId = uniqueId == null ? "" : uniqueId.replaceAll("[^A-Za-z0-9_-]", "_");
  }

  // Getters only for fixed attributes
  public String getOsmandHost() {
    return osmandHost;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  // Getter and setter for the entire TrackerStatus
  public TrackerStatus getTrackerStatus() {
    return status;
  }

  public void setTrackerStatus(TrackerStatus status) {
    // Copy fields from status instance to maintain encapsulation
    // consider using a copy constructor or builder pattern
    // for better encapsulation and immutability
    this.status.setLatitude(status.getLatitude());
    this.status.setLongitude(status.getLongitude());
    this.status.setAltitude(status.getAltitude());
    this.status.setSpeed(status.getSpeed());
    this.status.setBearing(status.getBearing());
    this.status.setBattery(status.getBattery());
    this.status.setFixTime(status.getFixTime());
  }

  // ------------------------------------------------------
  // SEE SimulatorService.java FOR MORE ADVANCED TRACKER FEATURES:
  // add distance threshold
  // add angle threshold
  // add periodic sending
  // ------------------------------------------------------

  MessageOsmand lastMessageSent = null;

  /**
   * Public API to send the current status as a message via the Camel route. Message is always
   * created from Tracker attributes. UniqueId is used for all messages and must be set before
   * sending. Latitude and longitude must be set to valid values before sending. If Fixtime is not
   * set, the message will be created with the current Universal Time Coordinate UTC as the world
   * time standard.
   */
  public void sendTrackerStatus() {
    MessageOsmand msg = createMessage();
    sendMessage(msg);
    lastMessageSent = msg;
  }

  /**
   * Public API to send a message immediately via the Camel route.
   *
   * <p>Note that the message is not validated against the current TrackerStatus attributes, so it
   * is the caller's responsibility to ensure that the message has a matching uniqueId and valid
   * latitude/longitude values. Also the timestamp of the message is not validated. If you send a
   * message with a timestamp in the past, like from a GPX track, it will be sent immediately and
   * not at the original timestamp. This method is intended for sending messages that are already
   * created, for example from a GPX track, and not for sending the current status of the tracker.
   * For sending the current status, use sendTrackerStatus() instead.
   *
   * <p>Visibility will most likely be narrowed to package-private, as this method is intended for
   * internal use.
   */
  // return true/false for success/failure ?
  public void sendMessage(MessageOsmand msg) {

    // validate message uniqueId matches tracker deviceId or ""

    if (tracker == null) {
      logger.error("transmitter not available - cannot send message");
      return;
    }

    /* create final endpoint string with safeId for route registration and message sending
     * this is redundant with the route registration in @PostConstruct
     * but ensures that messages are sent to the correct endpoint
     * even if the no-arg constructor was used and autowired later
     */

    // Synchronous at dev time - send to direct endpoint per-tracker
    String endpoint =
        "direct:send-osmand-"
            + (safeId == null
                ? (uniqueId == null ? "" : uniqueId.replaceAll("[^A-Za-z0-9_-]", "_"))
                : safeId);
    logger.info("tracker sending url message {}", msg); // safeId ?
    tracker.sendBody(endpoint, msg);
    // tracker.asyncSendBody("direct:send-osmand", msg);
  }

  public Double calculateSpeedFromLastMessage() {
    GeoTools.TrackPoint previousPoint = getLastSentTrackPoint();
    GeoTools.TrackPoint currentPoint = getCurrentTrackPoint();
    if (previousPoint == null || currentPoint == null) {
      return null;
    }

    return GeoTools.speedMetersPerSecond(previousPoint, currentPoint);
  }

  public Double calculateBearingFromLastMessage() {
    GeoTools.TrackPoint previousPoint = getLastSentTrackPoint();
    GeoTools.TrackPoint currentPoint = getCurrentTrackPoint();
    if (previousPoint == null || currentPoint == null) {
      return null;
    }

    return GeoTools.normalizedBearingDegrees(previousPoint, currentPoint);
  }

  private GeoTools.TrackPoint getLastSentTrackPoint() {
    MessageOsmand previousMessage = lastMessageSent;
    if (previousMessage == null) {
      return null;
    }

    return new GeoTools.TrackPoint(
        previousMessage.lat(),
        previousMessage.lon(),
        previousMessage.timestamp(),
        previousMessage.altitude());
  }

  private GeoTools.TrackPoint getCurrentTrackPoint() {
    if (Double.isNaN(status.getLatitude()) || Double.isNaN(status.getLongitude())) {
      return null;
    }

    long currentTimestamp =
        status.getFixTime() == null
            ? System.currentTimeMillis() / 1000L
            : status.getFixTime().toEpochSecond();

    return new GeoTools.TrackPoint(
        status.getLatitude(), status.getLongitude(), currentTimestamp, status.getAltitude());
  }

  private MessageOsmand createMessage() {

    if (uniqueId == null || uniqueId.isEmpty()) {
      throw new IllegalStateException("Tracker uniqueId is not set");
    }
    if (Double.isNaN(status.getLatitude()) || Double.isNaN(status.getLongitude())) {
      throw new IllegalStateException("Tracker latitude or longitude is not set");
    }

    /*
     * Time is defined in Universal Time Coordinate UTC as the world time standard.
     * Check time format against traccar/osmand requirements.
     */
    if (status.getFixTime() == null) {
      // MessageOsmand.now requires primitive values for speed/bearing/altitude,
      // use 0.0 when missing
      return MessageOsmand.now(
          uniqueId,
          status.getLatitude(),
          status.getLongitude(),
          status.getSpeed() == null ? 0.0 : status.getSpeed().doubleValue(),
          status.getBearing() == null ? 0.0 : status.getBearing().doubleValue(),
          status.getAltitude() == null ? 0.0 : status.getAltitude().doubleValue(),
          status.getBattery(), // can be null
          null);
    } else {
      return new MessageOsmand(
          uniqueId,
          status.getLatitude(),
          status.getLongitude(),
          status.getFixTime().toEpochSecond(),
          status.getSpeed() == null ? null : Double.valueOf(status.getSpeed().doubleValue()),
          status.getBearing() == null ? null : Double.valueOf(status.getBearing().doubleValue()),
          status.getAltitude() == null ? null : status.getAltitude(),
          status.getBattery(),
          null);
    }
  }

  // route registration must happen after dependency injection, so we use @PostConstruct.
  // consider CamelContextAware to avoid jakarta annotation ?
  // or use ApplicationContextAware and get CamelContext from there
  // or use @PostConstruct from Camel CDI or Spring
  @PostConstruct
  private void initRoutes() {
    if (osmandHost == null) return;
    if (camel == null) {
      logger.error("CamelContext not available in @PostConstruct for TrackerOsmAnd");
      return;
    }
    // ensure safeId is initialized in case the no-arg constructor was used and autowired later
    if (safeId == null) {
      safeId = uniqueId == null ? "" : uniqueId.replaceAll("[^A-Za-z0-9_-]", "_");
    }
    // Use SEDA-based Sender so Tracker sends to a queue consumed by the Sender route
    TrackerSender routes = new TrackerSender(osmandHost, uniqueId);
    try {
      camel.addRoutes(routes);
      // routesRegistered = true;
      logger.info("Registered TrackerRoutes for Tracker-{} to Host {}", safeId, osmandHost);
    } catch (Exception e) {
      logger.error("Failed to register TrackerRoutes for {}", osmandHost, e);
    }
  }

  private String getRouteId(String prefix) {
    return prefix
        + (safeId == null
            ? (uniqueId == null ? "" : uniqueId.replaceAll("[^A-Za-z0-9_-]", "_"))
            : safeId);
  }

  @PreDestroy
  private void destroyRoutes() {
    if (camel == null) {
      return;
    }
    String directRouteId = getRouteId("send-osmand-route-");
    String sedaRouteId = getRouteId("seda-send-osmand-route-");
    // redundant code - extract to helper method
    try {
      if (camel.getRouteController().getRouteStatus(directRouteId) != null) {
        camel.getRouteController().stopRoute(directRouteId);
        camel.removeRoute(directRouteId);
        logger.debug("Stopped and removed tracker route {}", directRouteId);
      }
    } catch (Exception e) {
      logger.warn("Failed to remove tracker route {}: {}", directRouteId, e.getMessage());
    }
    // redundant code
    try {
      if (camel.getRouteController().getRouteStatus(sedaRouteId) != null) {
        camel.getRouteController().stopRoute(sedaRouteId);
        camel.removeRoute(sedaRouteId);
        logger.debug("Stopped and removed tracker route {}", sedaRouteId);
      }
    } catch (Exception e) {
      logger.warn("Failed to remove tracker route {}: {}", sedaRouteId, e.getMessage());
    }
  }
}
