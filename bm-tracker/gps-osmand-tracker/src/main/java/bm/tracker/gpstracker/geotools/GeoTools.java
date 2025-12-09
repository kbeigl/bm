package bm.tracker.gpstracker.geotools;

import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public final class GeoTools {
  private GeoTools() {}

  private static final CoordinateReferenceSystem wgs84;

  static {
    CoordinateReferenceSystem crs = null;
    try {
      crs = CRS.decode("EPSG:4326");
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize WGS84 CRS", e);
    }
    wgs84 = crs;
  }

  // Haversine distance in meters using GeoTools
  public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
    GeodeticCalculator calc = new GeodeticCalculator(wgs84);
    calc.setStartingGeographicPoint(lon1, lat1);
    calc.setDestinationGeographicPoint(lon2, lat2);
    return calc.getOrthodromicDistance();
  }

  // Initial bearing in degrees from point1 to point2 using GeoTools
  public static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
    GeodeticCalculator calc = new GeodeticCalculator(wgs84);
    calc.setStartingGeographicPoint(lon1, lat1);
    calc.setDestinationGeographicPoint(lon2, lat2);
    return calc.getAzimuth();
  }
}
