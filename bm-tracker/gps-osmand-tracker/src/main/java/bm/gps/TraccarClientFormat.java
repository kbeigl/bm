package bm.gps;

import java.util.Map;

public class TraccarClientFormat {
  public static class Location {
    public String timestamp;
    public Coords coords;
    public boolean is_moving;
    public double odometer;
    public String event;
    public Battery battery;
    public Activity activity;
    public Map<String, Object> extras;
  }

  public static class Coords {
    public double latitude;
    public double longitude;
    public double accuracy;
    public double speed;
    public double heading;
    public double altitude;
  }

  public static class Battery {
    public double level;
    public boolean is_charging;
  }

  public static class Activity {
    public String type;
  }

  public Location location;
  public String device_id;
}
