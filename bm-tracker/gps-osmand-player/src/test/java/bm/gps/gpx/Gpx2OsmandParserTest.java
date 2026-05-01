package bm.gps.gpx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class Gpx2OsmandParserTest {

  private final Gpx2OsmandParser parser = new Gpx2OsmandParser();

  @TempDir Path tempDir;

  @Test
  void logMalformedXmlWithLineAndColumn(CapturedOutput output) throws IOException {
    Path invalidGpxFile = tempDir.resolve("broken.gpx");
    Files.writeString(invalidGpxFile, "<gpx>\n  <trkpt lat=\"49.0\" lon=\"12.0\"></gpx>");

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> parser.parse(invalidGpxFile.toFile(), null));

    assertTrue(exception.getMessage().contains("Failed to parse GPX file"));
    assertTrue(exception.getMessage().contains("line"));
    assertTrue(exception.getMessage().contains("column"));
    assertTrue(output.toString().contains("Malformed GPX XML in"));
    assertFalse(output.toString().contains("[Fatal Error]"));
  }
}
