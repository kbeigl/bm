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
 * TraccarWsClientRoute is divided into three steps: <br>
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

  // this should be moved outside (to ApiService) ?
  public void loginAndConnect(String email, String password) throws Exception {
    Exchange exchange = getContext().getEndpoint("direct:traccarLogin").createExchange();
    exchange.getIn().setHeader("email", email);
    exchange.getIn().setHeader("password", password);
    producer.send("direct:traccarLogin", exchange);
  }

  @Override
  public void configure() throws Exception {
    from("direct:traccarLogin")
        .routeId("traccarLoginRoute")
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
        .setBody(simple("email=${header.email}&password=${header.password}"))
        .toD(host + "/api/session")
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
                // Dynamically add a new route for the WebSocket connection with the session cookie
                String wsRouteId = "traccarWebSocketDynamicRoute";
                if (getContext().getRouteController().getRouteStatus(wsRouteId) != null) {
                  getContext().getRouteController().stopRoute(wsRouteId);
                  getContext().removeRoute(wsRouteId);
                }
                String wsUri = traccarWebSocketUrl + "&handshake.Cookie=JSESSIONID=" + jSessionId;
                getContext()
                    .addRoutes(
                        new RouteBuilder() {
                          @Override
                          public void configure() {
                            from("vertx-websocket:" + wsUri)
                                .routeId(wsRouteId)
                                .log("Received Traccar WebSocket update: ${body}");
                          }
                        });
              } else {
                log.error("Failed to obtain JSESSIONID from Traccar login response.");
                // Handle login failure, e.g., retry or send alert
              }
            })
        .onException(HttpOperationFailedException.class)
        .handled(true)
        .log("Traccar login failed: ${exception.message}")
        .end();
  }
}
