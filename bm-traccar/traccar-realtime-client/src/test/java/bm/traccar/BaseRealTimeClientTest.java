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
package bm.traccar;

import bm.gps.tracker.TrackerOsmandConfig;
import bm.traccar.api.ApiException;
import bm.traccar.api.scenario.ScenarioLoader;
import bm.traccar.rt.RealTimeConfig;
import bm.traccar.rt.RealTimeController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests provides setup and teardown of a full scenario for realtime
 * client integration tests.
 *
 * <p>This class uses RealTimeConfig (not RealTimeClient) to load the library configuration for
 * testing. The RealTimeClientRunner is skipped in tests via @ActiveProfiles("test").
 */
@SpringBootTest(
    classes = RealTimeConfig.class,
    properties = "logging.config=classpath:logback-test.xml")
@Import(TrackerOsmandConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public abstract class BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseRealTimeClientTest.class);

  // inject some major components to be used in tests
  @Autowired protected RealTimeController controller;
  // @Autowired protected RealTimeClient client;
  @Autowired protected ScenarioLoader scenario;

  // no @Test in base class

  // @BeforeEach setup doesn't conserve state between tests
  @BeforeAll
  public void setup() throws Exception {
    scenario.setupScenario();
    logger.info("--- initialize realtime controller ---");
    if (controller.loginAndInitialize(scenario.admin)) {
      logger.info("\t***** RealTimeController initialized *****");
    } else {
      throw new Exception("RealTimeController loginAndInitialize failed in test setup.");
    }
  }

  // @AfterEach teardown doesn't conserve state between tests
  // check @DirtiesContext javadoc for details
  @AfterAll
  public void teardown() throws ApiException {
    scenario.teardownScenario();
    controller.shutdown(); // clean up WebSocket route
  }

  /**
   * Sleep helper for tests. Converts InterruptedException into a runtime exception and restores the
   * thread interrupt flag.
   */
  protected void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while sleeping", e);
    }
  }
}
