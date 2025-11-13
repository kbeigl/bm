package bm.tracker.gpstracker.camel;

import bm.tracker.gpstracker.model.GpsMessage;
import bm.tracker.gpstracker.queue.MessageQueue;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CamelRoutes extends RouteBuilder {
  private static final Logger logger = LoggerFactory.getLogger(CamelRoutes.class);

  private final MessageQueue queue;

  @Value("${osmand.host}")
  private String osmandHost;

  public CamelRoutes(MessageQueue queue) {
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
        // .toD("http://localhost:5055?id=10&lat=52.525000&lon=13.405000&timestamp=1763996050&speed=15.00&bearing=90.00&altitude=50.00&batt=100") // ${header.targetUrl}
        // .toD("http://localhost:5055${header.targetUrl}")
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
                exchange.getIn().setHeader("targetUrl", buildUrl(msg));
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
      // exchange.getIn().setHeader(osmandHost, buildUrl(msg));
      exchange.getIn().setHeader("CamelHttpQuery", buildUrl(msg));
    }
    exchange.getIn().setBody(null); // Clear body for the GET request
  }

  private String buildUrl(GpsMessage msg) {
    long ts = msg.getTimestamp();
    StringBuilder sb = new StringBuilder();
    // sb.append(osmandHost)
    sb.append("id=") // leading slash ?
        .append(msg.getId())
        .append("&lat=")
        .append(msg.getLat())
        .append("&lon=")
        .append(msg.getLon())
        .append("&timestamp=")
        .append(ts);
    if (msg.getSpeed() != null) {
      sb.append("&speed=").append(msg.getSpeed());
    }
    if (msg.getBearing() != null) {
      sb.append("&bearing=").append(msg.getBearing());
    }
    return sb.toString();
  }
}
