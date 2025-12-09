package bm.tracker.gpstracker;

import bm.tracker.gpstracker.camel.TrackerRoutes;
import bm.tracker.gpstracker.model.GpsMessage;
import bm.tracker.gpstracker.queue.MessageQueue;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

/**
 * OsmAndTracker is a client component that sends GPS messages to the OsmAnd server. Note that this
 * Tracker does not have a unique devicId. For the time being, the id is taken from the GpsMessage
 * and a single Tracker instance can be used to send messages.
 *
 * <p>The actual device should be modeled after the OsmAnd protocol .. Device Entity.
 *
 * <p>On a long term, high performance solution, each device should have its own Tracker instance
 * with a unique id and its own Camel route to avoid message mix-up.
 */
@Component
public class OsmAndTracker {

  private String deviceId;
  private CamelContext camel;
  private ProducerTemplate tracker;
  private final MessageQueue queue = new MessageQueue();

  public OsmAndTracker() {}

  public OsmAndTracker(
      String deviceId, String osmandHost, CamelContext camelContext, ProducerTemplate producer) {
    this.deviceId = deviceId;
    this.camel = camelContext;
    this.tracker = producer;

    // add deviceID to route/name ?
    TrackerRoutes routes = new TrackerRoutes(osmandHost, queue);
    try {
      this.camel.addRoutes(routes);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Public API to send a message via the Camel route
  public void send(GpsMessage msg) {

    // validate message deviceId matches tracker deviceId

    // Asynchronous sending can lead to overlaps of background processes
    // as can be seen in tests: @AfterEach teardown starts while messages are still processed.
    // tracker.asyncSendBody("direct:send", msg);
    tracker.sendBody("direct:send", msg);
  }

  // SEE SimulatorService.java FOR MORE ADVANCED TRACKER FEATURES:
  // add distance threshold
  // add angle threshold
  // add periodic sending
}
