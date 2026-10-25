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
package bm.sy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Plain unit test for the settings of {@link ScotlandYard} - no Traccar server required. */
class ScotlandYardTest {

  @AfterEach
  void clearSystemProperties() {
    System.clearProperty(ScotlandYard.PROP_DETECTIVES);
    System.clearProperty(ScotlandYard.PROP_ROUNDS);
    System.clearProperty(ScotlandYard.PROP_ROUND_MILLIS);
  }

  @Test
  void settingsFromApplicationProperties() {
    // no explicit settings -> values of src/main/resources/application.properties
    ScotlandYard game = new ScotlandYard(new Properties());
    Properties file = ScotlandYard.loadProperties();
    assertEquals(
        Integer.parseInt(file.getProperty(ScotlandYard.PROP_DETECTIVES)), game.getNrOfDetectives());
    assertEquals(
        Integer.parseInt(file.getProperty(ScotlandYard.PROP_ROUNDS)), game.getNrOfRounds());
    assertEquals(
        Long.parseLong(file.getProperty(ScotlandYard.PROP_ROUND_MILLIS)), game.getRoundMillis());
    assertEquals("Scotland Yard Game", game.getName());
    assertTrue(game.getHostRole().isEmpty(), "no host before init");
    assertTrue(game.getPlayerRole().isEmpty(), "no player role before init");
    assertTrue(game.getMyPawns().isEmpty(), "no pawns before init");
    assertTrue(game.getCreatedUsers().isEmpty());
    assertTrue(game.getCreatedDevices().isEmpty());
  }

  @Test
  void defaultsForMissingKeys() {
    // blank values are treated as missing and fall back to the defaults
    Properties props = new Properties();
    props.setProperty(ScotlandYard.PROP_DETECTIVES, "");
    props.setProperty(ScotlandYard.PROP_ROUNDS, " ");
    props.setProperty(ScotlandYard.PROP_ROUND_MILLIS, "");
    ScotlandYard game = new ScotlandYard(props);
    assertEquals(ScotlandYard.DEFAULT_DETECTIVES, game.getNrOfDetectives());
    assertEquals(ScotlandYard.DEFAULT_ROUNDS, game.getNrOfRounds());
    assertEquals(ScotlandYard.DEFAULT_ROUND_MILLIS, game.getRoundMillis());
  }

  @Test
  void settingsFromProperties() {
    Properties props = new Properties();
    props.setProperty(ScotlandYard.PROP_DETECTIVES, "4");
    props.setProperty(ScotlandYard.PROP_ROUNDS, " 10 ");
    props.setProperty(ScotlandYard.PROP_ROUND_MILLIS, "0");

    ScotlandYard game = new ScotlandYard(props);
    assertEquals(4, game.getNrOfDetectives());
    assertEquals(10, game.getNrOfRounds());
    assertEquals(0, game.getRoundMillis());
  }

  @Test
  void systemPropertyOverridesFile() {
    Properties props = new Properties();
    props.setProperty(ScotlandYard.PROP_DETECTIVES, "4");
    System.setProperty(ScotlandYard.PROP_DETECTIVES, "5");

    assertEquals(5, new ScotlandYard(props).getNrOfDetectives());
  }

  @Test
  void loadsApplicationPropertiesFromClasspath() {
    ScotlandYard game = new ScotlandYard();
    Properties props = game.getProperties();
    // the realtime settings of src/test/resources/application.properties
    assertNotNull(props.getProperty(ScotlandYard.PROP_HOST));
    assertNotNull(props.getProperty(ScotlandYard.PROP_USER));
    assertNotNull(props.getProperty(ScotlandYard.PROP_EMAIL));
    // assertNotNull(props.getProperty(ScotlandYard.PROP_TOKEN));
  }

  @Test
  void invalidSettings() {
    assertThrows(IllegalArgumentException.class, () -> game(ScotlandYard.PROP_DETECTIVES, "0"));
    assertThrows(IllegalArgumentException.class, () -> game(ScotlandYard.PROP_ROUNDS, "-1"));
    assertThrows(IllegalArgumentException.class, () -> game(ScotlandYard.PROP_ROUND_MILLIS, "-5"));
    assertThrows(IllegalArgumentException.class, () -> game(ScotlandYard.PROP_ROUNDS, "many"));
    assertThrows(NullPointerException.class, () -> new ScotlandYard((Properties) null));
  }

  @Test
  void secretsAreMasked() {
    assertEquals("<not set>", ScotlandYard.mask(null));
    assertEquals("<not set>", ScotlandYard.mask(""));
    assertEquals("**", ScotlandYard.mask("ab"));
    assertEquals("a***n", ScotlandYard.mask("admin"));
  }

  private static ScotlandYard game(String key, String value) {
    Properties props = new Properties();
    props.setProperty(key, value);
    return new ScotlandYard(props);
  }
}
