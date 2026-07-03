package bm.traccar.scenario.examples;

import bm.traccar.scenario.BaseScenarioTest;
import bm.traccar.scenario.Scenario;
import org.junit.jupiter.api.Test;

/**
 * Example test demonstrating how to use the scenario framework.
 *
 * <p>This test shows how to:
 *
 * <ul>
 *   <li>Extend BaseScenarioTest to get scenario infrastructure
 *   <li>Create scenario instances
 *   <li>Execute scenarios using the ScenarioRunner
 *   <li>Run multiple scenarios in separate tests
 * </ul>
 */
public class ScenarioExamplesIT extends BaseScenarioTest {

  @Test
  void testSimpleTrackerScenario() throws Exception {
    logger.info("Starting SimpleTrackerScenario test");
    Scenario scenario = new SimplestTrackerScenario();
    executeScenario(scenario);
    logger.info("SimpleTrackerScenario test completed");
  }

  //  @Test
  void testHideAndSeekScenario() throws Exception {
    logger.info("Starting HideAndSeekScenario test");
    Scenario scenario = new HideAndSeekScenario();
    executeScenario(scenario);
    logger.info("HideAndSeekScenario test completed");
  }

  //  @Test
  void testManualScenarioExecution() throws Exception {
    logger.info("Starting manual scenario execution test");

    Scenario scenario = new SimplestTrackerScenario();

    // Manual execution with separate phases
    scenarioRunner.init(scenario, realTimeAppContext);
    logger.info("Scenario initialized, you can now inspect state");

    scenarioRunner.run(scenario);
    logger.info("Scenario executed, you can now verify results");

    scenarioRunner.shutdown(scenario);
    logger.info("Scenario cleaned up");

    logger.info("Manual scenario execution test completed");
  }
}
