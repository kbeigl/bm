package bm.gps.tracker.camel;

import bm.gps.OsmAndMessage;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.stereotype.Service;

/**
 * Manages the simulator's state and determines if a new GPS update should be sent. It acts as a
 * processor in the Camel route.
 */
@Service
public class SimulatorService {

  private static final Logger LOGGER = Logger.getLogger(SimulatorService.class.getName());

  // Configuration
  private static final double DISTANCE_THRESHOLD_METERS = 100;
  private static final double CENTER_LAT = 52.5200; // Berlin
  private static final double CENTER_LON = 13.4050;
  private static final double RADIUS = 0.005; // ~500m radius

  // State
  private int steps = 0;
  private OsmAndMessage lastSentData =
      OsmAndMessage.now(CENTER_LAT, CENTER_LON, 0.0, 0.0, 50.0, 100, 0);

  /**
   * Camel Handler method that simulates movement, checks distance, and updates the state. * @param
   * exchange The incoming Camel exchange (initially empty from the timer).
   *
   * @return The new GpsData record if the distance threshold is crossed, or null otherwise.
   */
  @Handler
  public OsmAndMessage updateLocation(Exchange exchange) {
    // --- 1. Calculate the *next potential* position ---
    double angle = Math.toRadians(steps * 0.5); // 0.5 degrees per tick

    double newLat = CENTER_LAT + (RADIUS * Math.cos(angle));
    double newLon = CENTER_LON + (RADIUS * Math.sin(angle));

    // --- 2. Calculate distance from the *last sent* position using GeoTools ---
    double distanceMoved =
        geotoolsDistance(
            getLastSentData().latitude(), getLastSentData().longitude(), newLat, newLon);

    // --- 3. Check if threshold is met ---
    if (distanceMoved >= DISTANCE_THRESHOLD_METERS) {

      LOGGER.info(
          String.format("Moved %.1f meters. Threshold met. Preparing update...", distanceMoved));

      double newBearing = (steps * 0.5 + 90) % 360; // Tangent to circle

      // Create new data record (this becomes the new message body)
      OsmAndMessage newGpsData =
          OsmAndMessage.now(
              newLat,
              newLon,
              15.0, // Speed 15 m/s
              newBearing,
              50.0 + (Math.sin(angle) * 10), // Vary altitude
              100,
              0);

      // Update state
      setLastSentData(newGpsData);
      steps++;

      return newGpsData;
    }

    // No update needed, advance step count and return null to stop the route
    steps++;
    return null;
  }

  /**
   * Calculates the geodesic distance in meters between two lat/lon points using the GeoTools
   * GeodeticCalculator with WGS84 (EPSG:4326).
   */
  private static double geotoolsDistance(double lat1, double lon1, double lat2, double lon2) {
    try {
      CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
      GeodeticCalculator calculator = new GeodeticCalculator(wgs84);

      // Set coordinates (Longitude, Latitude)
      calculator.setStartingGeographicPoint(lon1, lat1);
      calculator.setDestinationGeographicPoint(lon2, lat2);

      return calculator.getOrthodromicDistance();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error calculating geodesic distance with GeoTools", e);
      return 0.0;
    }
  }

  public OsmAndMessage getLastSentData() {
    return lastSentData;
  }

  public void setLastSentData(OsmAndMessage lastSentData) {
    this.lastSentData = lastSentData;
  }
}
