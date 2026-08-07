package bm.traccar.scenario;

import bm.traccar.BaseRealTimeAppTest;
import bm.traccar.RealTimeApp;
import bm.traccar.rtapp.RealTimeAppContext;
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
 *   <li>Provides access to RealTimeAppContext and ScenarioRunner
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
 *     scenarioRunner.execute(scenario, realTimeAppContext);
 *   }
 * }
 * </pre>
 */
@SpringBootTest(
    classes = RealTimeApp.class,
    properties = "logging.config=classpath:logback-test.xml")
@Import({RealTimeAppContext.class, ScenarioRunner.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseScenarioTest extends BaseRealTimeAppTest {
  protected static final Logger logger = LoggerFactory.getLogger(BaseScenarioTest.class);

  /* We don't want to use the ScenarioLoader here.
   * Should be done inside each scenario (init) */
  // @Autowired protected ScenarioLoader scenarioLoader;
  @Autowired protected RealTimeAppContext realTimeAppContext;
  @Autowired protected ScenarioRunner scenarioRunner;

  @BeforeAll
  public void setup() throws Exception {
    super.setup();
    // setup scenario-specific context
    logger.info("RealTimeAppContext initialized");
    logger.debug("  - RealTimeController: {}", realTimeAppContext.getRealTimeController() != null);
    logger.debug("  - Api: {}", realTimeAppContext.getApi() != null);
    logger.debug(
        "  - TrackerRegistration: {}", realTimeAppContext.getTrackerRegistration() != null);
    logger.debug("  - RealTimeAppService: {}", realTimeAppContext.getAppService() != null);
  }

  @AfterAll
  public void teardown() {
    logger.info("Tearing down BaseScenarioTest");
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
    scenarioRunner.execute(scenario, realTimeAppContext);
  }
}
