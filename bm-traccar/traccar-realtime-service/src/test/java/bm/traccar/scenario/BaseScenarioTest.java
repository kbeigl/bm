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
package bm.traccar.scenario;

import bm.traccar.BaseRealTimeAppTest;
import bm.traccar.RealTimeApp;
import bm.traccar.rtapp.RealTimeContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Base class for scenario-based integration tests.
 *
 * <p>This class:
 *
 * <ul>
 *   <li>Starts the RealTimeApp with full Spring context
 *   <li>Provides access to RealTimeContext and ScenarioRunner
 *   <li>Allows manual scenario execution in test methods
 * </ul>
 *
 * <p><b>Usage:</b>
 *
 * <pre>
 * public class MyScenarioTest extends BaseScenarioTest {
 *   {@literal @}Test
 *   void testMyScenario() throws Exception {
 *     Scenario scenario = new MyScenario();
 *     scenarioRunner.execute(scenario, realTimeContext);
 *   }
 * }
 * </pre>
 */
@SpringBootTest(
    classes = RealTimeApp.class,
    properties = "logging.config=classpath:logback-test.xml")
@Import({RealTimeContext.class, ScenarioRunner.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseScenarioTest extends BaseRealTimeAppTest {
  protected static final Logger logger = LoggerFactory.getLogger(BaseScenarioTest.class);

  /* We don't want to use the ScenarioLoader here.
   * Should be done inside each scenario (init) */
  // @Autowired protected ScenarioLoader scenarioLoader;
  @Autowired protected RealTimeContext realTimeContext;
  @Autowired protected ScenarioRunner scenarioRunner;

  @BeforeAll
  public void setup() throws Exception {
    super.setup();
    // is this clean? should be set implicitly with successful login ==
    // setup scenario-specific context
    //    if (admin != null) {
    //      realTimeContext.setRealtimeAdmin(admin);
    //    }
    // ==
    logger.info("RealTimeContext initialized");
    logger.debug("  - RealTimeController: {}", realTimeContext.getRealTimeController() != null);
    logger.debug("  - Api: {}", realTimeContext.getApi() != null);
    logger.debug("  - TrackerRegistration: {}", realTimeContext.getTrackerRegistration() != null);
    logger.debug("  - RealTimeService: {}", realTimeContext.getService() != null);
  }

  @AfterAll
  public void teardown() {
    logger.info("Tearing down BaseScenarioTest");
    realTimeContext.setRealtimeAdmin(null);
    // handle controller shutdown and admin user cleanup
    super.teardown();
  }

  /**
   * Helper method to execute a scenario.
   *
   * @param scenario the scenario to execute
   * @throws Exception if scenario execution fails
   */
  protected void executeScenario(Scenario scenario) throws Exception {
    scenarioRunner.execute(scenario, realTimeContext);
  }
}
