package bm.gps.tracker.camel;

import bm.gps.OsmAndMessage;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * Defines the Camel route for simulating OsmAnd GPS updates. The route is triggered on a timer and
 * uses the SimulatorService to determine if a message should be sent based on distance moved.
 */
@Component
public class TrackingRoute extends RouteBuilder {

  //  @Value("${tracker.host}")
  protected String protocolServer = "http://localhost:5055";

  // Inject the service where the simulation logic resides
  private final SimulatorService simulatorService;

  // Configuration for the target server and device ID
  //  private final String targetServerUrl = "http://localhost:8080/track";
  private final String deviceId = "10";

  // Spring Boot automatically injects this
  public TrackingRoute(SimulatorService simulatorService) {
    this.simulatorService = simulatorService;
  }

  @Override
  public void configure() {

    // --- Error Handling (Optional, but good practice) ---
    // onException(Exception.class).log(exceptionMessage()).log("❌ Failed to send GPS update.");

    // --- The Main Route ---
    from("timer:simTimer?period=500") // Trigger every 500ms (SIMULATION_TICK_MS)
        .routeId("OsmAnd-Simulator-Route")

        // 1. Process: Run simulation logic (movement & distance check)
        // The service returns GpsData if threshold met, or null if not.
        .bean(simulatorService, "updateLocation")

        // 2. Filter: Only continue if the body is not null (i.e., distance threshold met)
        .filter(body().isNotNull())

        // 3. Prepare HTTP Request: Map GpsData fields to OsmAnd query parameters
        // Using dynamic headers to build the query string is cleaner than manipulating the endpoint
        // URL.
        .process(
            exchange -> {
              OsmAndMessage data = exchange.getIn().getBody(OsmAndMessage.class);
              // Build the query string dynamically
              String queryString =
                  String.format(
                      "id=%s&lat=%.6f&lon=%.6f&timestamp=%d&speed=%.2f&bearing=%.2f&altitude=%.2f&batt=%d",
                      deviceId,
                      data.latitude(),
                      data.longitude(),
                      data.timestamp(),
                      data.speed(),
                      data.bearing(),
                      data.altitude(),
                      data.battery());

              // Set the query string as a header for the HTTP endpoint
              exchange.getIn().setHeader("CamelHttpQuery", queryString);
              exchange.getIn().setBody(""); // Clear body for the GET request
            })

        // 4. Send: Use the HTTP component to make the request
        .toD(protocolServer + "?bridgeEndpoint=true") // Sends to the target URL dynamically

        // 5. Log Result
        .log("✅ Successfully sent update for device ${header.CamelHttpQuery}");
  }
}
