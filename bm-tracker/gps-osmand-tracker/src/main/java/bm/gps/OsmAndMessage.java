package bm.gps;

/**
 * A simple record to hold a single GPS location snapshot. Records are an efficient way to create
 * immutable data classes in modern Java.
 */
public record OsmAndMessage(
    double latitude,
    double longitude,
    long timestamp,
    double speed,
    double bearing,
    double altitude,
    int battery,
    double hdop) {

  /**
   * Helper constructor to create a GpsData instance using the current system time for the timestamp
   * field.
   *
   * @return A new GpsData record with timestamp 'now'.
   */
  public static OsmAndMessage now(
      double lat, double lon, double speed, double bearing, double alt, int batt, double hdop) {
    // epoch seconds, not milliseconds, so we divide by 1000L
    return new OsmAndMessage(
        lat, lon, System.currentTimeMillis() / 1000L, speed, bearing, alt, batt, hdop);
  }
}
