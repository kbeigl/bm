package bm.gps.tracker;

import bm.gps.MessageOsmand;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transmitter component for Tracker. Implemented with Camel routes for sending messages to OsmAnd
 * server.
 *
 * <p>Messages can be sent "fire and forget" style. If sending fails, messages are queued for retry.
 * This way the Tracker continues to operate regardless of problems with network etc.
 */
public class Transmitter extends RouteBuilder {
  private static final Logger logger = LoggerFactory.getLogger(Transmitter.class);

  // @Autowired OsmAndTracker client; for back referencs ?

  /**
   * Fire and forget sending of messages to OsmAnd server.
   *
   * <p>Message queue to hold messages that are considered to be sent - in application semantics. If
   * messages can not be sent technically (OTA problems etc.) they are pushed into the queue. Camel
   * will take care of reconnecting, exponentiell backup strategy etc.
   */
  // currently MQ is private and not exposed
  private final MessageQueue queue = new MessageQueue();

  private String osmandHost;

  public Transmitter(String osmandHost) {
    this.osmandHost = osmandHost;
  }

  @Override
  public void configure() throws Exception {
    // On any exception while sending HTTP, enqueue the message for retry
    onException(Exception.class)
        .handled(true)
        .process(
            exchange -> {
              Object body = exchange.getIn().getBody();
              logger.error(
                  "TrackerRoutes.onException: Exception occurred: {}. Enqueuing message.",
                  exchange
                      .getProperty(org.apache.camel.Exchange.EXCEPTION_CAUGHT, Exception.class)
                      .getMessage());
              MessageOsmand msg = null;
              if (body instanceof MessageOsmand) {
                msg = (MessageOsmand) body;
              } else {
                // check Exchange property where we now store the original object
                Object prop = exchange.getProperty("OriginalMessage");
                if (prop instanceof MessageOsmand) {
                  msg = (MessageOsmand) prop;
                }
              }
              if (msg != null) {
                queue.enqueue(msg);
              }
            })
        .log("TrackerRoutes.onException: enqueued message due to exception: ${exception.message}");

    // Entry point for tracker messages
    from("direct:send-osmand")
        .routeId("send-osmand-route")
        .process(this::buildUrlHeader)
        .toD(osmandHost + "?bridgeEndpoint=true&throwExceptionOnFailure=true")
        .process(
            exchange -> {
              // Treat non-2xx responses as errors so the onException handler enqueues the message
              Object codeObj =
                  exchange.getMessage().getHeader(org.apache.camel.Exchange.HTTP_RESPONSE_CODE);
              int code = -1;
              if (codeObj instanceof Integer) {
                code = (Integer) codeObj;
              } else if (codeObj != null) {
                try {
                  code = Integer.parseInt(codeObj.toString());
                } catch (NumberFormatException ignored) {
                }
              }
              if (code >= 400) {
                throw new RuntimeException("HTTP error response: " + code);
              }
            })
        .log("Sent message to {}: " + osmandHost);

    // Periodically flush queued messages (attempt one per second)
    //    from("timer:flushQueue?period=1000")
    //        .routeId("flush-route")
    //        .process(
    //            exchange -> {
    //              MessageOsmand msg = queue.poll();
    //              if (msg != null) {
    //                exchange.getIn().setBody(msg);
    //                exchange.getIn().setHeader("targetUrl", msg.toString());
    //              } else {
    //                exchange.setProperty(Exchange.ROUTE_STOP, Boolean.TRUE);
    //              }
    //            })
    //        .choice()
    //        .when(body().isNotNull())
    //        .toD(osmandHost + "?throwExceptionOnFailure=true") // ${header.targetUrl}
    //        .process(
    //            exchange -> {
    //              Object codeObj =
    //
    // exchange.getMessage().getHeader(org.apache.camel.Exchange.HTTP_RESPONSE_CODE);
    //              int code = -1;
    //              if (codeObj instanceof Integer) {
    //                code = (Integer) codeObj;
    //              } else if (codeObj != null) {
    //                try {
    //                  code = Integer.parseInt(codeObj.toString());
    //                } catch (NumberFormatException ignored) {
    //                }
    //              }
    //              if (code >= 400) {
    //                throw new RuntimeException(
    //                    "HTTP error response while flushing queued message: " + code);
    //              }
    //            })
    //        .log("Flushed queued message to {}: " + osmandHost)
    //        .end();
  }

  private void buildUrlHeader(org.apache.camel.Exchange exchange) {

    Object body = exchange.getIn().getBody();
    if (body instanceof MessageOsmand) {
      MessageOsmand msg = (MessageOsmand) body;
      exchange.getIn().setHeader("CamelHttpQuery", msg.toString());
      // keep original message on the Exchange properties
      // so it is not coerced to String by the HTTP component
      exchange.setProperty("OriginalMessage", msg);
    }
    exchange.getIn().setBody(null); // Clear body for the GET request
  }
}
