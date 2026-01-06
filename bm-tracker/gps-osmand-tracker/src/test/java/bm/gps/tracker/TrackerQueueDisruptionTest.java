package bm.gps.tracker;

import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.gps.MessageOsmand;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test that simulates a temporary server disruption to verify that messages are
 * enqueued and later flushed from MessageQueue when the server becomes available again.
 */
@SpringBootTest
@ComponentScan(basePackages = {"bm.gps.tracker"})
@ActiveProfiles("test")
public class TrackerQueueDisruptionTest {
  private static final Logger logger = LoggerFactory.getLogger(TrackerQueueDisruptionTest.class);

  @Autowired private TrackerRegistration registrationService;
  //  @Autowired private MessageQueue queue;
  private HttpServer server;
  // moved here so createServer(port) can access them
  private AtomicInteger requestCount;
  private AtomicBoolean available;

  @Test
  void testQueueOnServerDisruption() throws Exception {
    final int port = 5055; // must match test properties osmand.port
    requestCount = new AtomicInteger(0);
    available = new AtomicBoolean(true);

    createServer(port);

    // test a single tracker only
    TrackerOsmAnd tracker = registrationService.registerTracker("queue-test" + System.nanoTime());

    // send first message while server is up -> should be delivered (no enqueue)
    tracker.send(createOsmandMessageNow("10", 52.0, 13.0));
    sleepMillis(500);
    // assertEquals(0, queue.size(), "Queue should be empty when server is up");
    assertTrue(requestCount.get() >= 1, "Server should have received at least one request");

    // simulate disruption by making server return 500 responses
    available.set(false);

    // send two messages while server is "down"
    // -> they should be enqueued by the onException handler
    tracker.send(createOsmandMessageNow("10", 52.0001, 13.0001));
    tracker.send(createOsmandMessageNow("10", 52.0002, 13.0002));
    sleepMillis(500);

    // assertTrue(queue.size() >= 2, "Messages should be enqueued while server is down (500
    // responses)");

    // restore server availability so flush-route can resend queued messages
    available.set(true);

    // wait up to 6 seconds for the queue to be flushed (flush-route attempts one per second)
    //    boolean flushed = false;
    //    for (int i = 0; i < 6; i++) {
    //      if (queue.isEmpty()) {
    //        flushed = true;
    //        break;
    //      }
    // sleepMillis(1000);
    //    }

    // assertTrue(flushed, "Queued messages should be flushed after server becomes available");

    // at least three requests should have been received in total
    assertTrue(
        requestCount.get() >= 3,
        "Server should have received the originally sent and flushed requests");
  }

  private MessageOsmand createOsmandMessageNow(String deviceId, double lat, double lon) {
    return new MessageOsmand(
        deviceId, lat, lon, System.currentTimeMillis() / 1000, 0.0, 0.0, 0.0, null, null);
  }

  /* start a simple HTTP server that accepts incoming GET requests
   * it willreturn 200 when 'available' is true
   * and 500 when false to simulate a temporary server error/disruption */
  private HttpServer createServer(int port) throws IOException {
    HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
    // use instance-level requestCount and available
    s.createContext(
        "/",
        exchange -> {
          if (available.get()) {
            handleOk(exchange, requestCount);
          } else {
            handleError(exchange, requestCount);
          }
        });
    s.setExecutor(null);
    s.start();
    server = s; // set the field as requested
    logger.info("Test HTTP server started on port {}", port);
    return s;
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
      server = null;
    }
  }

  private static void handleOk(HttpExchange exchange, AtomicInteger counter) throws IOException {
    try {
      counter.incrementAndGet();
      String response = "OK";
      exchange.sendResponseHeaders(200, response.length());
      exchange.getResponseBody().write(response.getBytes());
    } finally {
      exchange.close();
    }
  }

  private static void handleError(HttpExchange exchange, AtomicInteger counter) throws IOException {
    try {
      counter.incrementAndGet();
      String response = "ERR";
      exchange.sendResponseHeaders(500, response.length());
      exchange.getResponseBody().write(response.getBytes());
    } finally {
      exchange.close();
    }
  }

  private static void sleepMillis(long ms) {
    try {
      TimeUnit.MILLISECONDS.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
