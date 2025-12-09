package bm.tracker.gpstracker.model;

public class GpsMessage {
  private String id;
  private double lat;
  private double lon;
  private long timestamp; // seconds or millis as set by user
  private Double speed;
  private Double bearing;
  private Double altitude;
  private Double batt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLon() {
    return lon;
  }

  public void setLon(double lon) {
    this.lon = lon;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
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

  public Double getAltitude() {
    return altitude;
  }

  public void setAltitude(Double altitude) {
    this.altitude = altitude;
  }

  public Double getBatt() {
    return batt;
  }

  public void setBatt(Double batt) {
    this.batt = batt;
  }

  /** provides a string representation of the GpsMessage to append to the HTTP GET request */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("id=")
        .append(id)
        .append("&lat=")
        .append(lat)
        .append("&lon=")
        .append(lon)
        .append("&timestamp=")
        .append(timestamp);
    if (speed != null) {
      sb.append("&speed=").append(speed);
    }
    if (bearing != null) {
      sb.append("&bearing=").append(bearing);
    }
    if (altitude != null) {
      sb.append("&altitude=").append(altitude);
    }
    if (batt != null) {
      sb.append("&batt=").append(batt);
    }
    return sb.toString();
  }
}
