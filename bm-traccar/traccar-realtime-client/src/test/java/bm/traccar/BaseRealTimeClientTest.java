package bm.traccar;

import bm.gps.tracker.GpsOsmandTrackerConfig;
import bm.traccar.api.ApiException;
import bm.traccar.api.scenario.ScenarioLoader;
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
 * <p>This class runs the actual RealTimeClient application with full context for testing. Only the
 * RealTimeClient.run method is skipped in tests, i.e. @ActiveProfiles("test").
 */
@SpringBootTest(classes = RealTimeClient.class)
@Import(GpsOsmandTrackerConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public abstract class BaseRealTimeClientTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseRealTimeClientTest.class);

  // inject some major components to be used in tests
  @Autowired protected RealTimeController controller;
  @Autowired protected RealTimeClient client;
  @Autowired protected ScenarioLoader scenario;

  // no @Test in base class

  // @BeforeEach setup doesn't conserve state between tests
  @BeforeAll
  public void setup() throws Exception {
    scenario.setupScenario();
    logger.info("--- initialize realtime controller ---");
    if (controller.loginAndInitialize(scenario.admin)) {
      logger.info("\n\t\t***** RealTimeController initialized *****");
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
