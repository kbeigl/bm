package bm.traccar.ws;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Camel Route to connect to Traccar WebSocket API using the vertx-websocket component.
 * TraccarWsClientRoute is divided into these routes: <br>
 * 1. Connect to the Traccar WebSocket with JSESSIONID. <br>
 * 2. Processes incoming WebSocket messages. <br>
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

    // Process incoming WebSocket messages ---
    from("direct:traccarWebSocketMessages")
        .routeId("traccarWebSocketMessageProcessingRoute")
        .log("Received Traccar WebSocket message: ${body}")
        .choice() // ----------
        .when(body().isNotNull())
        .unmarshal()
        .json(JsonLibrary.Jackson) // Unmarshal JSON to Map/POJO
        .log("Received Traccar WebSocket update: ${body}")
        .to("bean:positionProcessor") // Process positions
        .otherwise() // -------
        .log("Received null or empty WebSocket message, skipping processing.")
        .end();

    // Here you can add your custom logic:
    // .choice()
    // .when(simple("${body.containsKey('positions')}"))
    // 		.log("New Positions: ${body.positions}")
    // 		.to("bean:positionProcessor") // Process position data
    // .when(simple("${body.containsKey('devices')}"))
    //      .log("New Devices: ${body.devices}")
    //      .to("bean:deviceProcessor") // Process device data
    // .when(simple("${body.containsKey('events')}"))
    //      .log("New Events: ${body.events}")
    //      .to("bean:eventProcessor") // Process event data
    // .otherwise()
    //      .log("Unknown Traccar WebSocket message type: ${body}")
    //      .end()
    // .endChoice();

    // required ?
    // from("timer:traccarWebSocketPoller?period=60000") // every 60 seconds
    // .routeId("traccarWebSocketPollingRoute")
    // .to("direct:traccarWebSocketMessages");
  }
}
