package bm.gps.gpx;

import java.io.File;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GpxMessageRoute extends RouteBuilder {

  private final Gpx2OsmandParser parser;
  private final String sendDirectory;
  private final String outputUri;
  private final String include;
  private final long pollDelayMs;
  private final long initialDelayMs;
  private final String deviceId;

  public GpxMessageRoute(
      Gpx2OsmandParser parser,
      @Value("${gps.player.send-directory}") String sendDirectory,
      @Value("${gps.player.output-uri}") String outputUri,
      @Value("${gps.player.include:.*\\.gpx}") String include,
      @Value("${gps.player.poll-delay-ms:1000}") long pollDelayMs,
      @Value("${gps.player.initial-delay-ms:0}") long initialDelayMs,
      @Value("${gps.player.device-id:}") String deviceId) {
    this.parser = parser;
    this.sendDirectory = sendDirectory;
    this.outputUri = outputUri;
    this.include = include;
    this.pollDelayMs = pollDelayMs;
    this.initialDelayMs = initialDelayMs;
    this.deviceId = deviceId;
  }

  @Override
  public void configure() {
    onException(IllegalArgumentException.class)
        .maximumRedeliveries(0)
        .handled(false)
        .logExhausted(false)
        .logExhaustedMessageHistory(false)
        .logRetryAttempted(false)
        .logRetryStackTrace(false)
        .logStackTrace(false)
        .retryAttemptedLogLevel(LoggingLevel.DEBUG);

    fromF(
            "file:%s?include=%s&delay=%d&initialDelay=%d&move=parsed/${file:name}&moveFailed=error/${file:name}",
            sendDirectory, include, pollDelayMs, initialDelayMs)
        // routeId should be filename or <trk><name>
        // validate individual or general route creation
        // ? moveFailed/ for invalid files
        .routeId("gpx-file-player")
        .process(
            exchange -> {
              File gpxFile = exchange.getIn().getBody(File.class);
              if (gpxFile == null) {
                throw new IllegalArgumentException(
                    "Unable to resolve GPX file from Camel file exchange");
              }
              exchange.getIn().setBody(gpxFile);
              if (deviceId != null && !deviceId.isBlank()) {
                exchange.getIn().setHeader(GpxPlayerHeaders.DEVICE_ID, deviceId.trim());
              } else {
                exchange.getIn().removeHeader(GpxPlayerHeaders.DEVICE_ID);
              }
            })
        .bean(parser, "parse")
        .split(body())
        .to(outputUri);
  }
}
