package bm.traccar.ws;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component("positionProcessor")
public class PositionProcessor {

  public void processPositions(Map<String, Object> body) {
    System.out.println("Processing positions...");

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
