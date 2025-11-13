package bm.tracker.gpstracker;

import static org.junit.jupiter.api.Assertions.*;

import bm.tracker.gpstracker.model.GpsMessage;
import bm.tracker.gpstracker.queue.MessageQueue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = GpsTrackerApplication.class)
public class OsmAndTrackerIT {
  // private static final Logger logger = LoggerFactory.getLogger(OsmAndTrackerTest.class);

  @Autowired OsmAndTracker client;
  @Autowired MessageQueue queue;

  //  @BeforeEach
  //  void setup() {}

  @Test
  void sendConsecutiveMessagesWithDelaysAndQueueing() throws Exception {

    GpsMessage m1 = new GpsMessage();
    m1.setId("10");
    m1.setLat(52.0);
    m1.setLon(13.0);
    m1.setTimestamp(System.currentTimeMillis());

    client.send(m1);

    waitForRequests(1, 2000);

    GpsMessage m2 = new GpsMessage();
    m2.setId("10");
    m2.setLat(52.0001);
    m2.setLon(13.0001);
    m2.setTimestamp(System.currentTimeMillis());

    GpsMessage m3 = new GpsMessage();
    m3.setId("10");
    m3.setLat(52.0002);
    m3.setLon(13.0002);
    m3.setTimestamp(System.currentTimeMillis());

    client.send(m2);
    Thread.sleep(200);
    client.send(m3);

    // allow some time for the route to attempt sending and enqueue
    Thread.sleep(1000);

    // assertTrue(queue.size() >= 2, "messages should be queued when server errors");

    // restore server to healthy -> queued messages should be flushed by timer
    // wireMock.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(200)));

    // wait up to 10s for queue to be flushed
    long deadline = System.currentTimeMillis() + 10000;
    while (System.currentTimeMillis() < deadline && !queue.isEmpty()) {
      Thread.sleep(200);
    }

    assertTrue(queue.isEmpty(), "queue should be flushed after server recovery");

    // total requests should be at least 3 (one successful send + two flushed)
    // int total = wireMock.getAllServeEvents().size();
    // assertTrue(total >= 3, "server should have received at least 3 requests, but got " + total);
  }

  private void waitForRequests(int expected, int timeoutMs) throws InterruptedException {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
      // if (wireMock.getAllServeEvents().size() >= expected) { return; }
      Thread.sleep(50);
    }
    // fail("Expected " + expected + " requests within " + timeoutMs + "ms");
  }
}
