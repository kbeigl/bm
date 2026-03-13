package bm.traccar.ws.entities;

import bm.traccar.generated.model.dto.Device;
import bm.traccar.rt.RealTimeManager;
import java.util.ArrayList;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
  private final RealTimeManager stateManager;

  @Autowired
  public DeviceProcessor(RealTimeManager stateManager) {
    this.stateManager = stateManager;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    /*
     * TODO This should apply for all messages!
     * The body has been validated in the route
     * and is most probably a Collection of a device map.
     */
    Object body = exchange.getIn().getBody();
    // validation
    if (!(body instanceof ArrayList)) return;
    // ArrayList of LinkedHashMap devices
    ArrayList<?> devices = (ArrayList<?>) body;
    if (devices.isEmpty()) return;

    logger.info("DeviceProcessor: received {} device message/s.", devices.size());

    // convert and update all devices
    for (Object item : devices) {
      try {
        Device d = EntityMapper.get().convertValue(item, Device.class);
        if (d != null) {

          // ???? USE CONTROLLER TO UPDATE STATE, NOT DIRECTLY IN RTM ????
          stateManager.addOrUpdateDevice(d);

          logger.info("Updated device with id={} / uniqueId={}", d.getId(), d.getUniqueId());
        }
      } catch (IllegalArgumentException ex) {
        logger.warn("Failed to convert device entry to Device: {}", ex.getMessage());
      }
    }
  }
}
