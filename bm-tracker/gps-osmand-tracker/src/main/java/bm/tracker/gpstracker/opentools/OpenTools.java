package bm.tracker.gpstracker.opentools;

public final class OpenTools {
  private OpenTools() {}

  // Haversine distance in meters
  public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
    final int R = 6371000; // Radius of the earth in meters
    double latRad1 = Math.toRadians(lat1);
    double latRad2 = Math.toRadians(lat2);
    double deltaLat = Math.toRadians(lat2 - lat1);
    double deltaLon = Math.toRadians(lon2 - lon1);

    double a =
        Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(latRad1)
                * Math.cos(latRad2)
                * Math.sin(deltaLon / 2)
                * Math.sin(deltaLon / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  // Initial bearing in degrees from point1 to point2
  public static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
    double phi1 = Math.toRadians(lat1);
    double phi2 = Math.toRadians(lat2);
    double lambda1 = Math.toRadians(lon1);
    double lambda2 = Math.toRadians(lon2);

    double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
    double x =
        Math.cos(phi1) * Math.sin(phi2)
            - Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
    double theta = Math.atan2(y, x);
    double bearing = Math.toDegrees(theta);
    bearing = (bearing + 360) % 360;
    return bearing;
  }
}
