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

import bm.traccar.RealTimeApp;
import bm.traccar.rtapp.RealTimeContext;
import bm.traccar.scenario.ScenarioRunner;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Plain Java entry point to launch the {@link ScotlandYard} scenario.
 *
 * <p>The scenario itself is plain Java, only the underlying realtime stack ({@code
 * traccar-realtime-service}) is a Spring Boot application. This class boots that Spring context
 * (without a web server), hands the scenario settings from the Spring {@link Environment} (i.e.
 * {@code application.properties}, system properties, command line) to the {@link ScotlandYard}
 * scenario, executes it with the {@link ScenarioRunner} and finally closes the context.
 *
 * <p>Properties (see {@code src/main/resources/application.properties}):
 *
 * <ul>
 *   <li>{@value ScotlandYard#PROP_HOST} - Traccar server (REST API)
 *   <li>{@value ScotlandYard#PROP_DETECTIVES} - number of detectives
 *   <li>{@value ScotlandYard#PROP_ROUNDS} - number of rounds to play
 *   <li>{@value ScotlandYard#PROP_ROUND_MILLIS} - duration of a round in ms
 * </ul>
 */
public final class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  /** Properties handed from the Spring Environment to the scenario. */
  static final List<String> SCENARIO_PROPERTIES =
      List.of(
          ScotlandYard.PROP_HOST,
          ScotlandYard.PROP_USER,
          ScotlandYard.PROP_EMAIL,
          ScotlandYard.PROP_PASSWORD,
          // ScotlandYard.PROP_TOKEN,
          ScotlandYard.PROP_DETECTIVES,
          ScotlandYard.PROP_ROUNDS,
          ScotlandYard.PROP_ROUND_MILLIS);

  private Main() {}

  public static void main(String[] args) {
    int exitCode = 0;

    SpringApplication app = new SpringApplication(RealTimeApp.class);
    app.setWebApplicationType(WebApplicationType.NONE);

    try (ConfigurableApplicationContext spring = app.run(args)) {
      Environment env = spring.getEnvironment();
      env.getRequiredProperty(ScotlandYard.PROP_HOST);

      RealTimeContext context = spring.getBean(RealTimeContext.class);
      ScenarioRunner runner = spring.getBean(ScenarioRunner.class);

      ScotlandYard scenario = new ScotlandYard(settings(env));
      runner.execute(scenario, context);

    } catch (Exception e) {
      logger.error("Scotland Yard failed: {}", e.getMessage(), e);
      exitCode = 1;
    }

    System.exit(exitCode);
  }

  /** Collect the scenario settings from the Spring Environment. */
  static Properties settings(Environment env) {
    Properties settings = new Properties();
    for (String key : SCENARIO_PROPERTIES) {
      String value = env.getProperty(key);
      if (value != null) {
        settings.setProperty(key, value);
      }
    }
    return settings;
  }
}
