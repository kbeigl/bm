package bm.traccar.ws;

import bm.traccar.generated.model.dto.Device;
import bm.traccar.rt.RealTimeManager;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Camel Route to connect to Traccar WebSocket API using the vertx-websocket component. The Route is
 * subdivided into three steps: <br>
 * 1. http to Traccar API to obtain a JSESSIONID cookie. <br>
 * 2. Connect to the Traccar WebSocket with JSESSIONID. <br>
 * 3. Processes incoming WebSocket messages. <br>
 */
@Component
public class TraccarWebSocketRoute extends RouteBuilder {
  private static final Logger logger = LoggerFactory.getLogger(TraccarWebSocketRoute.class);

  @Value("${traccar.host}")
  private String host;

  //  @Value("${traccar.websocket.url}")
  //  private String traccarWebSocketUrl;

  // no REST API involved !
  @Autowired private TraccarSessionManager sessionManager;
  @Autowired protected ProducerTemplate producer;
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final RealTimeManager stateManager = RealTimeManager.getInstance();

  // move outside (to ApiService) ?
  public void loginAndConnect(String email, String password) throws Exception {
    Exchange exchange = getContext().getEndpoint("direct:traccarLogin").createExchange();
    exchange.getIn().setHeader("email", email);
    exchange.getIn().setHeader("password", password);
    producer.send("direct:traccarLogin", exchange);
    // now the server is waiting for live events
    // Wait for the given number of seconds after WS connected
    // Thread.sleep(10 * 1000L); // wait in seconds
    // TODO add a timer (via CamelCtxt?) to keep the connection alive
  }

  // TODO auto restart/retry after disconnect: traccar.websocket.refresh

  @Override
  public void configure() throws Exception {

    // invalid JSON parsing
    onException(JsonParseException.class)
        .handled(true)
        .log(LoggingLevel.WARN, "Invalid JSON ignored: ${exception.message}");

    // internal Exception
    configureLoginAndConnectRoute();

    // CBR to process incoming WebSocket messages ---
    from("direct:start-websocket-connection")
        .routeId("traccarWebSocketMessageProcessingRoute")
        // set to LoggingLevel.DEBUG
        .log(LoggingLevel.DEBUG, "Process WebSocket raw  message: ${body}")
        .unmarshal() // convert body to ..
        .json() // .. LinkedHashMap Map<String, Object>
        // ===== JSON parsing
        .log("Process WebSocket json message: ${body}")
        .choice()
        // ===== empty message
        .when(simple("${body.isEmpty()}"))
        .log("Empty message ignored: ${body}")
        // ===== devices
        .when(simple("${body[devices]} != null"))
        // . (simple("${body.containsKey('devices')}"))
        .log("devices message received: ${body}")
        .setBody(simple("${body[devices]}"))
        .process(
            ex -> {
              Device[] devices = mapper.convertValue(ex.getIn().getBody(), Device[].class);
              for (Device device : devices) stateManager.addOrUpdateDevice(device);
              logger.info("{} Devices updated.", devices.length);
            });

    //  .end();
  }

  // maybe we'll split this into two routes later
  // with optional Login via Api to obtain JSESSIONID cookie
  private void configureLoginAndConnectRoute() {
    from("direct:traccarLogin")
        .routeId("traccarLoginRoute")
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
        .setBody(simple("email=${header.email}&password=${header.password}"))
        .toD(host + "/api/session")
        .process(
            // we also need to get the User returned for Traccar Model/Manager
            exchange -> {
              sessionManager.setJsessionidCookie(null);
              String setCookieHeader = exchange.getIn().getHeader("Set-Cookie", String.class);
              log.debug("Set-Cookie: " + setCookieHeader);

              if (setCookieHeader != null) {
                String[] cookies = setCookieHeader.split(";");
                for (String cookie : cookies) {
                  if (cookie.trim().startsWith("JSESSIONID=")) {
                    sessionManager.setJsessionidCookie(
                        cookie.substring("JSESSIONID=".length()).trim());
                    break;
                  }
                }
              }

              if (sessionManager.getJsessionidCookie() != null) {
                log.info("Obtained JSESSIONID: {}", sessionManager.getJsessionidCookie());
                String wsRouteId = "traccarWebSocketDynamicRoute";
                if (getContext().getRouteController().getRouteStatus(wsRouteId) != null) {
                  getContext().getRouteController().stopRoute(wsRouteId);
                  getContext().removeRoute(wsRouteId);
                }

                String wsUri = deriveWsFromUrl(host, sessionManager.getJsessionidCookie());
                log.info("Connecting to Traccar WebSocket at: {}", wsUri);

                getContext()
                    .addRoutes(
                        new RouteBuilder() {
                          @Override
                          public void configure() {
                            from("vertx-websocket:" + wsUri)
                                .routeId(wsRouteId)
                                .log(
                                    LoggingLevel.DEBUG,
                                    "Received Traccar WebSocket update: ${body}")
                                .to("direct:start-websocket-connection");
                          }
                        });
              } else {
                log.error("Failed to obtain JSESSIONID from Traccar login response.");
                // handle login failure, retry or send alert
              }
            })
        .onException(HttpOperationFailedException.class)
        .handled(true)
        .log("Traccar login failed: ${exception.message}")
        .end();
  }

  /** Construct the WebSocket URL from the base API URL */
  private String deriveWsFromUrl(String traccarUrl, String jSessionid) throws URISyntaxException {
    URI httpUri = new URI(traccarUrl);
    String traccarWsUrl =
        (httpUri.getScheme().equalsIgnoreCase("https") ? "wss://" : "ws://")
            + httpUri.getHost()
            + (httpUri.getPort() > 0 ? ":" + httpUri.getPort() : "")
            + "/api/socket?consumeAsClient=true"
            // : ("wss".equals(scheme) ? 443 : 80));
            + "&handshake.Cookie=JSESSIONID="
            + jSessionid;
    log.info("Constructed Traccar WebSocket URL: {}", traccarWsUrl);
    return traccarWsUrl;
  }
}
