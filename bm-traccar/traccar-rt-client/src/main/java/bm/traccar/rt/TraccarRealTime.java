package bm.traccar.rt;

import bm.traccar.generated.model.dto.Device;
import bm.traccar.generated.model.dto.Position;
import bm.traccar.generated.model.dto.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A thread-safe Singleton class to manage the real-time state of the Traccar client. It uses
 * ConcurrentHashMaps to allow safe updates from background threads (e.g., WebSocket listeners).
 */
public final class TraccarRealTime {

  private static final TraccarRealTime INSTANCE = new TraccarRealTime();

  // Data is stored in maps for fast O(1) lookups by ID.
  // ConcurrentHashMap is used for thread safety.
  private final Map<Long, Device> devices = new ConcurrentHashMap<>();
  private final Map<Long, Position> positions = new ConcurrentHashMap<>();
  // You would add more maps here for Geofences, Events, etc.

  private User currentUser;
  private volatile boolean isAuthenticated = false;

  private TraccarRealTime() {
    // Private constructor to prevent instantiation.
  }

  public static TraccarRealTime getInstance() {
    return INSTANCE;
  }

  // --- State Modification Methods ---

  /** Sets the current user after a successful login. */
  public void loginUser(User user) {
    this.currentUser = user;
    this.isAuthenticated = true;
  }

  /** Clears all user data on logout. */
  public void logout() {
    this.currentUser = null;
    this.isAuthenticated = false;
    this.devices.clear();
    this.positions.clear();
  }

  /** Loads the initial list of devices from the REST API. */
  public void loadInitialDevices(List<Device> deviceList) {
    devices.clear();
    // Convert the list to a map keyed by device ID
    Map<Long, Device> deviceMap =
        deviceList.stream().collect(Collectors.toMap(Device::getId, Function.identity()));
    devices.putAll(deviceMap);
  }

  /**
   * Adds or updates a position from a WebSocket message. Crucially, it also updates the associated
   * device's state.
   */
  public void addOrUpdatePosition(Position position) {
    if (position == null) return;

    // 1. Add/update the position in the positions map.
    positions.put(position.getId(), position);

    // 2. Find the associated device and update its references.
    Device device = devices.get(position.getDeviceId());
    if (device != null) {
      device.setPositionId(position.getId());
      device.setLastUpdate(position.getServerTime());
      // You might also update the device status based on the position data
      // e.g., if position.getAttributes().get("ignition") is true/false.
      device.setStatus("online");
    }
  }

  /** Adds or updates a device from a WebSocket message. */
  public void addOrUpdateDevice(Device device) {
    if (device == null) return;
    devices.put(device.getId(), device);
  }

  // --- Data Retrieval Methods ---

  public Optional<User> getCurrentUser() {
    return Optional.ofNullable(currentUser);
  }

  public boolean isAuthenticated() {
    return isAuthenticated;
  }

  public Optional<Device> getDeviceById(long id) {
    return Optional.ofNullable(devices.get(id));
  }

  public Optional<Position> getPositionById(long id) {
    return Optional.ofNullable(positions.get(id));
  }

  /**
   * Returns a copy of the list of all devices.
   *
   * @return A new List containing all current devices.
   */
  public List<Device> getAllDevices() {
    return new ArrayList<>(devices.values());
  }

  /**
   * A powerful helper method to get the latest position for a given device. This demonstrates how
   * relationships are resolved.
   *
   * @param deviceId The ID of the device.
   * @return An Optional containing the latest Position, or empty if not found.
   */
  public Optional<Position> getLatestPositionForDevice(long deviceId) {
    return getDeviceById(deviceId)
        .map(Device::getPositionId) // Get the positionId from the device
        .flatMap(this::getPositionById); // Use that ID to get the Position object
  }
}
