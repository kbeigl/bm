package bm.traccar.ws.entities;

import java.util.ArrayList;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @Component("positionProcessor")
public class PositionProcessor implements Processor {
  private static final Logger logger = LoggerFactory.getLogger(PositionProcessor.class);

  @Override
  // or Bean method processPositions(Map<String, Object> positions)
  public void process(Exchange exchange) throws Exception {

    Object body = exchange.getIn().getBody();
    // validation
    if (!(body instanceof ArrayList)) return;
    // ArrayList of LinkedHashMap devices
    ArrayList<?> positions = (ArrayList<?>) body;
    if (positions.isEmpty()) return;

    logger.info("PositionProcessor: received {} Position message/s.", positions.size());

    //    if (body.containsKey("positions")) {
    //      List<Map<String, Object>> positions = (List<Map<String, Object>>) body.get("positions");
    //      for (Map<String, Object> position : positions) {
    //        Long deviceId = (Long) position.get("deviceId");
    //        Double latitude = (Double) position.get("latitude");
    //        Double longitude = (Double) position.get("longitude");
    //        // ... extract other position data
    //
    //        // log.info("Device ID: {}, Lat: {}, Lon: {}", deviceId, latitude, longitude);
    //        // Your logic here:
    //        // - Store in database
    //        // - Send to another service
    //        // - Update a real-time map
    //      }
    //    }
  }
}
