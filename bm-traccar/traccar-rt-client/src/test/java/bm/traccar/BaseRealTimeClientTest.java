package bm.traccar;

import bm.traccar.api.ApiException;
import bm.traccar.api.scenario.ScenarioLoader;
import bm.traccar.rt.RealTimeController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests provides setup and teardown of a full scenario for realtime
 * client integration tests.
 *
 * <p>This class runs the actual RealTimeClient application with full context for testing. Only the
 * RealTimeClient.run method is skipped in tests.
 */
@SpringBootTest(classes = RealTimeClient.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public abstract class BaseRealTimeClientTest {

  // some major components to be used in tests
  @Autowired protected RealTimeController controller;
  @Autowired protected RealTimeClient client;
  @Autowired protected ScenarioLoader scenario;

  // no @Test in base class

  // @BeforeAll would be better, ....
  @BeforeEach
  public void setup() throws ApiException {
    scenario.setupScenario();
  }

  // ... but @AfterAll teardown doeesn't conserverve state between tests
  // check @DirtiesContext javadoc for details
  @AfterEach
  public void teardown() throws ApiException {
    scenario.teardownScenario();
  }
}
