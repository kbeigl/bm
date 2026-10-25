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
package bm.sy.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bm.sy.ScotlandYard;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GamePropsTest {

  @AfterEach
  void clearSystemProperties() {
    System.clearProperty(ScotlandYard.PROP_DETECTIVES);
    System.clearProperty(ScotlandYard.PROP_ROUNDS);
    System.clearProperty(ScotlandYard.PROP_ROUND_MILLIS);
  }

  @Test
  void loadPropertiesFromClasspath() {
    Properties props = GameProps.loadProperties();
    assertNotNull(props);
    assertNotNull(props.getProperty(ScotlandYard.PROP_HOST));
    assertNotNull(props.getProperty(ScotlandYard.PROP_DETECTIVES));
  }

  @Test
  void parsePropertiesWithDefaults() {
    Properties props = new Properties();
    props.setProperty(ScotlandYard.PROP_DETECTIVES, "");
    props.setProperty(ScotlandYard.PROP_ROUNDS, " 15 ");
    props.setProperty(ScotlandYard.PROP_ROUND_MILLIS, "500");

    GameProps gameProps = new GameProps(props);
    assertEquals(2, gameProps.intProperty(ScotlandYard.PROP_DETECTIVES, 2));
    assertEquals(15, gameProps.intProperty(ScotlandYard.PROP_ROUNDS, 22));
    assertEquals(500L, gameProps.longProperty(ScotlandYard.PROP_ROUND_MILLIS, 100L));
    assertEquals(
        15, GameProps.intProperty(gameProps.getProperties(), ScotlandYard.PROP_ROUNDS, 22));
  }

  @Test
  void systemPropertyPrecedence() {
    Properties props = new Properties();
    props.setProperty(ScotlandYard.PROP_DETECTIVES, "3");
    System.setProperty(ScotlandYard.PROP_DETECTIVES, "5");

    GameProps gameProps = new GameProps(props);
    assertEquals("5", gameProps.property(ScotlandYard.PROP_DETECTIVES));
    assertEquals(5, gameProps.intProperty(ScotlandYard.PROP_DETECTIVES, 2));
  }

  @Test
  void invalidNumberThrowsException() {
    Properties props = new Properties();
    props.setProperty(ScotlandYard.PROP_ROUNDS, "not-a-number");
    GameProps gameProps = new GameProps(props);
    assertThrows(
        IllegalArgumentException.class, () -> gameProps.intProperty(ScotlandYard.PROP_ROUNDS, 22));
    assertThrows(
        IllegalArgumentException.class,
        () -> gameProps.longProperty(ScotlandYard.PROP_ROUNDS, 22L));
  }

  @Test
  void secretsMasking() {
    assertEquals("<not set>", GameProps.mask(null));
    assertEquals("<not set>", GameProps.mask(""));
    assertEquals("**", GameProps.mask("x"));
    assertEquals("**", GameProps.mask("xy"));
    assertEquals("a***n", GameProps.mask("admin"));
  }
}
