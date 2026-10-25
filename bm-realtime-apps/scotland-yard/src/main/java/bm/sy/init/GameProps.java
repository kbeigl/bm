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

import static bm.sy.ScotlandYard.PROP_DETECTIVES;
import static bm.sy.ScotlandYard.PROP_EMAIL;
import static bm.sy.ScotlandYard.PROP_HOST;
import static bm.sy.ScotlandYard.PROP_PASSWORD;
import static bm.sy.ScotlandYard.PROP_ROUNDS;
import static bm.sy.ScotlandYard.PROP_ROUND_MILLIS;
import static bm.sy.ScotlandYard.PROP_USER;

import bm.traccar.api.Api.Users.TraccarRole;
import bm.traccar.generated.model.dto.User;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handles loading, parsing and logging of Scotland Yard scenario properties. */
public class GameProps {

  private static final Logger logger = LoggerFactory.getLogger(GameProps.class);

  private final Properties properties;

  public GameProps() {
    this(new Properties());
  }

  public GameProps(Properties settings) {
    Objects.requireNonNull(settings, "settings");
    this.properties = new Properties(loadProperties());
    this.properties.putAll(settings);
  }

  public Properties getProperties() {
    return properties;
  }

  /** Log the realtime and the scotland yard settings, secrets are masked. */
  public void logProperties(
      Logger targetLogger,
      User host,
      TraccarRole hostRole,
      int nrOfDetectives,
      int nrOfRounds,
      long roundMillis) {
    logProperties(
        targetLogger != null ? targetLogger : logger,
        host,
        hostRole,
        properties,
        nrOfDetectives,
        nrOfRounds,
        roundMillis);
  }

  /** Log the realtime and the scotland yard settings, secrets are masked. */
  public static void logProperties(
      Logger targetLogger,
      User host,
      TraccarRole hostRole,
      Properties properties,
      int nrOfDetectives,
      int nrOfRounds,
      long roundMillis) {
    Logger log = targetLogger != null ? targetLogger : logger;
    log.info("host user         : {} ({}) as {}", host.getName(), host.getEmail(), hostRole);
    log.info("{}      : {}", PROP_HOST, properties.getProperty(PROP_HOST));
    log.info("{}      : {}", PROP_USER, properties.getProperty(PROP_USER));
    log.info("{}     : {}", PROP_EMAIL, properties.getProperty(PROP_EMAIL));
    log.info("{}  : {}", PROP_PASSWORD, mask(properties.getProperty(PROP_PASSWORD)));
    // log.info("{} : {}", PROP_TOKEN, mask(properties.getProperty(PROP_TOKEN)));
    log.info("{} : {}", PROP_DETECTIVES, nrOfDetectives);
    log.info("{}     : {}", PROP_ROUNDS, nrOfRounds);
    log.info("{}     : {} ms", PROP_ROUND_MILLIS, roundMillis);
  }

  /** Load {@code application.properties} from the classpath, empty if not present. */
  public static Properties loadProperties() {
    Properties props = new Properties();
    try (InputStream in =
        GameProps.class.getClassLoader().getResourceAsStream("application.properties")) {
      if (in != null) {
        props.load(in);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read application.properties", e);
    }
    return props;
  }

  /** System property, then scenario properties, then default. */
  public String property(String key) {
    return property(properties, key);
  }

  public int intProperty(String key, int defaultValue) {
    return intProperty(properties, key, defaultValue);
  }

  public long longProperty(String key, long defaultValue) {
    return longProperty(properties, key, defaultValue);
  }

  /** System property, then scenario properties, then default. */
  public static String property(Properties properties, String key) {
    String value = System.getProperty(key);
    return value != null ? value : properties.getProperty(key);
  }

  public static int intProperty(Properties properties, String key, int defaultValue) {
    String value = property(properties, key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(key + " is not a number: '" + value + "'");
    }
  }

  public static long longProperty(Properties properties, String key, long defaultValue) {
    String value = property(properties, key);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(key + " is not a number: '" + value + "'");
    }
  }

  public static String mask(String secret) {
    if (secret == null || secret.isEmpty()) {
      return "<not set>";
    }
    return secret.length() <= 2
        ? "**"
        : secret.charAt(0) + "***" + secret.charAt(secret.length() - 1);
  }
}
