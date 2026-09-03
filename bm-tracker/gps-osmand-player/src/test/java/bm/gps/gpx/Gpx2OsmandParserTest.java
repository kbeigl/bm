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
