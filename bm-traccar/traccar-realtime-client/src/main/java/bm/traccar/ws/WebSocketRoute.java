package bm.traccar.ws;

import bm.traccar.ws.entities.DeviceProcessor;
import bm.traccar.ws.entities.PositionProcessor;
import com.fasterxml.jackson.core.JsonParseException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
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
public class WebSocketRoute extends RouteBuilder {
  // private static final Logger logger = LoggerFactory.getLogger(WebSocketRoute.class);
  // using RouteBuilder log

  @Value("${traccar.host}")
  private String host;

  // no REST API involved !
  @Autowired private SessionManager sessionManager;
  @Autowired protected ProducerTemplate producer;

  // move (to ApiService) ?
  public void loginAndConnect(String email, String password) throws Exception {
    Exchange ex = getContext().getEndpoint("direct:traccarLogin").createExchange();
    ex.getIn().setHeader("email", email);
    ex.getIn().setHeader("password", password);
    producer.send("direct:traccarLogin", ex);
  }

  // TODO auto restart/retry after disconnect: traccar.websocket.refresh

  @Override
  public void configure() throws Exception {

    onException(JsonParseException.class)
        .handled(true)
        .log(LoggingLevel.WARN, "Invalid JSON ignored: ${exception.message}");

    // internal Exception
    configureLoginAndConnectRoute();

    // CBR for WebSocket messages ---
    from("direct:start-websocket-connection")
        .routeId("process-websocket-message-route")
        .log(LoggingLevel.DEBUG, "Process WebSocket raw  message: ${body}")
        .unmarshal() // convert body to ..
        .json() // .. ArrayList with LinkedHashMap Map<String, Object>
        // else JsonParseException
        .process(
            // compute whether body should be considered empty
            ex -> {
              ex.getIn().setHeader("bm.emptyBody", isEmptyPayload(ex.getIn().getBody()));
            })
        .choice()
        // ===== JSON parsing
        .log("process json message: ${body}")

        // ===== empty message: use computed header
        .when(header("bm.emptyBody").isEqualTo(true))
        .log(LoggingLevel.DEBUG, "Empty message ignored: ${body}")

        // ===== devices
        .when(simple("${body[devices]} != null"))
        .log("devices message received: ${body}")
        .setBody(simple("${body[devices]}"))
        .bean(DeviceProcessor.class)

        // ===== positions
        .when(simple("${body[positions]} != null"))
        .log("positions message received: ${body}")
        .setBody(simple("${body[positions]}"))
        .bean(PositionProcessor.class)

        // ===== unknown/unimplemented message
        .otherwise()
        .log(LoggingLevel.WARN, "Unknown or Unimplemented WebSocket message ignored: ${body}")
        .end(); // of choice
  }

  // maybe we'll split this route in two
  // with optional Login via Api to obtain JSESSIONID cookie
  private void configureLoginAndConnectRoute() {
    from("direct:traccarLogin")
        .routeId("traccar-login-route")
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
                log.debug("Obtained JSESSIONID: {}", sessionManager.getJsessionidCookie());
                String wsRouteId = "traccar-ws-dynamic-route";
                if (getContext().getRouteController().getRouteStatus(wsRouteId) != null) {
                  getContext().getRouteController().stopRoute(wsRouteId);
                  getContext().removeRoute(wsRouteId);
                }

                String wsUri = deriveWsFromUrl(host, sessionManager.getJsessionidCookie());
                log.debug("Connecting to Traccar WebSocket at: {}", wsUri);

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
    log.debug("Derived Traccar WebSocket URL: {}", traccarWsUrl);
    return traccarWsUrl;
  }

  /** Helper used by route processor to determine if a payload is "empty". */
  // {}, {positions=[]} considered empty.
  private static boolean isEmptyPayload(Object body) {
    if (body == null) return true;
    if (body instanceof String) return ((String) body).trim().isEmpty();
    if (body instanceof Collection) return ((Collection<?>) body).isEmpty();
    if (body.getClass().isArray()) return Arrays.asList((Object[]) body).isEmpty();
    if (body instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) body;
      if (map.isEmpty()) return true;
      // consider map empty if all values are empty (recursively)
      for (Object v : map.values()) {
        if (v == null) continue;
        if (!isEmptyPayload(v)) return false;
      }
      return true;
    }
    return false;
  }
}
