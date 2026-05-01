package bm.gps;

import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public final class GeoTools {
  private GeoTools() {}

  /**
   * Helper to represent a GPS track point with timestamp. Can be used to calculate speed and
   * bearing between points.
   */
  public record TrackPoint(double latitude, double longitude, long timestamp, Double altitude) {}

  private static final CoordinateReferenceSystem wgs84;

  static {
    CoordinateReferenceSystem crs = null;
    try {
      crs = CRS.decode("EPSG:4326"); // i.e. WGS84 lat/lon
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize WGS84 CRS", e);
    }
    wgs84 = crs;
  }

  // Haversine 2d distance in meters using GeoTools
  public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
    GeodeticCalculator calc = new GeodeticCalculator(wgs84);
    calc.setStartingGeographicPoint(lon1, lat1);
    calc.setDestinationGeographicPoint(lon2, lat2);
    return calc.getOrthodromicDistance();
  }

  // Initial 2d bearing in degrees from point1 to point2 using GeoTools
  public static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
    GeodeticCalculator calc = new GeodeticCalculator(wgs84);
    calc.setStartingGeographicPoint(lon1, lat1);
    calc.setDestinationGeographicPoint(lon2, lat2);
    return calc.getAzimuth();
  }

  public static Double speedMetersPerSecond(TrackPoint previous, TrackPoint current) {
    double distanceMeters =
        distanceMeters(
            previous.latitude(), previous.longitude(), current.latitude(), current.longitude());
    long deltaSeconds = current.timestamp() - previous.timestamp();
    if (deltaSeconds > 0L) {
      return Double.valueOf(distanceMeters / deltaSeconds);
    }
    if (distanceMeters == 0.0d) {
      return Double.valueOf(0.0d);
    }
    return null;
  }

  public static Double normalizedBearingDegrees(TrackPoint previous, TrackPoint current) {
    double distanceMeters =
        distanceMeters(
            previous.latitude(), previous.longitude(), current.latitude(), current.longitude());
    if (distanceMeters <= 0.01d) {
      return null;
    }

    return Double.valueOf(
        normalizeBearing(
            bearingDegrees(
                previous.latitude(),
                previous.longitude(),
                current.latitude(),
                current.longitude())));
  }

  private static double normalizeBearing(double bearing) {
    double normalized = bearing % 360.0d;
    return normalized < 0.0d ? normalized + 360.0d : normalized;
  }
}
