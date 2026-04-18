package bm.traccar.ws.entities;

import bm.traccar.generated.model.dto.Device;
import bm.traccar.rt.RealTimeController;
import java.util.ArrayList;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/* create abstract base class for all processors that handle entity messages,
 * centralize common logic, like mapping, to add more processing steps in the future,
 * e.g. ETL, validation, transformation, etc. */

/**
 * Processor that handles device messages from Traccar WebSocket. Expects incoming message body to
 * be a Collection or array of device maps or a single device map.
 */
@Component
public class DeviceProcessor implements Processor {
  private static final Logger logger = LoggerFactory.getLogger(DeviceProcessor.class);

  /**
   * Processors should not directly manipulate the state manager. Instead, it should delegate to the
   * controller, which can then decide how to handle the update (e.g., update the state manager,
   * trigger events, etc.). This keeps the architecture clean and maintainable.
   *
   * <p>The controller is the single source of truth for all state updates, ensuring that all
   * changes go through a consistent pathway, allowing for better control and potential future
   * enhancements (e.g., event triggering, validation, etc.).
   *
   * <p>The intended layering is that the controller mediates all state transitions.
   */
  private final RealTimeController controller;

  public DeviceProcessor(RealTimeController controller) {
    this.controller = controller;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    /*
     * TODO This should apply for all messages!
     * The body has been validated in the route
     * and is most probably a Collection of a device map.
     * Redundant code in other processors should be removed
     * logic should be centralized in a base class or utility method.
     */
    Object body = exchange.getIn().getBody();
    // validation
    if (!(body instanceof ArrayList)) return;
    // ArrayList of LinkedHashMap devices
    ArrayList<?> devices = (ArrayList<?>) body;
    if (devices.isEmpty()) return;

    logger.info("received {} device event message/s.", devices.size());

    // convert and update all devices
    for (Object item : devices) {
      try {
        Device dev = EntityMapper.get().convertValue(item, Device.class);
        if (dev != null) {
          controller.addOrUpdateDevice(dev);
          logger.info("updated device (uniqueId={},id={})", dev.getUniqueId(), dev.getId());
        }
      } catch (IllegalArgumentException ex) {
        logger.warn("Failed to convert Device: {}", ex.getMessage());
      }
    }
  }
}
