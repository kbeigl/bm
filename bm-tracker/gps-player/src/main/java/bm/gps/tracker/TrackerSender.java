package bm.gps.tracker;

import bm.gps.MessageOsmand;
import java.io.IOException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;

/**
 * Sender is an alternative to Transmitter that uses a Camel SEDA endpoint as its ingress. Messages
 * are placed on the SEDA queue and this route forwards them to the configured osmandHost. The route
 * uses a Dead Letter Channel error handler with exponential backoff.
 */
public class TrackerSender extends RouteBuilder {
  // private static final Logger logger = LoggerFactory.getLogger(Sender.class);

  // uniqueId
  private final String osmandHost, safeId, sedaEndpoint, routeId;
  private final String directEndpoint, directRouteId;

  public TrackerSender(String osmandHost, String uniqueId) {
    this.osmandHost = osmandHost;
    this.safeId = uniqueId == null ? "" : uniqueId.replaceAll("[^A-Za-z0-9_-]", "_");
    // concurrent consumers on SEDA queue for higher throughput
    this.sedaEndpoint = "seda:send-osmand-" + safeId + "?concurrentConsumers=4";
    this.directEndpoint = "direct:send-osmand-" + safeId;
    this.routeId = "seda-send-osmand-route-" + safeId;
    this.directRouteId = "direct-send-osmand-route-" + safeId;
  }

  @Override
  public void configure() throws Exception {
    // activate DLQ ? for unrecoverable errors
    // errorHandler( deadLetterChannel("log:dead?level=ERROR")...

    // TODO: use parameters for  Error Handling Policy (app.props)
    // catch connection issues and HTTP errors (like 503 Service Unavailable)
    // Narrow exception handling to IO and HTTP operation failures instead of all Exceptions
    // Some RouteBuilder variants require a single exception class per onException call,
    // so register two handlers with the same retry policy.
    onException(IOException.class)
        .maximumRedeliveries(5) // Try 5 times
        .redeliveryDelay(2000) // Wait 2 seconds between tries
        .useExponentialBackOff()
        .backOffMultiplier(2) // Double the wait time each failure (2s, 4s, 8s...)
        .retryAttemptedLogLevel(LoggingLevel.WARN)
        .handled(true) // Don't crash the whole route
        .log("Failed to reach server after retries. Giving up on ${body}");

    onException(HttpOperationFailedException.class)
        .maximumRedeliveries(5)
        .redeliveryDelay(2000)
        .useExponentialBackOff()
        .backOffMultiplier(2)
        .retryAttemptedLogLevel(LoggingLevel.WARN)
        .handled(true)
        .log("Failed to reach server after retries. Giving up on ${body}");

    // forward to SEDA queue for asynchronous processing, returns *instantly*!
    from(directEndpoint)
        .routeId(directRouteId)
        .log(
            LoggingLevel.DEBUG,
            "Received message on {} -> forwarding to {}",
            directEndpoint,
            sedaEndpoint)
        .to(sedaEndpoint);

    // Consume messages from the SEDA queue and forward them to the OsmAnd HTTP endpoint
    from(sedaEndpoint)
        .routeId(routeId)
        .process(this::buildUrlHeader)
        .toD(osmandHost + "?bridgeEndpoint=true&throwExceptionOnFailure=true")
        .log(LoggingLevel.DEBUG, "Sender: sent message to " + osmandHost + " via " + routeId);
  }

  /** Build headers required by the OsmAnd HTTP GET endpoint. Mirrors Transmitter.buildUrlHeader. */
  private void buildUrlHeader(Exchange exchange) {
    Object body = exchange.getIn().getBody();
    if (body instanceof MessageOsmand) {
      MessageOsmand msg = (MessageOsmand) body;
      // Set CamelHttpQuery so HTTP GET encodes the message as query parameters
      exchange.getIn().setHeader("CamelHttpQuery", msg.toString());
      // keep original message on the exchange for potential error handling
      exchange.setProperty("OriginalMessage", msg);
    }
    // Clear body for the GET request
    exchange.getIn().setBody(null);
  }
}
