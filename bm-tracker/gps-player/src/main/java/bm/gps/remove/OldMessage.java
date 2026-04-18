package bm.gps.remove;

public record OldMessage(
    double latitude,
    double longitude,
    long timestamp,
    double speed,
    double bearing,
    double altitude,
    int battery,
    double hdop) {

  public static OldMessage now(
      double lat, double lon, double speed, double bearing, double alt, int batt, double hdop) {
    return new OldMessage(
        lat, lon, System.currentTimeMillis() / 1000L, speed, bearing, alt, batt, hdop);
  }
}
