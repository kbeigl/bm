package bm.gps.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bm.gps.MessageOsmand;
import bm.gps.gpx.file.CopyFileRoute;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Test for the Camel-based GPX file polling and parsing route.
 *
 * <p>This test uses Camel's ProducerTemplate to send GPX files directly to the route, eliminating
 * the need for explicit java.nio.file operations. The route polls from the configured
 * gps.player.send-directory and parses GPX files into Osmand messages.
 */
@SpringBootTest(
    classes = PlayerApplication.class,
    properties = {
      "gps.player.output-uri=seda:camel-test-output", // redundant to OUTPUT_URI ?
      "logging.config=classpath:logback-test.xml"
    })
@ExtendWith(OutputCaptureExtension.class)
class GpxMessageRouteTest {

  private static final String OUTPUT_URI = "seda:camel-test-output";
  private static final int EXPECTED_MESSAGE_COUNT = 161;
  private static final long FILE_MOVE_TIMEOUT_MS = 10_000L;

  @Autowired private ProducerTemplate producer;
  @Autowired private ConsumerTemplate consumer;
  @Autowired private CamelContext camel;

  @Value("${gps.player.send-directory}")
  private String sendDirectory;

  // not necessarily related to any project
  @Value("${gpx.source-directory}")
  private String sourceDirectory;

  /**
   * Test that the GpxMessageRoute polls for new *.gpx files and parses them into Osmand messages
   * using only Camel operations.
   *
   * <p>The route is triggered by sending a GPX file directly to the file endpoint using
   * ProducerTemplate, avoiding explicit filesystem operations.
   */
  @Test
  void sendAndParseGpxFile() throws Exception {
    String fileName = "RGB-BUELL-SE-080607.gpx";
    Path parsedFile = parsedPath(fileName);
    Path errorFile = errorPath(fileName);
    Files.deleteIfExists(parsedFile);
    Files.deleteIfExists(errorFile);

    copyGpxFromSourceToInputAndAwait(fileName);

    // Consume the parsed Osmand messages from the output URI
    List<MessageOsmand> messages = new ArrayList<>(EXPECTED_MESSAGE_COUNT);
    for (int index = 0; index < EXPECTED_MESSAGE_COUNT; index++) {
      MessageOsmand message = consumer.receiveBody(OUTPUT_URI, 10_000L, MessageOsmand.class);
      assertNotNull(message, "Expected GPX message at index " + index);
      messages.add(message);
    }
    assertMessages(messages);

    awaitFileMove(parsedFile);
  }

  @Test
  void sendInvalidGpxFile(CapturedOutput output) throws Exception {
    String fileName = "RGB-BUELL-SE-80608-INVALID.gpx";
    Path parsedFile = parsedPath(fileName);
    Path errorFile = errorPath(fileName);
    Files.deleteIfExists(parsedFile);
    Files.deleteIfExists(errorFile);

    copyGpxFromSourceToInputAndAwait(fileName);
    // Expect no messages to be produced for the invalid GPX file
    assertNull(consumer.receiveBody(OUTPUT_URI, 1_000L, MessageOsmand.class));
    awaitFileMove(errorFile);
    assertTrue(output.toString().contains("Malformed GPX XML in"));
    assertFalse(output.toString().contains("ERROR DefaultErrorHandler - Failed delivery"));
  }

  // @Test
  void moveInvalidGpxFileToErrorDirectory() throws Exception {
    String fileName = "invalid-" + System.nanoTime() + ".gpx";
    Path parsedFile = parsedPath(fileName);
    Path errorFile = errorPath(fileName);
    Files.deleteIfExists(parsedFile);
    Files.deleteIfExists(errorFile);

    producer.sendBodyAndHeader(
        "file:" + sendDirectory,
        "<gpx><trk><name>broken</name></trk>",
        Exchange.FILE_NAME,
        fileName);

    awaitFileMove(errorFile);
  }

  private void copyGpxFromSourceToInputAndAwait(String fileName) throws Exception {
    assertTrue(
        Files.exists(Path.of(sourceDirectory, fileName)),
        "Sample GPX source file must be available");

    CopyFileRoute copyFileRoute = new CopyFileRoute(fileName, sourceDirectory, sendDirectory);
    copyFileRoute.copyAndAwait(camel, producer);
  }

  private void assertMessages(List<MessageOsmand> messages) {
    MessageOsmand first = messages.getFirst();
    assertEquals("RGB-BUELL-SE-080607", first.id());
    assertEquals(49.0310032d, first.lat());
    assertEquals(12.1030925d, first.lon());
    assertEquals(Instant.parse("2008-06-07T09:08:55Z").getEpochSecond(), first.timestamp());
    assertEquals(319.7756348d, first.altitude());
    assertNull(first.speed());
    assertNull(first.bearing());

    MessageOsmand second = messages.get(1);
    assertEquals("RGB-BUELL-SE-080607", second.id());
    assertNotNull(second.speed());
    assertTrue(second.speed() >= 0.0d);
    assertNotNull(second.bearing());
    assertTrue(second.bearing() >= 0.0d && second.bearing() <= 360.0d);

    MessageOsmand last = messages.getLast();
    assertEquals(48.9773291d, last.lat());
    assertEquals(12.1578119d, last.lon());
    assertEquals(Instant.parse("2008-06-07T09:36:53Z").getEpochSecond(), last.timestamp());
    assertEquals(329.8695068d, last.altitude());
  }

  private Path parsedPath(String fileName) {
    return Path.of(sendDirectory, "parsed", fileName);
  }

  private Path errorPath(String fileName) {
    return Path.of(sendDirectory, "error", fileName);
  }

  private void awaitFileMove(Path expectedPath) throws Exception {
    long deadline = System.currentTimeMillis() + FILE_MOVE_TIMEOUT_MS;
    while (System.currentTimeMillis() < deadline) {
      if (Files.exists(expectedPath)) {
        return;
      }
      Thread.sleep(100L);
    }
    assertTrue(Files.exists(expectedPath), "Expected moved file at " + expectedPath);
  }
}
