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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The RealTimeManager synchronizes the 'RealTimeModel' of the included Traccar entities.
 *
 * <p>A thread-safe Singleton class to manage the real-time state of the client. It uses
 * ConcurrentHashMaps to allow safe updates from background webSocket threads.
 */
@Component
public class RealTimeManager {
  private static final Logger logger = LoggerFactory.getLogger(RealTimeManager.class);

  private volatile User currentUser;
  private volatile boolean isAuthenticated = false;

  private final Map<Long, User> users = new ConcurrentHashMap<>();
  private final Map<Long, Device> devices = new ConcurrentHashMap<>();
  private final Map<Long, Position> positions = new ConcurrentHashMap<>();
  // map for O(1) lookup from device.uniqueId -> deviceId = database Id
  private final Map<String, Long> deviceIdByUniqueId = new ConcurrentHashMap<>();
  // lock to keep devices and deviceIdByUniqueId consistent
  private final Object deviceMapLock = new Object();

  // visibility (exclusiv to controller?) of RTM and methods
  // will be assigned with the application usage in mind.
  // For now, we can keep it package-private and make public if needed.

  // --- State Modification Methods ---

  /** Sets the current user after a successful login. */
  public void setLoginUser(User user) {
    this.currentUser = user;
    this.isAuthenticated = true;
  }

  /** Clears all user data on logout. */
  public void logoutAndClear() {
    this.currentUser = null;
    this.isAuthenticated = false;
    this.users.clear();
    // synchronize clearing of related device maps to avoid races with updates
    synchronized (deviceMapLock) {
      this.devices.clear();
      this.positions.clear();
      this.deviceIdByUniqueId.clear();
    }
  }

  /** Generic method to load initial entities into a ConcurrentHashMap by their ID. */
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
    // populate devices map and deviceIdByUniqueId atomically
    synchronized (deviceMapLock) {
      loadInitialEntities(deviceList, devices, Device::getId);
      deviceIdByUniqueId.clear();
      deviceList.stream()
          .filter(d -> d.getUniqueId() != null)
          .forEach(d -> deviceIdByUniqueId.put(d.getUniqueId(), d.getId()));
    }
  }

  // DO NOT load initial list of positions from server!
  // a scenario will have a start time (genesis) and end (doomsday, -time)
  // and we only want positions after that time

  /** Adds or updates a position and - crucially - also updates associated device's state. */
  public void addOrUpdatePosition(Position position) {
    if (position == null) return;

    // Perform mutations under deviceMapLock so position insertion and device maps remain consistent
    synchronized (deviceMapLock) {
      // 1. Add/update the position in the positions map.
      positions.put(position.getId(), position);

      // 2. Find the associated device and update its references.
      Device device = devices.get(position.getDeviceId());
      if (device != null) {
        device.setPositionId(position.getId());
        device.setLastUpdate(position.getServerTime()); // .. make sense ?
        // update device stati based on position data
        // e.g., if position.getAttributes().get("ignition") is true/false.
        // raise events ...
        device.setStatus("online");
      }
    }
  }

  /** Adds or updates a device (from a WebSocket message). */
  public void addOrUpdateDevice(Device device) {
    if (device == null) return;
    logger.info(
        "RTM.addOrUpdateDevice: update device with id={} / uniqueId={}",
        device.getId(),
        device.getUniqueId());

    // synchronize updates so both maps remain consistent
    synchronized (deviceMapLock) {
      // store/replace device explicitly
      Device previous = devices.get(device.getId());
      devices.put(device.getId(), device);

      // If uniqueId changed remove old mapping
      if (previous != null) {
        String prevUnique = previous.getUniqueId();
        String newUnique = device.getUniqueId();
        if (prevUnique != null && !prevUnique.equals(newUnique)) {
          deviceIdByUniqueId.remove(prevUnique);
        }
      }

      // ensure mapping from uniqueId -> id
      if (device.getUniqueId() != null) {
        deviceIdByUniqueId.put(device.getUniqueId(), device.getId());
      }
    }
  }

  // --- Data Retrieval Methods ---

  /**
   * Mapping from uniqueId to deviceId
   *
   * <p>The uniqueId identifies a unique device. Every hardware equipped with a SIM card has IMEI or
   * similar unique identifier, which is used as the uniqueId in Traccar. This uniqueId is what we
   * use to identify devices in our application and is the primary key for device-related
   * operations. The deviceId, on the other hand, is an internal database ID that Traccar uses to
   * manage relationships between entities.
   *
   * <p>Mapping from uniqueId to deviceId is a common pattern in systems where the external
   * identifier (uniqueId) differs from the internal database identifier (deviceId). This Map is a
   * crucial for maintaining the integrity of the data model state and ensuring that all updates are
   * correctly associated with the right device.
   *
   * <p>If the application consequentially uses uniqueIds - which can be semantic to its context it
   * can easily change the server and database as required by a scenario. This actually happens with
   * every Integration Test ...
   *
   * @param uniqueId
   * @return database deviceId
   */
  public long lookupDeviceIdByUniqueId(String uniqueId) {
    if (uniqueId == null) return -1L;
    return deviceIdByUniqueId.getOrDefault(uniqueId, -1L);
  }

  public Optional<User> getCurrentUser() {
    return Optional.ofNullable(currentUser);
  }

  public boolean isAuthenticated() {
    return isAuthenticated;
  }

  // TODO make generic to reduce method bloat
  public Optional<User> getUserById(long id) {
    return Optional.ofNullable(users.get(id));
  }

  public Optional<Device> getDeviceById(long id) {
    return Optional.ofNullable(devices.get(id));
  }

  public Optional<Position> getPositionById(long id) {
    return Optional.ofNullable(positions.get(id));
  }

  // make generic to reduce method bloat
  /** Returns a copy of the list of all users. */
  public List<User> getAllUsers() {
    return new ArrayList<>(users.values());
  }

  /** Returns a copy of the list of all devices. */
  public List<Device> getAllDevices() {
    return new ArrayList<>(devices.values());
  }

  /* demo resolving relationships */
  public Optional<Position> getLatestPositionForDevice(long deviceId) {
    return getDeviceById(deviceId)
        // Get the positionId from device
        .map(Device::getPositionId)
        // Use ID to get Position object
        .flatMap(this::getPositionById);
  }
}
