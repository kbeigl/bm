package bm.traccar.rt;

import bm.traccar.api.ApiConfig;
import bm.traccar.api.ApiException;
import bm.traccar.api.ApiService;
import bm.traccar.api.scenario.ScenarioConfig;
import bm.traccar.api.scenario.ScenarioLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Base class for integration tests using Spring Boot. This class provides a common setup and
 * teardown of a full scenario for realtime integration tests.
 *
 * <p>An abstract base class for Spring Boot Camel integration tests.
 *
 * <p>This class provides the necessary setup to initialize a Spring ApplicationContext that scans
 * for components, services, and Camel routes in specified packages, which is necessary when
 * no @SpringBootApplication class is present.
 *
 * <p>It disables JMX for faster test execution and provides pre-configured Camel test utilities
 * like CamelContext and ProducerTemplate.
 */
@SpringBootTest // (classes = BaseRealTimeTest.TestConfig.class)
@ContextConfiguration(
    classes = {ApiService.class, ApiConfig.class, ScenarioLoader.class, ScenarioConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseRealTimeTest {
  // private static final Logger logger = LoggerFactory.getLogger(BaseRealTimeTest.class);

  @Autowired ScenarioLoader scenario;

  @BeforeAll
  public void setup() throws ApiException {
    scenario.setupScenario();
  }

  @AfterAll
  public void teardown() throws ApiException {
    scenario.teardownScenario();
  }

  // @Autowired private ApplicationContext appContext;

}
