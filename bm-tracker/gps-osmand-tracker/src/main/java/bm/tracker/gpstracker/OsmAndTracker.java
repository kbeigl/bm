package bm.tracker.gpstracker;

import bm.tracker.gpstracker.model.GpsMessage;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

@Component
public class OsmAndTracker {
  // private static final Logger LOGGER = Logger.getLogger(OsmAndTracker.class.getName());
  private final ProducerTemplate device;

  public OsmAndTracker(ProducerTemplate template) {
    this.device = template;
  }

  // Public API to send a message via the Camel route
  public void send(GpsMessage msg) {
    device.asyncSendBody("direct:send", msg);
  }
}
