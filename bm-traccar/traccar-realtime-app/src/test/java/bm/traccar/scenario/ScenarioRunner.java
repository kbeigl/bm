package bm.traccar.scenario;

import bm.traccar.rtapp.RealTimeAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ScenarioRunner executes scenarios with proper lifecycle management.
 *
 * <p>The runner ensures that scenarios are executed in the correct order:
 *
 * <ol>
 *   <li>Initialize the scenario
 *   <li>Run the scenario
 *   <li>Shutdown the scenario (even if execution fails)
 * </ol>
 *
 * <p><b>Usage:</b>
 *
 * <pre>
 * {@literal @}Autowired ScenarioRunner runner;
 * {@literal @}Autowired RealTimeAppContext context;
 *
 * {@literal @}Test
 * void testMyScenario() {
 *   Scenario scenario = new MyScenario();
 *   runner.execute(scenario, context);
 * }
 * </pre>
 */
@Component
public class ScenarioRunner {
  private static final Logger logger = LoggerFactory.getLogger(ScenarioRunner.class);

  /**
   * Execute a scenario with proper lifecycle management.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Sets the context on the scenario
   *   <li>Calls {@link Scenario#initScenario()}
   *   <li>Calls {@link Scenario#runScenario()}
   *   <li>Calls {@link Scenario#shutdownScenario()} (always, even if run fails)
   * </ol>
   *
   * @param scenario the scenario to execute
   * @param context the context providing access to RealTimeApp components
   * @throws Exception if scenario execution fails (after cleanup)
   */
  public void execute(Scenario scenario, RealTimeAppContext context) throws Exception {
    scenario.setContext(context);

    logger.info("========================================");
    logger.info("Executing scenario: {}", scenario.getName());
    logger.info("========================================");

    Exception executionException = null;

    try {
      // Phase 1: Initialize
      logger.info(">>> Phase 1: Initialize scenario '{}'", scenario.getName());
      scenario.initScenario();
      logger.info("<<< Initialization complete");

      // Phase 2: Run
      logger.info(">>> Phase 2: Run scenario '{}'", scenario.getName());
      scenario.runScenario();
      logger.info("<<< Scenario execution complete");

    } catch (Exception e) {
      logger.error("Scenario '{}' failed during execution: {}", scenario.getName(), e.getMessage());
      executionException = e;
    } finally {
      // Phase 3: Shutdown (always)
      try {
        logger.info(">>> Phase 3: Shutdown scenario '{}'", scenario.getName());
        scenario.shutdownScenario();
        logger.info("<<< Shutdown complete");
      } catch (Exception e) {
        logger.error(
            "Scenario '{}' failed during shutdown: {}", scenario.getName(), e.getMessage(), e);
        // If we had an execution exception, keep it; otherwise, use shutdown exception
        if (executionException == null) {
          executionException = e;
        }
      }
    }

    logger.info("========================================");
    logger.info("Scenario '{}' completed", scenario.getName());
    logger.info("========================================");

    // Re-throw the first exception that occurred
    if (executionException != null) {
      throw executionException;
    }
  }

  /**
   * Execute only the init phase of a scenario. Useful for debugging or step-by-step execution.
   *
   * @param scenario the scenario to initialize
   * @param context the context providing access to RealTimeApp components
   * @throws Exception if initialization fails
   */
  public void init(Scenario scenario, RealTimeAppContext context) throws Exception {
    scenario.setContext(context);
    logger.info("Initializing scenario: {}", scenario.getName());
    scenario.initScenario();
    logger.info("Initialization complete");
  }

  /**
   * Execute only the run phase of a scenario. Assumes init was already called.
   *
   * @param scenario the scenario to run
   * @throws Exception if execution fails
   */
  public void run(Scenario scenario) throws Exception {
    logger.info("Running scenario: {}", scenario.getName());
    scenario.runScenario();
    logger.info("Execution complete");
  }

  /**
   * Execute only the shutdown phase of a scenario. Useful for cleaning up after manual testing.
   *
   * @param scenario the scenario to shutdown
   * @throws Exception if shutdown fails
   */
  public void shutdown(Scenario scenario) throws Exception {
    logger.info("Shutting down scenario: {}", scenario.getName());
    scenario.shutdownScenario();
    logger.info("Shutdown complete");
  }
}
