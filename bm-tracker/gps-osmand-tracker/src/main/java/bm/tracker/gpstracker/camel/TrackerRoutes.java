package bm.tracker.gpstracker.camel;

import bm.tracker.gpstracker.model.GpsMessage;
import bm.tracker.gpstracker.queue.MessageQueue;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class TrackerRoutes extends RouteBuilder {
  // private static final Logger logger = LoggerFactory.getLogger(TrackerRoutes.class);

  // @Autowired OsmAndTracker client;
  // import test towards rt client
  // private Device device = null;

  private final MessageQueue queue;
  private String osmandHost;

  public TrackerRoutes(String osmandHost, MessageQueue queue) {
    this.osmandHost = osmandHost;
    this.queue = queue;
  }

  @Override
  public void configure() throws Exception {
    // On any exception while sending HTTP, enqueue the message for retry
    onException(Exception.class)
        .handled(true)
        .process(
            exchange -> {
              Object body = exchange.getIn().getBody();
              if (body instanceof GpsMessage) {
                queue.enqueue((GpsMessage) body);
              }
            });

    // Entry point for tracker messages
    from("direct:send")
        .routeId("send-route")
        .process(this::buildUrlHeader)
        .toD(osmandHost + "?bridgeEndpoint=true")
        .log("Sent message to {}: " + osmandHost);

    // Periodically flush queued messages (attempt one per second)
    from("timer:flushQueue?period=1000")
        .routeId("flush-route")
        .process(
            exchange -> {
              GpsMessage msg = queue.poll();
              if (msg != null) {
                exchange.getIn().setBody(msg);
                exchange.getIn().setHeader("targetUrl", msg.toString());
              } else {
                exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
              }
            })
        .choice()
        .when(body().isNotNull())
        .toD(osmandHost) // ${header.targetUrl}
        .log("Flushed queued message to {}: " + osmandHost)
        .end();
  }

  private void buildUrlHeader(org.apache.camel.Exchange exchange) {

    Object body = exchange.getIn().getBody();
    if (body instanceof GpsMessage) {
      GpsMessage msg = (GpsMessage) body;
      exchange.getIn().setHeader("CamelHttpQuery", msg.toString());
    }
    exchange.getIn().setBody(null); // Clear body for the GET request
  }
}
