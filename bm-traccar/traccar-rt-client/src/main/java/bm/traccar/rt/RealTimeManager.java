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
 * ConcurrentHashMaps to allow safe updates from background webSocket threads.
 */
public final class RealTimeManager {

  private static final RealTimeManager INSTANCE = new RealTimeManager();

  private User currentUser;
  private volatile boolean isAuthenticated = false;

  private RealTimeManager() {}

  public static RealTimeManager getInstance() {
    return INSTANCE;
  }

  private final Map<Long, User> users = new ConcurrentHashMap<>();
  private final Map<Long, Device> devices = new ConcurrentHashMap<>();
  private final Map<Long, Position> positions = new ConcurrentHashMap<>();

  // --- State Modification Methods ---

  /** Sets the current user after a successful login. */
  public void loginUser(User user) {
    this.currentUser = user;
    this.isAuthenticated = true;
  }

  /** Clears all user data on logout. */
  public void logoutAndClear() {
    this.currentUser = null;
    this.isAuthenticated = false;
    this.users.clear();
    this.devices.clear();
    this.positions.clear();
  }

  /**
   * Generic method to load initial entities into a ConcurrentHashMap by their ID.
   *
   * @param entityList List of entities to load
   * @param map The map to populate
   * @param idExtractor Function to extract the ID from the entity
   * @param <T> Entity type
   * @param <K> ID type
   */
  private <T, K> void loadInitialEntities(
      List<T> entityList, Map<K, T> map, java.util.function.Function<T, K> idExtractor) {
    map.clear();
    Map<K, T> entityMap =
        entityList.stream().collect(Collectors.toMap(idExtractor, Function.identity()));
    map.putAll(entityMap);
  }

  /** Loads the initial list of users from server. */
  public void loadInitialUsers(List<User> userList) {
    loadInitialEntities(userList, users, User::getId);
  }

  /** Loads the initial list of devices from server. */
  public void loadInitialDevices(List<Device> deviceList) {
    loadInitialEntities(deviceList, devices, Device::getId);
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
    System.out.println("RTM addOrUpdateDevice: " + device);
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

  public Optional<User> getUserById(long id) {
    return Optional.ofNullable(users.get(id));
  }

  public Optional<Device> getDeviceById(long id) {
    return Optional.ofNullable(devices.get(id));
  }

  public Optional<Position> getPositionById(long id) {
    return Optional.ofNullable(positions.get(id));
  }

  /** Returns a copy of the list of all users. */
  public List<User> getAllUsers() {
    return new ArrayList<>(users.values());
  }

  /** Returns a copy of the list of all devices. */
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
