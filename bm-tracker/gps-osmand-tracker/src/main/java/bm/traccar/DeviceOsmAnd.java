package bm.traccar;

import bm.gps.MessageOsmand;
import bm.gps.tracker.TrackerOsmAnd;
import bm.gps.tracker.TrackerRegistration;
import bm.traccar.generated.model.dto.Device;
import org.springframework.beans.factory.annotation.Autowired;

/** Holder to pair a Traccar Device DTO. */
@Deprecated
public class DeviceOsmAnd {

  private Device device;
  private TrackerOsmAnd tracker;
  @Autowired TrackerRegistration registration;

  public DeviceOsmAnd() {}

  /*  device implies uniqueId for registration of tracker */
  public DeviceOsmAnd(Device device) {
    this.device = device;
    try {
      // this.tracker = registration.registerTracker(device);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Device getDevice() {
    return device;
  }

  public DeviceOsmAnd setDevice(Device device) {
    this.device = device;
    return this;
  }

  public TrackerOsmAnd getTracker() {
    return tracker;
  }

  public DeviceOsmAnd setTracker(TrackerOsmAnd tracker) {
    this.tracker = tracker;
    return this;
  }

  /** Send an OsmandMessage using the underlying tracker. */
  public void send(MessageOsmand msg) {
    if (tracker == null) {
      throw new IllegalStateException("Tracker is not set on Device");
    }
    tracker.send(msg);
  }

  // delegated convenience getters for commonly used Device fields
  public Long getId() {
    return device == null ? null : device.getId();
  }

  public String getName() {
    return device == null ? null : device.getName();
  }

  public String getUniqueId() {
    return device == null ? null : device.getUniqueId();
  }

  // delegated convenience setters for commonly used Device fields
  public DeviceOsmAnd setId(Long id) {
    if (this.device == null) {
      this.device = new Device();
    }
    this.device.setId(id);
    return this;
  }

  public DeviceOsmAnd setName(String name) {
    if (this.device == null) {
      this.device = new Device();
    }
    this.device.setName(name);
    return this;
  }

  public DeviceOsmAnd setUniqueId(String uniqueId) {
    if (this.device == null) {
      this.device = new Device();
    }
    this.device.setUniqueId(uniqueId);
    return this;
  }
}
