package bm.traccar;

import java.util.Map;

/**
 * Data format for Traccar Client API.
 *
 * <p>Generated based on TraccarClient.json in main/resources.
 *
 * <p>Currently this class is not used directly, but serves as documentation of the expected format.
 */
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
