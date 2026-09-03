/*
 * (C) Copyright 2026 Kristof Beiglböck
 *               kbeigl.github.io/bm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bm.gps.tracker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.gps.GeoTools;
import bm.gps.tracker.TrackerOsmAnd.TrackerStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * Simulate the OsmAnd tracking process with the TrackerOsmAnd API. A tracker is registered via
 * TrackerRegistration and moved on a circle around Hockenheim. A new GPS update is only sent when
 * the distance to the last sent position (calculated with GeoTools) exceeds a threshold - replacing
 * the timer/route based simulation of the old bm.gps.remove package.
 *
 * <p>This test can run with an embedded HTTP server (on port 5055) to receive the tracker messages,
 * or it can send to an external server if one is already running on that port. And a tracker with
 * the same unique ID ("10") must be registered on the external server to accept the messages.
 */
@SpringBootTest
@ComponentScan(basePackages = {"bm.gps.tracker"})
@ActiveProfiles("test")
class CircleSimulatorTest {

  private static final Logger logger = LoggerFactory.getLogger(CircleSimulatorTest.class);

  // Configuration
  private static final double DISTANCE_THRESHOLD_METERS = 50;
  private static final double CENTER_LAT = 49.329646; // Hockenheim
  private static final double CENTER_LON = 8.568838;
  private static final double RADIUS = 0.004; // ~400m radius
  private static final long SIMULATION_TICK_MS = 300;
  private static final double DEGREES_PER_TICK = 2.0;

  // increas duration to watch the simulation on the Hockenheim Ring track
  private static final long SIMULATION_DURATION_MS = 5 * 1000;

  @Autowired private TrackerRegistration registrationService;

  private HttpServer server;
  private AtomicInteger requestCount;

  @BeforeEach
  void startServer() {
    final int port = 5055; // must match test properties osmand.dcs
    requestCount = new AtomicInteger(0);
    try {
      server = HttpServer.create(new InetSocketAddress(port), 0);
      server.createContext("/", this::handleOk);
      server.setExecutor(null);
      server.start();
      logger.info("Test HTTP server started on port {}", port);
    } catch (IOException e) {
      // an external server (e.g. Traccar) is already running on the OsmAnd port
      logger.info("Port {} already in use - sending to the external server instead", port);
      server = null;
    }
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
      server = null;
    }
  }

  @Test
  void runCircleSimulation() throws Exception {
    System.out.println("------------------------------------------------------------------");
    System.out.println("Starting Circle Simulator Test around Hockenheim ...");
    System.out.println("Tracker ticks every " + SIMULATION_TICK_MS + "ms and sends a message");
    System.out.println(
        "every ~"
            + (int) DISTANCE_THRESHOLD_METERS
            + " meters for "
            + SIMULATION_DURATION_MS / 1000
            + " seconds.");
    System.out.println("------------------------------------------------------------------");

    // same device id as the old bm.gps.remove.TrackingRoute simulation
    String uniqueId = "10";
    TrackerOsmAnd tracker = registrationService.registerTracker(uniqueId);
    assertNotNull(tracker, "Tracker should be registered");
    // get object reference to the TrackerStatus ONCE for the simulation!
    TrackerStatus status = tracker.getTrackerStatus();

    double lastSentLat = CENTER_LAT;
    double lastSentLon = CENTER_LON;
    int steps = 0;
    int messagesSent = 0;

    long start = System.currentTimeMillis();
    while (System.currentTimeMillis() - start < SIMULATION_DURATION_MS) {

      // --- 1. Calculate the next position on the circle ---
      double angle = Math.toRadians(steps * DEGREES_PER_TICK);
      double newLat = CENTER_LAT + (RADIUS * Math.cos(angle));
      double newLon = CENTER_LON + (RADIUS * Math.sin(angle));

      // --- 2. Calculate distance from the *last sent* position using GeoTools ---
      double distanceMoved = GeoTools.distanceMeters(lastSentLat, lastSentLon, newLat, newLon);

      // --- 3. Check if threshold is met ---
      if (distanceMoved >= DISTANCE_THRESHOLD_METERS) {
        logger.info(
            String.format("Moved %.1f meters. Threshold met. Sending update ...", distanceMoved));

        // TrackerStatus status = tracker.getTrackerStatus();
        status.setLatitude(newLat);
        status.setLongitude(newLon);
        status.setAltitude(100.0 + (Math.sin(angle) * 10)); // vary altitude
        status.setSpeed(15.0); // 15 m/s
        status.setBearing((steps * DEGREES_PER_TICK + 90) % 360); // tangent to circle
        status.setBattery(100.0);
        status.setFixTime(OffsetDateTime.now());
        tracker.sendTrackerStatus();

        // Update state
        lastSentLat = newLat;
        lastSentLon = newLon;
        messagesSent++;
      }

      steps++;
      TimeUnit.MILLISECONDS.sleep(SIMULATION_TICK_MS);
    }

    System.out.println("Simulation run complete. Messages sent: " + messagesSent);
    System.out.println("Last sent position: Lat=" + lastSentLat + ", Lon=" + lastSentLon);
    System.out.println("------------------------------------------------------------------");

    assertTrue(messagesSent >= 1, "At least one message should have been sent");
    if (server != null) {
      System.out.println("Embedded server received " + requestCount.get() + " requests.");
      assertTrue(
          requestCount.get() >= messagesSent,
          "Server should have received all sent tracker messages");
    }
  }

  private void handleOk(HttpExchange exchange) throws IOException {
    try {
      requestCount.incrementAndGet();
      logger.info("Server received: {}", exchange.getRequestURI().getQuery());
      String response = "OK";
      exchange.sendResponseHeaders(200, response.length());
      exchange.getResponseBody().write(response.getBytes());
    } finally {
      exchange.close();
    }
  }
}
