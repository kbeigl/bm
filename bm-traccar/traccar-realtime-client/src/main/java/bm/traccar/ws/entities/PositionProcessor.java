/*
 * (C) Copyright 2026 Kristof Beiglböck
 *               kbeigl.github.io/bm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bm.traccar.ws.entities;

import bm.traccar.generated.model.dto.Position;
import bm.traccar.rt.RealTimeController;
import java.util.ArrayList;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PositionProcessor implements Processor {
  private static final Logger logger = LoggerFactory.getLogger(PositionProcessor.class);
  private final RealTimeController controller;

  public PositionProcessor(RealTimeController controller) {
    this.controller = controller;
  }

  @Override
  // or Bean method processPositions(Map<String, Object> positions)
  public void process(Exchange exchange) throws Exception {

    Object body = exchange.getIn().getBody();
    // validation
    if (!(body instanceof ArrayList)) return;
    // ArrayList of LinkedHashMap devices
    ArrayList<?> positions = (ArrayList<?>) body;
    if (positions.isEmpty()) return;

    logger.debug("received {} position motion message/s.", positions.size());

    // convert and update all positions
    for (Object item : positions) {
      try {
        Position pos = EntityMapper.get().convertValue(item, Position.class);
        if (pos != null) {
          controller.addOrUpdatePosition(pos);
          logger.info("updated position (id={},deviceId={})", pos.getId(), pos.getDeviceId());
        }
      } catch (IllegalArgumentException ex) {
        logger.warn("Failed to convert Position: {}", ex.getMessage());
      }
    }

    // log.info("Device ID: {}, Lat: {}, Lon: {}", deviceId, latitude, longitude);
    // Your logic here:
    // - Store in database
    // - Send to another service
    // - Update a real-time map
  }
}
