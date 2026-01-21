package bm.gps.tracker;

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
   * The user can set individual status attributes as desired. The Tracker always creates one OsmAnd
   * record at one time and transmitts it asap.
   *
   * <p>Types should harmonize with Device and Position models.
   */
  private double latitude = Double.NaN;

  private double longitude = Double.NaN;
  // nullable values
  private Double altitude;
  private Double speed;
  private Double bearing;
  private Double battery;
  private OffsetDateTime fixTime;
  // TODO private OffsetDateTime deviceTime;

  @Autowired private ProducerTemplate tracker;
  @Autowired private CamelContext camel;

  public TrackerOsmAnd() {}

  public TrackerOsmAnd(String uniqueId, String osmandHost) {
    this.uniqueId = uniqueId;
    this.osmandHost = osmandHost;
    this.safeId = uniqueId == null ? "" : uniqueId.replaceAll("[^A-Za-z0-9_-]", "_");
  }

  @PreDestroy
  private void destroyRoutes() {
    logger.debug("Destroying TrackerOsmAnd routes for uniqueId={}", uniqueId);

    if (camel == null) return;

    String routeId =
        "send-osmand-route-"
            + (safeId == null
                ? (uniqueId == null ? "" : uniqueId.replaceAll("[^A-Za-z0-9_-]", "_"))
                : safeId);
    try {
      if (camel.getRouteController().getRouteStatus(routeId) != null) {
        camel.getRouteController().stopRoute(routeId);
        camel.removeRoute(routeId);
        logger.info("Stopped and removed tracker route {}", routeId);
      }
    } catch (Exception e) {
      logger.warn("Failed to remove tracker route {}: {}", routeId, e.getMessage());
    }
  }

  // Getters only for fixed attributes
  public String getOsmandHost() {
    return osmandHost;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  // Getters and setters for location/time attributes
  public double getLatitude() {
    return latitude;
  }

  // offer 2d (lat, lon) and 3d (alt) Geo coordinates -----
  // JTS Coordinate to hold (lon, lat, alt) -> (x, y, z) mutable states
  // private final Coordinate coordinate;

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

  // ------------------------------------------------------
  // Other telemetry data (not part of geometry)

  public Double getSpeed() {
    return speed;
  }

  // meters/second ?
  public void setSpeed(Double speed) {
    this.speed = speed;
  }

  public Double getBearing() {
    return bearing;
  }

  // degrees (0-360)
  public void setBearing(Double bearing) {
    this.bearing = bearing;
  }

  public Double getBattery() {
    return battery;
  }

  // 0-100
  public void setBattery(Double battery) {
    this.battery = battery;
  }

  public OffsetDateTime getFixTime() {
    return fixTime;
  }

  public void setFixTime(OffsetDateTime fixTime) {
    this.fixTime = fixTime;
  }

  // ------------------------------------------------------
  // SEE SimulatorService.java FOR MORE ADVANCED TRACKER FEATURES:
  // add distance threshold
  // add angle threshold
  // add periodic sending
  // ------------------------------------------------------

  MessageOsmand lastMessageSent = null;

  private MessageOsmand createMessage() {

    if (uniqueId == null || uniqueId.isEmpty()) {
      throw new IllegalStateException("Tracker uniqueId is not set");
    }
    if (Double.isNaN(latitude) || Double.isNaN(longitude)) {
      throw new IllegalStateException("Tracker latitude or longitude is not set");
    }

    /*
     * Time is defined in Universal Time Coordinate UTC as the world time standard.
     * Check time format against traccar/osmand requirements.
     */
    if (fixTime == null) {
      // MessageOsmand.now requires primitive values for speed/bearing/altitude, use 0.0 when
      // missing
      return MessageOsmand.now(
          uniqueId,
          latitude,
          longitude,
          speed == null ? 0.0 : speed.doubleValue(),
          bearing == null ? 0.0 : bearing.doubleValue(),
          altitude == null ? 0.0 : altitude.doubleValue(),
          battery, // can be null
          null);
    } else {
      return new MessageOsmand(
          uniqueId,
          latitude,
          longitude,
          fixTime.toEpochSecond(),
          speed == null ? null : Double.valueOf(speed.doubleValue()),
          bearing == null ? null : Double.valueOf(bearing.doubleValue()),
          altitude == null ? null : altitude,
          battery,
          null);
    }
  }

  /**
   * Public API to send the current status as a message via the Camel route. Message is always
   * created from Tracker attributes.
   */
  public void sendNow() {
    MessageOsmand msg = createMessage();
    sendNow(msg);
    lastMessageSent = msg;
  }

  /** Public API to send a message immediately via the Camel route. */
  // return true/false for success/failure ?
  public void sendNow(MessageOsmand msg) {

    // validate message uniqueId matches tracker deviceId or ""

    // set Traccar attributes from immutable message !

    if (tracker == null) {
      logger.error("transmitter not available - cannot send message");
      return;
    }

    // Synchronous at dev time - send to per-tracker direct endpoint
    String endpoint =
        "direct:send-osmand-"
            + (safeId == null
                ? (uniqueId == null ? "" : uniqueId.replaceAll("[^A-Za-z0-9_-]", "_"))
                : safeId);
    logger.info("tracker-{} sending message", uniqueId); // safeId ?
    tracker.sendBody(endpoint, msg);
    // tracker.asyncSendBody("direct:send-osmand", msg);
  }

  /*
   * Actual route registration must happen after dependency injection, so we use @PostConstruct.
   */
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
    Transmitter routes = new Transmitter(osmandHost, uniqueId);
    try {
      camel.addRoutes(routes);
      // routesRegistered = true;
      logger.info("Registered TrackerRoutes for host {}", osmandHost);
    } catch (Exception e) {
      logger.error("Failed to register TrackerRoutes for {}", osmandHost, e);
    }
  }
}
