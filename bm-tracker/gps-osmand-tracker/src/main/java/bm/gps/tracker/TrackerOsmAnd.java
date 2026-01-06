package bm.gps.tracker;

import bm.gps.MessageOsmand;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
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
  private String osmandHost, uniqueId;

  /**
   * The user can set individual status attributes as desired. The Tracker always creates one OsmAnd
   * record at one time and transmitts it asap.
   *
   * <p>Types according to Device and Position models.
   */
  // Location/time attributes
  // more precision than Osmand messages ?
  private BigDecimal latitude;

  private BigDecimal longitude;
  private BigDecimal altitude;
  private BigDecimal speed;
  private BigDecimal bearing;
  private BigDecimal battery;
  private OffsetDateTime fixTime;

  @Autowired private ProducerTemplate tracker;
  @Autowired private CamelContext camel;

  public TrackerOsmAnd() {}

  public TrackerOsmAnd(String uniqueId, String osmandHost) {
    this.uniqueId = uniqueId;
    this.osmandHost = osmandHost;
  }

  // Getters and setters for location/time attributes
  public BigDecimal getLatitude() {
    return latitude;
  }

  // offer 2d (lat, lon) and 3d (alt) Geo coordinates -----
  // JTS Coordinate to hold (lon, lat, alt) -> (x, y, z) mutable states
  // private final Coordinate coordinate;

  public void setLatitude(BigDecimal latitude) {
    this.latitude = latitude;
  }

  public BigDecimal getLongitude() {
    return longitude;
  }

  public void setLongitude(BigDecimal longitude) {
    this.longitude = longitude;
  }

  public BigDecimal getAltitude() {
    return altitude;
  }

  public void setAltitude(BigDecimal altitude) {
    this.altitude = altitude;
  }

  // ------------------------------------------------------
  // Other telemetry data (not part of geometry)

  public BigDecimal getSpeed() {
    return speed;
  }

  // meters/second ?
  public void setSpeed(BigDecimal speed) {
    this.speed = speed;
  }

  public BigDecimal getBearing() {
    return bearing;
  }

  // degrees (0-360)
  public void setBearing(BigDecimal bearing) {
    this.bearing = bearing;
  }

  public BigDecimal getBattery() {
    return battery;
  }

  // 0-100
  public void setBattery(BigDecimal battery) {
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

  private void createMessage() {

    if (uniqueId == null || uniqueId.isEmpty()) {
      throw new IllegalStateException("Tracker uniqueId is not set");
    }
    if (latitude == null || longitude == null) {
      throw new IllegalStateException("Tracker latitude or longitude is not set");
    }

    /*
     * Time is defined in Universal Time Coordinate UTC as the world time standard.
     * Check time format against traccar/osmand requirements.
     */
    if (fixTime == null) {
      // MessageOsmand.now requires primitive values for speed/bearing/altitude, use 0.0 when
      // missing
      MessageOsmand msg =
          MessageOsmand.now(
              uniqueId,
              latitude.doubleValue(),
              longitude.doubleValue(),
              speed == null ? 0.0 : speed.doubleValue(),
              bearing == null ? 0.0 : bearing.doubleValue(),
              altitude == null ? 0.0 : altitude.doubleValue(),
              battery == null ? null : battery.doubleValue(),
              null);
      lastMessageSent = msg;
    } else {
      MessageOsmand msg =
          new MessageOsmand(
              uniqueId,
              latitude.doubleValue(),
              longitude.doubleValue(),
              fixTime.toEpochSecond(),
              speed == null ? null : Double.valueOf(speed.doubleValue()),
              bearing == null ? null : Double.valueOf(bearing.doubleValue()),
              altitude == null ? null : Double.valueOf(altitude.doubleValue()),
              battery == null ? null : battery.doubleValue(),
              null);
      lastMessageSent = msg;
    }
  }

  /**
   * Public API to send the current status as a message via the Camel route. Message is always
   * created from Tracker status.
   */
  public void send() {
    createMessage();
    send(lastMessageSent);
  }

  // Public API to send a message via the Camel route
  public void send(MessageOsmand msg) {

    // validate message deviceId matches tracker deviceId

    // set Traccar attributes from immutable message !

    if (tracker == null) {
      logger.error("transmitter not available - cannot send message");
      return;
    }

    // Synchronous at dev time
    tracker.sendBody("direct:send-osmand", msg);
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
    Transmitter routes = new Transmitter(osmandHost);
    try {
      camel.addRoutes(routes);
      // routesRegistered = true;
      logger.info("Registered TrackerRoutes for host {}", osmandHost);
    } catch (Exception e) {
      logger.error("Failed to register TrackerRoutes for {}", osmandHost, e);
    }
  }
}
