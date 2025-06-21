package bm.traccar.ws;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

// @Component
public class TraccarWsClientRouteExperimental extends RouteBuilder {

  @Value("${traccar.user.password}")
  private String password;

  @Value("${traccar.user.email}")
  private String email;

  @Value("${traccar.host}")
  private String host;

  @Value("${traccar.websocket.url}")
  private String traccarWebSocketUrl;

  // CookieManager, um die Session-Cookies zu verwalten
  // Wird beim Erstellen der Komponente instanziiert.
  private CookieManager cookieManager = new CookieManager();

  // verify this approach at this point
  // @Produce("direct:start")
  @Autowired protected ProducerTemplate template;

  //  private final PositionProcessor positionProcessor;
  //  TraccarWsClientRoute(PositionProcessor positionProcessor) {
  //    this.positionProcessor = positionProcessor;
  //  }

  @Override
  public void configure() throws Exception {

    // WICHTIG: Setzt den Standard-CookieHandler für die HTTP-Anfragen im JVM.
    // Dies ist der entscheidende Schritt, damit HttpURLConnection (von Camel-HTTP verwendet)
    // die Cookies automatisch im CookieManager speichern kann.
    // Dies muss nur einmal pro JVM/Anwendung erfolgen.
    CookieHandler.setDefault(cookieManager);

    // --- Step 1: Login to Traccar and get JSESSIONID cookie ---
    from("timer:traccarLoginTimer?period=3600000") // refresh session every hour
        .routeId("traccarLoginRoute")
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/x-www-form-urlencoded"))
        .setBody(simple("email=" + email + "&password=" + password))
        .toD(host + "/api/session") // Perform login
        .process(
            exchange -> {
              log.debug("response headers after traccar login: " + exchange.getIn().getHeaders());
              String setCookieHeader = exchange.getIn().getHeader("Set-Cookie", String.class);
              log.info("Set-Cookie: " + setCookieHeader);

              URI loginUri = null;
              try {
                loginUri = new URI(host + "/api/session"); // <-- FIX
              } catch (Exception e) {
                System.err.println("Fehler beim Erstellen der Login-URI: " + e.getMessage());
                throw new RuntimeException("Konnte Login-URI nicht erstellen", e);
              }

              if (setCookieHeader != null) { // widen
                try {
                  List<HttpCookie> parsedCookies = HttpCookie.parse(setCookieHeader);
                  for (HttpCookie cookie : parsedCookies) {
                    // Stellen Sie sicher, dass das Cookie nicht abgelaufen ist und
                    // der Domain/Pfad passt.
                    // Der CookieManager sollte dies intern handhaben,
                    // aber explizite Checks können helfen.
                    // Für JSESSIONID ist Path=/ und keine Domain oft der Fall, was passt.
                    // Wichtig: Fügen Sie das Cookie für die URI hinzu, von der es kam.
                    cookieManager.getCookieStore().add(loginUri, cookie);
                    log.info(
                        "Manuell zum CookieStore hinzugefügt: Name={}, Value={}",
                        cookie.getName(),
                        cookie.getValue());
                  }
                } catch (IllegalArgumentException e) {
                  log.error("Fehler beim Parsen des 'Set-Cookie'-Headers: {}", e.getMessage());
                }
              }

              List<HttpCookie> cookies = null;
              try {
                cookies =
                    cookieManager
                        .getCookieStore()
                        .get(loginUri); // <-- VERWENDET DIE KORRIGIERTE URI
                log.info("Abrufen von Cookies aus dem CookieStore für URI: " + loginUri);
              } catch (Exception e) {
                log.error("Fehler beim Abrufen von Cookies aus dem CookieStore: " + e.getMessage());
              }

              String jSessionId = null;
              if (cookies != null) {
                for (HttpCookie storedCookie : cookies) {
                  log.info(
                      "  Cookie: Name={}, Value={}, Domain={}, Path={}",
                      storedCookie.getName(),
                      storedCookie.getValue(),
                      storedCookie.getDomain(),
                      storedCookie.getPath());
                  if ("JSESSIONID".equals(storedCookie.getName())) {
                    jSessionId = storedCookie.getValue();
                  }
                }
              }

              if (jSessionId != null) {
                log.info("Successfully obtained JSESSIONID: {}", jSessionId);
                // exchange.setProperty("JSESSIONID", jSessionId);

                // grab jSessionId from browser session
                // jSessionId = "node01cllqhrahg3mr1nng23l2a9cxn3.node0";

                // Trigger the WebSocket connection route
                // WICHTIG: JSESSIONID als Header an die nächste Route senden
                // template.sendBody("direct:connectTraccarWebSocket", "Connect");
                template.sendBodyAndHeader( // Geändert von sendBody
                    "direct:connectTraccarWebSocket",
                    "Connect", // Body der Nachricht
                    // Neuer Header-Name für die JSESSIONID, i.e. hiden parameter
                    "TraccarJSessionId",
                    jSessionId // Wert der JSESSIONID
                    );

              } else {
                log.error("Failed to obtain JSESSIONID from Traccar login response.");
                // Handle login failure, e.g., retry or send alert
              }
            })
        .onException(HttpOperationFailedException.class)
        .handled(true)
        .log("Traccar login failed: ${exception.message}")
        .end();

    // --- Step 2: Connect to WebSocket with JSESSIONID ---
    from("direct:connectTraccarWebSocket")
        .routeId("traccarWebSocketConnectionRoute")
        .process(
            exchange -> {
              // String jSessionId = exchange.getProperty("JSESSIONID", String.class);
              // log.info("jSessionId = " + jSessionId);

              // WICHTIG: JSESSIONID nun aus dem Header der eingehenden Nachricht abrufen
              String jSessionId = exchange.getIn().getHeader("TraccarJSessionId", String.class);
              if (jSessionId != null) {
                // Traccar's WebSocket expects the JSESSIONID as a cookie in the initial handshake
                // The vertx-websocket component allows setting custom headers, but explicitly
                // setting the Cookie header
                // with a pre-obtained JSESSIONID is the common approach for Traccar.
                String cookieHeaderValue = "JSESSIONID=" + jSessionId;
                exchange.getIn().setHeader("Cookie", cookieHeaderValue);
                log.info(
                    "Attempting to connect to Traccar WebSocket with 'Cookie' Header: {}",
                    cookieHeaderValue);
              } else {
                log.warn("No JSESSIONID available. Cannot establish Traccar WebSocket connection.");
                exchange.setException(
                    new IllegalStateException("No JSESSIONID to establish WebSocket."));
              }
            })
        .toD("vertx-websocket:" + traccarWebSocketUrl) // ?consumeAsClient=true
        .log("Connected to Traccar WebSocket.")
        .to("direct:traccarWebSocketMessages");

    // --- Step 3: Process incoming WebSocket messages ---
    from("direct:traccarWebSocketMessages")
        .routeId("traccarWebSocketMessageProcessingRoute")
        .unmarshal()
        .json(JsonLibrary.Jackson) // Unmarshal JSON to Map/POJO
        .log("Received Traccar WebSocket update: ${body}");

    // Here you can add your custom logic:
    // .choice()
    //     .when(simple("${body.containsKey('positions')}"))
    //         .log("New Positions: ${body.positions}")
    //         .to("bean:positionProcessor") // Process position data
    //     .when(simple("${body.containsKey('devices')}"))
    //         .log("New Devices: ${body.devices}")
    //         .to("bean:deviceProcessor") // Process device data
    //     .when(simple("${body.containsKey('events')}"))
    //         .log("New Events: ${body.events}")
    //         .to("bean:eventProcessor") // Process event data
    // .end()
    // .otherwise()
    //     .log("Unknown Traccar WebSocket message type: ${body}")
    // .endChoice();
  }
}
