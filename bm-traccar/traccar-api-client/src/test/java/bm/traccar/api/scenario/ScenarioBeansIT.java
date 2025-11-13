package bm.traccar.api.scenario;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScenarioBeansIT extends BaseScenarioTest {
  private static final Logger logger = LoggerFactory.getLogger(BaseScenarioTest.class);

  @Test
  void contextLoads() {
    logger.info("Context loads successfully");
    listAllBeansInTestContext();
  }
}
