package bm.traccar.ws;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Camel Route to connect to Traccar WebSocket API using the vertx-websocket component.
 * TraccarWsClientRoute is divided into three routes: <br>
 * 1. http to Traccar API to obtain a JSESSIONID cookie. <br>
 * 2. Connect to the Traccar WebSocket with JSESSIONID. <br>
 * 3. Processes incoming WebSocket messages. <br>
 */
@Component
public class TraccarWsClientRoute extends RouteBuilder {

  @Value("${traccar.user.password}")
  private String password;

  @Value("${traccar.user.email}")
  private String email;

  @Value("${traccar.host}")
  private String host;

  @Value("${traccar.websocket.url}")
  private String traccarWebSocketUrl;

  @Autowired protected ProducerTemplate producer;

  private final PositionProcessor positionProcessor;

  TraccarWsClientRoute(PositionProcessor positionProcessor) {
    this.positionProcessor = positionProcessor;
  }

  @Override
  public void configure() throws Exception {

    // TODO implement automatic restart after disconnect: traccar.websocket.refresh

    from("timer:traccarLoginTimer?period=3600000") // app.props
        .routeId("traccarLoginRoute")
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
        .setBody(simple("email=" + email + "&password=" + password)) // credentials
        .toD(host + "/api/session") // login
        .process(
            exchange -> {
              String jSessionId = null;
              String setCookieHeader = exchange.getIn().getHeader("Set-Cookie", String.class);
              log.debug("Set-Cookie: " + setCookieHeader);

              // Extract JSESSIONID from the Set-Cookie header
              if (setCookieHeader != null) {
                String[] cookies = setCookieHeader.split(";");
                for (String cookie : cookies) {
                  if (cookie.trim().startsWith("JSESSIONID=")) {
                    jSessionId = cookie.substring("JSESSIONID=".length()).trim();
                    break;
                  }
                }
              }

              if (jSessionId != null) {
                log.info("Successfully obtained JSESSIONID: {}", jSessionId);
                exchange.setProperty("JSESSIONID", jSessionId);
                // Trigger WebSocket connection route
                producer.sendBodyAndHeader(
                    "direct:connectTraccarWebSocket", "Connect", "JSESSIONID", jSessionId);
              } else {
                log.error("Failed to obtain JSESSIONID from Traccar login response.");
                // Handle login failure, e.g., retry or send alert
              }
            })
        .onException(HttpOperationFailedException.class)
        .handled(true)
        .log("Traccar login failed: ${exception.message}")
        .end();

    from("direct:connectTraccarWebSocket")
        .routeId("traccarWebSocketConnectionRoute")
        .process(
            exchange -> {
              String jSessionId = exchange.getIn().getHeader("JSESSIONID", String.class);
              log.info("jSessionId = " + jSessionId);

              if (jSessionId != null) {
                String cookieHeaderValue = "JSESSIONID=" + jSessionId;
                exchange.getIn().setHeader("Cookie", cookieHeaderValue);
                log.info(
                    "Attempting to connect to Traccar WebSocket with Cookie: {}",
                    cookieHeaderValue);
              } else {
                log.warn("No JSESSIONID available. Cannot establish Traccar WebSocket connection.");
                exchange.setException(
                    new IllegalStateException("No JSESSIONID to establish WebSocket."));
              }
            })
        .toD("vertx-websocket:" + traccarWebSocketUrl + "&handshake.Cookie=${header.Cookie}")
        .log("Connected to Traccar WebSocket.")
        .to("direct:traccarWebSocketMessages");

    // --- Step 3: Process incoming WebSocket messages ---
    from("direct:traccarWebSocketMessages")
        .routeId("traccarWebSocketMessageProcessingRoute")
        // .unmarshal()
        // .json(JsonLibrary.Jackson) // Unmarshal JSON to Map/POJO
        .log("Received Traccar WebSocket update: ${body}");

    // Here you can add your custom logic:
    //        .choice()
    //        .when(simple("${body.containsKey('positions')}"))
    //        .log("New Positions: ${body.positions}")
    //        .to("bean:positionProcessor") // Process position data
    //        .when(simple("${body.containsKey('devices')}"))
    //        .log("New Devices: ${body.devices}")
    //        .to("bean:deviceProcessor") // Process device data
    //        .when(simple("${body.containsKey('events')}"))
    //        .log("New Events: ${body.events}")
    //        .to("bean:eventProcessor") // Process event data
    //        .otherwise()
    //        .log("Unknown Traccar WebSocket message type: ${body}")
    //        .end()
    //        .endChoice();

    //    from("timer:traccarWebSocketPoller?period=60000") // every 60 seconds
    //        .routeId("traccarWebSocketPollingRoute")
    //        .to("direct:traccarWebSocketMessages");
  }
}
